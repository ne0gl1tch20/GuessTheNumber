#include "jarrlyy_dsp_engine.h"
#include <aaudio/AAudio.h>
#include <algorithm>
#include <array>
#include <atomic>
#include <cmath>
#include <cstdint>
#include <mutex>

namespace jarrlyy_dsp {
namespace {
constexpr uint32_t kQueueSize=32, kDelayFrames=12000;
constexpr int kVoiceCount=12;
constexpr float kPi=3.14159265358979323846f;
struct Command { int32_t preset=0; float volume=0.18f; uint32_t effects=0; };
struct Voice { bool active=false; int32_t preset=0; uint64_t age=0; float phase=0.0f; float volume=0.0f; };
struct Pattern { std::array<float,4> notes{}; int count=1; int wave=0; uint32_t noteFrames=2200; };
Pattern patternFor(int p) {
 switch(p) {
  case kConfirm: return {{{660,880,0,0}},2,0,3800};
  case kBack: return {{{590,420,0,0}},2,0,3600};
  case kError: return {{{235,175,0,0}},2,2,4600};
  case kReward: return {{{523.25f,659.25f,783.99f,1046.5f}},4,0,4200};
  case kArcade: return {{{392,523.25f,659.25f,783.99f}},4,1,3000};
  case kLow: return {{{392.0f,329.63f,0,0}},2,1,5200};
  case kHigh: return {{{698.46f,880,0,0}},2,1,3400};
  default: return {{{990,0,0,0}},1,0,2200};
 }
}
class Engine {
public:
 bool play(int32_t p,float v,uint32_t fx) {
  std::lock_guard<std::mutex> apiLock(apiMutex_);
  if(p<kTap||p>kHigh||!ensureStream()) return false;
  if(!startOrRecoverStream()) return false;
  uint32_t w=write_.load(std::memory_order_relaxed), next=(w+1U)%kQueueSize;
  if(next==read_.load(std::memory_order_acquire)) return false;
  queue_[w]=Command{p,std::clamp(v,0.0f,0.45f),fx&15U};
  write_.store(next,std::memory_order_release);
  stop_.store(false,std::memory_order_release);
  return true;
 }
 void stopAll(){ stop_.store(true,std::memory_order_release); }
 bool supported() const { return true; }
private:
 bool startOrRecoverStream() {
  if(!stream_) return ensureStream();
  aaudio_stream_state_t state=AAudioStream_getState(stream_);
  for(int attempt=0;state==AAUDIO_STREAM_STATE_STOPPING&&attempt<5;++attempt) {
   aaudio_stream_state_t next=state;
   (void)AAudioStream_waitForStateChange(stream_,state,&next,50000000LL);
   state=AAudioStream_getState(stream_);
  }
  if(state==AAUDIO_STREAM_STATE_DISCONNECTED||state==AAUDIO_STREAM_STATE_STOPPING) {
   (void)AAudioStream_close(stream_);
   stream_=nullptr;
   return ensureStream();
  }
  if(state==AAUDIO_STREAM_STATE_STOPPED||state==AAUDIO_STREAM_STATE_PAUSED) {
   if(AAudioStream_requestStart(stream_)==AAUDIO_OK) return true;
   (void)AAudioStream_close(stream_);
   stream_=nullptr;
   return ensureStream();
  }
  return state==AAUDIO_STREAM_STATE_STARTED||state==AAUDIO_STREAM_STATE_STARTING;
 }
 bool ensureStream() {
  if(stream_!=nullptr) {\n   if(AAudioStream_getState(stream_)!=AAUDIO_STREAM_STATE_DISCONNECTED) return true;\n   (void)AAudioStream_close(stream_);\n   stream_=nullptr;\n  }
  AAudioStreamBuilder* b=nullptr;
  if(AAudio_createStreamBuilder(&b)!=AAUDIO_OK||!b) return false;
  AAudioStreamBuilder_setDirection(b,AAUDIO_DIRECTION_OUTPUT);
  AAudioStreamBuilder_setFormat(b,AAUDIO_FORMAT_PCM_FLOAT);
  AAudioStreamBuilder_setChannelCount(b,2);
  AAudioStreamBuilder_setSampleRate(b,48000);
  AAudioStreamBuilder_setPerformanceMode(b,AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
  AAudioStreamBuilder_setSharingMode(b,AAUDIO_SHARING_MODE_SHARED);
  AAudioStreamBuilder_setDataCallback(b,&Engine::callback,this);
  aaudio_result_t r=AAudioStreamBuilder_openStream(b,&stream_);
  AAudioStreamBuilder_delete(b);
  if(r!=AAUDIO_OK||!stream_){stream_=nullptr;return false;}
  const aaudio_result_t startResult=AAudioStream_requestStart(stream_);
  if(startResult!=AAUDIO_OK){
   (void)AAudioStream_close(stream_);
   stream_=nullptr;
   return false;
  }
  return true;
 }
 static aaudio_data_callback_result_t callback(AAudioStream*,void* u,void* data,int32_t frames) {
  auto* e=static_cast<Engine*>(u); e->render(static_cast<float*>(data),frames);
  return e->idleFrames_>72000U?AAUDIO_CALLBACK_RESULT_STOP:AAUDIO_CALLBACK_RESULT_CONTINUE;
 }
 bool dequeue(Command& c) {
  uint32_t r=read_.load(std::memory_order_relaxed);
  if(r==write_.load(std::memory_order_acquire))return false;
  c=queue_[r];read_.store((r+1U)%kQueueSize,std::memory_order_release);return true;
 }
 float wave(float phase,int kind) {
  if(kind==1)return phase<0.5f?1.0f:-1.0f;
  if(kind==2)return 1.0f-4.0f*std::abs(phase-0.5f);
  if(kind==3)return 2.0f*phase-1.0f;
  return std::sin(phase*2.0f*kPi);
 }
 void render(float* out,int32_t frames) {
  if(!out||frames<=0)return;
  if(stop_.exchange(false,std::memory_order_acq_rel))for(auto& v:voices_)v.active=false;
  Command c;
  while(dequeue(c)){
   auto it=std::find_if(voices_.begin(),voices_.end(),[](const Voice& v){return !v.active;});
   if(it==voices_.end())it=voices_.begin();
   *it=Voice{true,c.preset,0U,0.0f,c.volume}; effects_=c.effects;
  }
  int rate=stream_?AAudioStream_getSampleRate(stream_):48000;
  bool active=false;
  for(int f=0;f<frames;++f){
   float mixed=0.0f;
   for(auto& v:voices_){
    if(!v.active)continue;
    Pattern p=patternFor(v.preset);uint64_t total=uint64_t(p.count)*p.noteFrames;
    if(v.age>=total){v.active=false;continue;}
    uint32_t ni=uint32_t(v.age/p.noteFrames), nf=uint32_t(v.age%p.noteFrames);
    float hz=p.notes[std::min<uint32_t>(ni,3U)];
    float env=std::min(1.0f,float(nf)/(rate*0.004f))*std::min(1.0f,float(p.noteFrames-nf)/(rate*0.022f));
    mixed+=wave(v.phase,p.wave)*env*v.volume;
    v.phase+=hz/float(rate);if(v.phase>=1.0f)v.phase-=1.0f;++v.age;active=true;
   }
   float l=mixed,r=mixed;
   if(effects_&kLowPass){filterL_+=0.16f*(l-filterL_);filterR_+=0.16f*(r-filterR_);l=filterL_;r=filterR_;}
   if(effects_&(kReverb|kChorus)){
    uint32_t delay=(effects_&kChorus)?uint32_t(std::sin(chorusPhase_)*220.0f+380.0f):5200U;
    uint32_t rd=(delayIndex_+kDelayFrames-delay)%kDelayFrames;
    float wet=(effects_&kChorus)?0.28f:0.32f;l+=delayL_[rd]*wet;r+=delayR_[rd]*wet;
    chorusPhase_+=0.0008f;if(chorusPhase_>=2*kPi)chorusPhase_-=2*kPi;
   }
   if(effects_&kDistortion){l=std::tanh(l*3.5f);r=std::tanh(r*3.5f);}
   delayL_[delayIndex_]=l;delayR_[delayIndex_]=r;delayIndex_=(delayIndex_+1U)%kDelayFrames;
   out[f*2]=std::clamp(l,-0.95f,0.95f);out[f*2+1]=std::clamp(r,-0.95f,0.95f);
  }
  if(active)idleFrames_=0;else idleFrames_+=uint64_t(frames);
  if(idleFrames_>72000U)std::fill(out,out+frames*2,0.0f);
 }
 std::mutex apiMutex_;
 AAudioStream* stream_=nullptr;
 std::array<Command,kQueueSize> queue_{};
 std::array<Voice,kVoiceCount> voices_{};
 std::atomic<uint32_t> write_{0},read_{0};
 std::atomic<bool> stop_{false};
 uint32_t effects_=0,delayIndex_=0;
 uint64_t idleFrames_=0;
 float filterL_=0,filterR_=0,chorusPhase_=0;
 std::array<float,kDelayFrames> delayL_{},delayR_{};
};
Engine& get(){static Engine e;return e;}
}
bool playPreset(int32_t p,float v,uint32_t fx){return get().play(p,v,fx);}
void stopAll(){get().stopAll();}
bool isSupported(){return get().supported();}
}
extern "C" int jarrlyy_dsp_play_preset(int32_t p,float v,uint32_t fx){return jarrlyy_dsp::playPreset(p,v,fx)?1:0;}
extern "C" void jarrlyy_dsp_stop_all(){jarrlyy_dsp::stopAll();}
extern "C" int jarrlyy_dsp_is_supported(){return jarrlyy_dsp::isSupported()?1:0;}

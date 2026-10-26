#pragma once
#include <cstdint>
namespace jarrlyy_dsp {
enum Effect : uint32_t { kLowPass=1U, kReverb=2U, kChorus=4U, kDistortion=8U };
enum Preset : int32_t { kTap=0, kConfirm=1, kBack=2, kError=3, kReward=4, kArcade=5, kLow=6, kHigh=7 };
bool playPreset(int32_t preset, float volume, uint32_t effects);
void stopAll();
bool isSupported();
}
#if defined(__GNUC__)
#define JARRLYY_DSP_API __attribute__((visibility("default")))
#else
#define JARRLYY_DSP_API
#endif

extern "C" {
JARRLYY_DSP_API int jarrlyy_dsp_play_preset(int32_t preset, float volume, uint32_t effects);
JARRLYY_DSP_API void jarrlyy_dsp_stop_all();
JARRLYY_DSP_API int jarrlyy_dsp_is_supported();
}

#include <jni.h>
#include "jarrlyy_dsp.h"
extern "C" JNIEXPORT jboolean JNICALL
Java_com_jarrlyyy_guessthenumber_audio_JarrlyyDspEngine_nativePlayPreset(JNIEnv*,jobject,jint p,jfloat v,jint fx){
 return jarrlyy_dsp_play_preset(static_cast<int32_t>(p),v,static_cast<uint32_t>(fx))?JNI_TRUE:JNI_FALSE;
}
extern "C" JNIEXPORT void JNICALL
Java_com_jarrlyyy_guessthenumber_audio_JarrlyyDspEngine_nativeStopAll(JNIEnv*,jobject){jarrlyy_dsp_stop_all();}
extern "C" JNIEXPORT jboolean JNICALL
Java_com_jarrlyyy_guessthenumber_audio_JarrlyyDspEngine_nativeIsSupported(JNIEnv*,jobject){
 return jarrlyy_dsp_is_supported()?JNI_TRUE:JNI_FALSE;
}

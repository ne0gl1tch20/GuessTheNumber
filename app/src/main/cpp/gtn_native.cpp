#include <jni.h>

#include <string>

#include "gtn_native_core.h"

extern "C" JNIEXPORT jlong JNICALL
Java_com_jarrlyyy_guessthenumber_shared_engine_NativeGameplayOptimizer_nativeComputeAdaptiveTarget(
    JNIEnv* env,
    jobject,
    jlong rangeMin,
    jlong rangeMax,
    jstring difficulty,
    jint streak,
    jlong attempts,
    jlong seed) {
    const char* chars = difficulty == nullptr ? nullptr : env->GetStringUTFChars(difficulty, nullptr);
    const std::string difficultyId = chars == nullptr ? "classic" : std::string(chars);
    if (chars != nullptr) env->ReleaseStringUTFChars(difficulty, chars);
    return static_cast<jlong>(gtn_native::adaptiveTarget(
        static_cast<int64_t>(rangeMin),
        static_cast<int64_t>(rangeMax),
        difficultyId,
        static_cast<int32_t>(streak),
        static_cast<int64_t>(attempts),
        static_cast<int64_t>(seed)));
}

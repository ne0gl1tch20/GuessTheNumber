#include <jni.h>

#include <algorithm>
#include <cstdint>
#include <limits>
#include <string>

namespace {

uint64_t mix(uint64_t value) {
    value += 0x9E3779B97F4A7C15ULL;
    value = (value ^ (value >> 30U)) * 0xBF58476D1CE4E5B9ULL;
    value = (value ^ (value >> 27U)) * 0x94D049BB133111EBULL;
    return value ^ (value >> 31U);
}

uint64_t difficultySalt(const std::string& difficulty) {
    if (difficulty == "extreme") return 0xE17E4E9B5A7C31D1ULL;
    if (difficulty == "hard") return 0xA11CE5D4B8F23071ULL;
    return 0xC1A551C0DE2026A1ULL;
}

jlong adaptiveTarget(
    jlong rangeMin,
    jlong rangeMax,
    const std::string& difficulty,
    jint streak,
    jlong attempts,
    jlong seed) {
    if (rangeMax < rangeMin) return rangeMin;
    const uint64_t width = static_cast<uint64_t>(rangeMax - rangeMin) + 1ULL;
    if (width == 0ULL) return rangeMin;

    // Personalization inputs are deliberately limited to non-sensitive gameplay state.
    // The native path never reads or mutates saves directly.
    uint64_t state = static_cast<uint64_t>(seed);
    state ^= difficultySalt(difficulty);
    state ^= static_cast<uint64_t>(std::max<jint>(0, streak)) * 0xD6E8FEB86659FD93ULL;
    state ^= static_cast<uint64_t>(std::max<jlong>(0, attempts)) * 0xA5A3564E27F3C91BULL;

    const uint64_t sample = mix(state);
    uint64_t offset = sample % width;

    // Higher difficulty and longer streaks move the distribution away from always
    // selecting the midpoint, while still remaining fully inside the saved range.
    const uint64_t pressure = static_cast<uint64_t>(std::min<jint>(streak, 32)) +
                              static_cast<uint64_t>(difficulty == "extreme" ? 3 : difficulty == "hard" ? 2 : 1);
    if (width > 4ULL) {
        const uint64_t edgeBand = std::min(width / 4ULL, pressure * width / 128ULL);
        if (edgeBand > 0ULL && (sample & 1ULL) != 0ULL) {
            offset = std::min<uint64_t>(width - 1ULL, offset + edgeBand);
        }
    }

    const uint64_t result = static_cast<uint64_t>(rangeMin) + offset;
    if (result > static_cast<uint64_t>(std::numeric_limits<jlong>::max())) {
        return rangeMax;
    }
    return static_cast<jlong>(result);
}

}  // namespace

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
    return adaptiveTarget(rangeMin, rangeMax, difficultyId, streak, attempts, seed);
}

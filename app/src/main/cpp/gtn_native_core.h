#pragma once

#include <algorithm>
#include <cstdint>
#include <limits>
#include <string>

namespace gtn_native {

inline uint64_t mix(uint64_t value) {
    value += 0x9E3779B97F4A7C15ULL;
    value = (value ^ (value >> 30U)) * 0xBF58476D1CE4E5B9ULL;
    value = (value ^ (value >> 27U)) * 0x94D049BB133111EBULL;
    return value ^ (value >> 31U);
}

inline uint64_t difficultySalt(const std::string& difficulty) {
    if (difficulty == "extreme") return 0xE17E4E9B5A7C31D1ULL;
    if (difficulty == "hard") return 0xA11CE5D4B8F23071ULL;
    return 0xC1A551C0DE2026A1ULL;
}

inline int64_t adaptiveTarget(
    int64_t rangeMin,
    int64_t rangeMax,
    const std::string& difficulty,
    int32_t streak,
    int64_t attempts,
    int64_t seed) {
    if (rangeMax < rangeMin) return rangeMin;
    const uint64_t width = static_cast<uint64_t>(rangeMax - rangeMin) + 1ULL;
    if (width == 0ULL) return rangeMin;

    uint64_t state = static_cast<uint64_t>(seed);
    state ^= difficultySalt(difficulty);
    state ^= static_cast<uint64_t>(std::max<int32_t>(0, streak)) * 0xD6E8FEB86659FD93ULL;
    state ^= static_cast<uint64_t>(std::max<int64_t>(0, attempts)) * 0xA5A3564E27F3C91BULL;

    const uint64_t sample = mix(state);
    uint64_t offset = sample % width;
    const uint64_t pressure = static_cast<uint64_t>(std::min<int32_t>(streak, 32)) +
                              static_cast<uint64_t>(difficulty == "extreme" ? 3 : difficulty == "hard" ? 2 : 1);
    if (width > 4ULL) {
        const uint64_t edgeBand = std::min(width / 4ULL, pressure * width / 128ULL);
        if (edgeBand > 0ULL && (sample & 1ULL) != 0ULL) {
            offset = std::min<uint64_t>(width - 1ULL, offset + edgeBand);
        }
    }

    const uint64_t result = static_cast<uint64_t>(rangeMin) + offset;
    if (result > static_cast<uint64_t>(std::numeric_limits<int64_t>::max())) {
        return rangeMax;
    }
    return static_cast<int64_t>(result);
}

}  // namespace gtn_native

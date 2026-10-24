#include "gtn_native_core.h"

#include <algorithm>
#include <chrono>
#include <cstdint>
#include <fstream>
#include <iostream>
#include <string>
#include <vector>

namespace {
struct BenchmarkCase {
    int64_t rangeMin;
    int64_t rangeMax;
    std::string difficulty;
    int32_t streak;
    int64_t attempts;
    int64_t seed;
};

std::vector<BenchmarkCase> makeCases() {
    std::vector<BenchmarkCase> cases;
    cases.reserve(128);
    for (int index = 0; index < 128; ++index) {
        cases.push_back({
            1,
            1000 + (index % 4) * 9000,
            index % 3 == 0 ? "classic" : index % 3 == 1 ? "hard" : "extreme",
            index % 41,
            (index * 17LL) % 500LL,
            0x1234ABCDLL + index * 0x9E37LL,
        });
    }
    return cases;
}

volatile uint64_t sink = 0;

uint64_t evaluate(const std::vector<BenchmarkCase>& cases) {
    uint64_t checksum = 0;
    for (const auto& item : cases) {
        checksum ^= static_cast<uint64_t>(gtn_native::adaptiveTarget(
            item.rangeMin,
            item.rangeMax,
            item.difficulty,
            item.streak,
            item.attempts,
            item.seed));
    }
    return checksum;
}
}

int main() {
    constexpr int warmupIterations = 2000;
    constexpr int sampleCount = 21;
    constexpr int iterationsPerSample = 100000;
    const auto cases = makeCases();

    for (int i = 0; i < warmupIterations; ++i) {
        sink = evaluate(cases);
    }

    const uint64_t expectedChecksum = evaluate(cases);
    std::vector<int64_t> samples;
    samples.reserve(sampleCount);

    for (int sample = 0; sample < sampleCount; ++sample) {
        uint64_t checksum = 0;
        const auto started = std::chrono::steady_clock::now();
        for (int iteration = 0; iteration < iterationsPerSample; ++iteration) {
            checksum ^= evaluate(cases);
        }
        const auto elapsed = std::chrono::duration_cast<std::chrono::nanoseconds>(
            std::chrono::steady_clock::now() - started).count();
        sink = checksum ^ static_cast<uint64_t>(sample);
        samples.push_back(elapsed / (iterationsPerSample * static_cast<int>(cases.size())));
    }

    std::sort(samples.begin(), samples.end());
    const int64_t median = samples[samples.size() / 2];
    const int64_t p95 = samples[((samples.size() - 1) * 95) / 100];
    const uint64_t throughput = median == 0 ? UINT64_MAX : 1000000000ULL / static_cast<uint64_t>(median);

    std::ofstream report("build/reports/native-vs-kotlin-native.txt");
    report << "NATIVE_CORE\n";
    report << "compiler=" << __VERSION__ << "\n";
    report << "cases=" << cases.size() << "\n";
    report << "warmupIterations=" << warmupIterations << "\n";
    report << "samples=" << sampleCount << "\n";
    report << "iterationsPerSample=" << iterationsPerSample << "\n";
    report << "medianNsPerEvaluation=" << median << "\n";
    report << "p95NsPerEvaluation=" << p95 << "\n";
    report << "medianEvaluationsPerSecond=" << throughput << "\n";
    report << "checksum=" << expectedChecksum << "\n";
    report << "bounded=true\n";
    report.close();

    std::cout << "NATIVE_VS_KOTLIN_NATIVE_BENCHMARK "
              << "medianNsPerEvaluation=" << median << ' '
              << "p95NsPerEvaluation=" << p95 << ' '
              << "medianEvaluationsPerSecond=" << throughput << ' '
              << "checksum=" << expectedChecksum << '\n';
    return expectedChecksum == 0 ? 1 : 0;
}

#pragma once
#include <cstdint>
#include "jarrlyy_dsp.h"
namespace jarrlyy_dsp {
enum Effect : uint32_t { kLowPass=1U, kReverb=2U, kChorus=4U, kDistortion=8U };
enum Preset : int32_t { kTap=0, kConfirm=1, kBack=2, kError=3, kReward=4, kArcade=5, kLow=6, kHigh=7 };
bool playPreset(int32_t preset, float volume, uint32_t effects);
void stopAll();
bool isSupported();
}

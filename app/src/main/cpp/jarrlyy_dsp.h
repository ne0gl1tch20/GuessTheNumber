#pragma once
#include <stdint.h>

#if defined(__GNUC__)
#define JARRLYY_DSP_API __attribute__((visibility("default")))
#else
#define JARRLYY_DSP_API
#endif

#ifdef __cplusplus
extern "C" {
#endif

/* Preset IDs: 0 tap, 1 confirm, 2 back, 3 error, 4 reward, 5 arcade,
 * 6 guess-too-low, 7 guess-too-high. Effects are bit flags 1, 2, 4, and 8.
 * Volume is clamped by the engine to the safe range 0.0–0.45.
 */
JARRLYY_DSP_API int jarrlyy_dsp_play_preset(int32_t preset, float volume, uint32_t effects);
JARRLYY_DSP_API void jarrlyy_dsp_stop_all(void);
JARRLYY_DSP_API int jarrlyy_dsp_is_supported(void);

#ifdef __cplusplus
}
#endif

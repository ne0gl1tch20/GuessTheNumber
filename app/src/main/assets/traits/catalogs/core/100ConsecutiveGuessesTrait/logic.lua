local best = tonumber(gtn.input.bestStreak or "0")
return {
  eligible = best >= 100,
  effects = { "NEBULA_BONUS" }
}

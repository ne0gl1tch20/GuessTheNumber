-- Random-event selection lives in Lua. Kotlin validates the returned event ID
-- and remains responsible for applying all rewards and permanent game-state changes.
local trigger = math.random() < 0.35
local event_types = {
    "CASH_BURST",
    "NEBULA_RAIN",
    "JACKPOT",
    "STREAK_SURGE",
    "RANGE_SHUFFLE",
    "TAX_REFUND",
    "COSMIC_GIFT"
}
local selected = event_types[math.random(1, #event_types)]

return {
    trigger = trigger,
    type = selected
}

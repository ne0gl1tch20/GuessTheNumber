-- Invisible Trait System v2
-- Lua decides which approved helper intents are useful.
-- Kotlin remains authoritative and validates every returned intent.

local MAX_HELPERS = 3

local function number(name)
    return tonumber(gtn.input[name] or "0") or 0
end

local function boolean(name)
    return gtn.input[name] == "1"
end

local function add_helper(out, id)
    if #out < MAX_HELPERS then
        out[#out + 1] = id
    end
end

local function traits_evaluate()
    local helpers = {}
    local bonuses = {}
    local failed = number("failed_guesses")
    local correct = number("correct_guesses")
    local long_streaks = number("long_streaks_observed")
    local current_streak = number("current_streak")
    local prestige = number("prestige_count")
    local ultra = number("ultra_count")

    if failed >= 3 then add_helper(helpers, "mistake_insurance") end
    if failed >= 5 and correct >= 10 then add_helper(helpers, "range_scout") end
    if long_streaks > 0 then add_helper(helpers, "streak_guard") end
    if failed >= 6 and correct > 0 then add_helper(helpers, "comeback_reward") end
    if current_streak >= 5 then add_helper(helpers, "momentum_reward") end

    if prestige >= 1 and not boolean("first_prestige_awarded") then
        bonuses[#bonuses + 1] = "first_prestige_bonus"
    end
    if ultra >= 1 and not boolean("first_ultra_awarded") then
        bonuses[#bonuses + 1] = "first_ultra_bonus"
    end
    if prestige >= 10 and not boolean("veteran_awarded") then
        bonuses[#bonuses + 1] = "veteran_bonus"
    end

    return {
        helpers = helpers,
        bonuses = bonuses
    }
end

return traits_evaluate()

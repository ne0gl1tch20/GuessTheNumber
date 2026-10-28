-- Invisible Trait System v2
-- Lua decides which approved helper intents are useful.
-- Kotlin remains authoritative and validates every returned intent.

local MAX_HELPERS = 3

local function add_helper(out, id)
    if #out < MAX_HELPERS then
        out[#out + 1] = id
    end
end

function traits.evaluate(snapshot)
    local helpers = {}
    local bonuses = {}

    if snapshot.failed_guesses >= 3 then
        add_helper(helpers, "mistake_insurance")
    end
    if snapshot.failed_guesses >= 5 and snapshot.correct_guesses >= 10 then
        add_helper(helpers, "range_scout")
    end
    if snapshot.best_streak >= 10 then
        add_helper(helpers, "streak_guard")
    end
    if snapshot.failed_guesses >= 6 and snapshot.correct_guesses > 0 then
        add_helper(helpers, "comeback_reward")
    end
    if snapshot.current_streak >= 5 then
        add_helper(helpers, "momentum_reward")
    end

    if snapshot.prestige_count >= 1 and not snapshot.first_prestige_awarded then
        bonuses[#bonuses + 1] = "first_prestige_bonus"
    end
    if snapshot.ultra_count >= 1 and not snapshot.first_ultra_awarded then
        bonuses[#bonuses + 1] = "first_ultra_bonus"
    end
    if snapshot.prestige_count >= 10 and not snapshot.veteran_awarded then
        bonuses[#bonuses + 1] = "veteran_bonus"
    end

    return {
        helpers = helpers,
        bonuses = bonuses
    }
end

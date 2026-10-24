-- Example trait logic module.
-- The host should provide read-only state and validated request functions only.
-- This module does not write saves or award rewards.
return {
    id = "100_consecutive_guesses",
    requiredCorrectGuesses = 100,
    activationRequestedByHost = true
}

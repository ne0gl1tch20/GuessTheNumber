-- Bundled example activity. The Kotlin host validates the returned UI model.
gtn.log("Number Rush definition loaded")

return {
    title = "Number Rush",
    description = "One quick question. Pick the right answer to complete this round.",
    prompt = "Which number is a prime number?",
    options = { "21", "29", "39", "51" },
    correct_option = 2,
    success_message = "Correct! You spotted the prime number.",
    failure_message = "Not quite. 29 is the prime number."
}

gtn.log("Signed LiveOps demo loaded")

return {
    title = "LiveOps Demo",
    description = "This activity came from a verified signed bundle.",
    prompt = "Which number is even?",
    options = { "13", "27", "42", "55" },
    correct_option = 3,
    success_message = "Correct! The signed activity ran successfully.",
    failure_message = "42 is the even number."
}

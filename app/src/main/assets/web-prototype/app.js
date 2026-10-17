(() => {
  const form = document.querySelector("#guess-form");
  const input = document.querySelector("#guess-input");
  const button = document.querySelector("#guess-submit");
  const feedback = document.querySelector("#feedback");
  const rangeMin = document.querySelector("#range-min");
  const rangeMax = document.querySelector("#range-max");
  const rangeBadge = document.querySelector("#range-badge");
  const attempts = document.querySelector("#attempts");
  const correctGuesses = document.querySelector("#correct-guesses");
  const streak = document.querySelector("#streak");
  let nextId = 1;
  let pending = false;
  let pendingRequestId = null;
  let ready = false;

  function send(type, guess) {
    if (!window.gtnHost || typeof window.gtnHost.postMessage !== "function") {
      showFeedback("Secure local bridge unavailable.", "error");
      return;
    }
    const id = String(nextId++);
    pendingRequestId = id;
    const request = { version: 1, id, type };
    if (typeof guess === "string") request.guess = guess;
    window.gtnHost.postMessage(JSON.stringify(request));
  }

  function showFeedback(message, kind) {
    feedback.textContent = message;
    feedback.className = "gtn-feedback" + (kind ? " gtn-feedback--" + kind : "");
  }

  function applyMessage(message) {
    let response;
    try { response = JSON.parse(message.data); }
    catch { showFeedback("The host returned an unreadable response. Waiting for a valid reply.", "error"); return; }
    // Ignore delayed or unrelated replies instead of letting them roll the UI
    // back to an older state or unlock a newer in-flight request.
    if (typeof response.id !== "string" || response.id !== pendingRequestId) return;
    pendingRequestId = null;
    if (response.version !== 1) { showFeedback("Unsupported bridge version.", "error"); pending = false; button.disabled = !ready; return; }
    if (response.type === "error") {
      showFeedback(response.error === "guess_processing_failed" ? "The guess could not be processed. Try again." : "Request rejected: " + String(response.error || "unknown_error"), "error");
      pending = false;
      button.disabled = !ready;
      return;
    }
    const state = response.state;
    if (response.type !== "state" || !state || !Number.isFinite(state.rangeMin) || !Number.isFinite(state.rangeMax)) {
      showFeedback("The host returned invalid game state.", "error");
      pending = false;
      button.disabled = !ready;
      return;
    }
    rangeMin.textContent = String(state.rangeMin);
    rangeMax.textContent = String(state.rangeMax);
    attempts.textContent = String(state.attempts);
    correctGuesses.textContent = String(state.correctGuesses);
    streak.textContent = String(state.streak);
    rangeBadge.textContent = "Kotlin connected";
    ready = true;
    pending = false;
    button.disabled = false;
    input.min = String(state.rangeMin);
    input.max = String(state.rangeMax);
    const status = state.lastGuessStatus;
    if (status === "rate_limited" || state.accepted === false) showFeedback("Slow down a moment, then try again.", "warning");
    else if (status === "CORRECT") showFeedback("Correct! 🎉 New round ready.", "success");
    else if (status === "TOO_LOW") showFeedback("Too low. Try a higher number.", "warning");
    else if (status === "TOO_HIGH") showFeedback("Too high. Try a lower number.", "warning");
    else if (status) showFeedback("Guess recorded.", "");
    else showFeedback("Ready when you are.", "");
  }

  if (window.gtnHost && typeof window.gtnHost.postMessage === "function") {
    window.gtnHost.onmessage = applyMessage;
    form.addEventListener("submit", event => {
      event.preventDefault();
      if (!ready || pending) return;
      const value = input.value.trim();
      if (value.length < 1 || value.length > 20 || !/^-?[0-9]+$/.test(value)) { showFeedback("Enter a whole number using at most 20 characters.", "error"); return; }
      pending = true;
      button.disabled = true;
      showFeedback("Checking guess…", "");
      send("submit_guess", value);
    });
    send("read_state");
  } else {
    rangeBadge.textContent = "Offline preview";
    showFeedback("The secure host bridge is unavailable. Open this screen inside the app.", "error");
    button.disabled = true;
  }
})();
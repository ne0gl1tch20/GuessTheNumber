(() => {
  'use strict';

  const host = window.gtnHost;
  const els = {
    range: document.getElementById('rangeText'),
    feedback: document.getElementById('feedback'),
    attempts: document.getElementById('attempts'),
    correct: document.getElementById('correct'),
    streak: document.getElementById('streak'),
    bestStreak: document.getElementById('bestStreak'),
    money: document.getElementById('money'),
    income: document.getElementById('income'),
    progress: document.getElementById('progressFill'),
    input: document.getElementById('guessInput'),
    button: document.getElementById('guessButton'),
    status: document.getElementById('statusLabel'),
    microcopy: document.getElementById('microcopy'),
    toast: document.getElementById('toast')
  };

  let nextId = 0;
  let pendingId = null;
  let toastTimer = 0;

  function id() {
    nextId = (nextId + 1) % 2147483647;
    return `play-${Date.now()}-${nextId}`;
  }

  function send(type, guess) {
    if (!host || pendingId !== null) return false;
    const requestId = id();
    pendingId = requestId;
    els.button.disabled = true;
    host.postMessage(JSON.stringify({ version: 1, id: requestId, type, ...(guess === undefined ? {} : { guess }) }));
    return true;
  }

  function showToast(message) {
    els.toast.textContent = message;
    els.toast.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => els.toast.classList.remove('show'), 1700);
  }

  function animateNumber(element, value) {
    const old = element.textContent;
    element.textContent = value;
    if (old !== String(value)) {
      element.closest('.stat-card')?.classList.remove('bump');
      void element.offsetWidth;
      element.closest('.stat-card')?.classList.add('bump');
    }
  }

  function render(state) {
    if (!state) return;
    els.range.textContent = `${state.rangeMin} — ${state.rangeMax}`;
    animateNumber(els.attempts, state.attempts ?? 0);
    animateNumber(els.correct, state.correctGuesses ?? 0);
    animateNumber(els.streak, state.streak ?? 0);
    els.bestStreak.textContent = `BEST ${state.bestStreak ?? 0}`;
    els.money.textContent = state.money ?? '0';
    els.income.textContent = state.incomePerSecond ?? '0';

    const maxStreak = Math.max(1, Number(state.bestStreak ?? state.streak ?? 1));
    const currentStreak = Math.max(0, Number(state.streak ?? 0));
    els.progress.style.width = `${Math.min(100, currentStreak / maxStreak * 100)}%`;

    if (state.lastGuessStatus) {
      const status = String(state.lastGuessStatus);
      const messages = {
        correct: '✨ Perfect hit. The run is accelerating.',
        too_low: '📈 Too low. Push the number higher.',
        too_high: '📉 Too high. Bring it down.',
        rate_limited: '⏳ The engine needs a tiny breather.'
      };
      els.feedback.textContent = messages[status] || 'Run updated.';
      els.feedback.classList.remove('pop');
      void els.feedback.offsetWidth;
      els.feedback.classList.add('pop');
    }
  }

  function handleReply(raw) {
    let message;
    try { message = JSON.parse(raw); } catch { return; }
    if (!message || message.id !== pendingId) return;

    pendingId = null;
    els.button.disabled = false;

    if (message.type === 'error') {
      els.status.textContent = 'READY';
      els.microcopy.textContent = message.error === 'duplicate_request' ? 'That move was already received.' : 'The game engine rejected that request.';
      showToast('Try that move again.');
      return;
    }
    if (message.type !== 'state') return;

    els.status.textContent = 'LIVE';
    render(message.state);
    if (message.state?.accepted === false) showToast('Guess rate-limited');
  }

  window.addEventListener('message', event => handleReply(event.data));
  if (host && typeof host.onMessage === 'function') host.onMessage(handleReply);

  document.getElementById('guessForm').addEventListener('submit', event => {
    event.preventDefault();
    const value = els.input.value.trim();
    if (!/^-?[0-9]+$/.test(value) || value.length > 20) {
      showToast('Enter a whole number');
      return;
    }
    if (send('submit_guess', value)) els.input.value = '';
  });

  els.input.addEventListener('keydown', event => {
    if (event.key === 'Enter') event.preventDefault();
  });

  if (host) {
    send('read_state');
  } else {
    els.status.textContent = 'OFFLINE';
    els.microcopy.textContent = 'Game engine unavailable.';
  }
})();

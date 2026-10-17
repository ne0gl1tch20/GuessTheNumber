(() => {
  'use strict';
  const host = window.gtnHost;
  const els = {
    range: document.getElementById('rangeText'), feedback: document.getElementById('feedback'), attempts: document.getElementById('attempts'), correct: document.getElementById('correct'), streak: document.getElementById('streak'), bestStreak: document.getElementById('bestStreak'), money: document.getElementById('money'), income: document.getElementById('income'), progress: document.getElementById('progressFill'), input: document.getElementById('guessInput'), button: document.getElementById('guessButton'), status: document.getElementById('statusLabel'), microcopy: document.getElementById('microcopy'), toast: document.getElementById('toast')
  };
  let nextId = 0, pendingId = null, toastTimer = 0;
  const requestId = () => { nextId = (nextId + 1) % 2147483647; return `p${Date.now()}_${nextId}`; };
  function send(type, guess) { if (!host || typeof host.postMessage !== 'function' || pendingId !== null) return false; const id = requestId(); pendingId = id; els.button.disabled = true; const request = { version: 1, id, type }; if (guess !== undefined) request.guess = guess; host.postMessage(JSON.stringify(request)); return true; }
  function toast(message) { els.toast.textContent = message; els.toast.classList.add('show'); clearTimeout(toastTimer); toastTimer = setTimeout(() => els.toast.classList.remove('show'), 1700); }
  function bump(el) { const card = el.closest('.stat-card'); if (!card) return; card.classList.remove('bump'); void card.offsetWidth; card.classList.add('bump'); }
  function setNumber(el, value) { const next = String(value ?? 0); if (el.textContent !== next) bump(el); el.textContent = next; }
  function render(state) {
    if (!state) return;
    els.range.textContent = `${state.rangeMin} — ${state.rangeMax}`;
    setNumber(els.attempts, state.attempts); setNumber(els.correct, state.correctGuesses); setNumber(els.streak, state.streak);
    els.bestStreak.textContent = `BEST ${state.bestStreak ?? 0}`; els.money.textContent = state.money ?? '0'; els.income.textContent = state.incomePerSecond ?? '0';
    const best = Math.max(1, Number(state.bestStreak ?? state.streak ?? 1));
    els.progress.style.width = `${Math.min(100, Math.max(0, Number(state.streak ?? 0)) / best * 100)}%`;
    const status = String(state.lastGuessStatus || '');
    const messages = { CORRECT: '✨ Perfect hit. The run is accelerating.', TOO_LOW: '📈 Too low. Push the number higher.', TOO_HIGH: '📉 Too high. Bring it down.', rate_limited: '⏳ The engine needs a tiny breather.' };
    if (status) { els.feedback.textContent = messages[status] || 'Run updated.'; els.feedback.classList.remove('pop'); void els.feedback.offsetWidth; els.feedback.classList.add('pop'); }
  }
  function handleReply(raw) { let message; try { message = JSON.parse(raw); } catch { return; } if (!message || message.id !== pendingId) return; pendingId = null; els.button.disabled = false; if (message.type === 'error') { els.status.textContent = 'READY'; els.microcopy.textContent = 'The game engine rejected that request.'; toast('Try that move again.'); return; } if (message.type !== 'state' || !message.state) return; els.status.textContent = 'LIVE'; render(message.state); if (message.state.accepted === false) toast('Guess rate-limited'); }
  if (host && typeof host.postMessage === 'function') {
    host.onmessage = event => handleReply(event.data);
    document.getElementById('guessForm').addEventListener('submit', event => { event.preventDefault(); const value = els.input.value.trim(); if (!/^-?[0-9]+$/.test(value) || value.length > 20) { toast('Enter a whole number'); return; } if (send('submit_guess', value)) els.input.value = ''; });
    send('read_state');
  } else { els.status.textContent = 'OFFLINE'; els.microcopy.textContent = 'Game engine unavailable.'; }
})();

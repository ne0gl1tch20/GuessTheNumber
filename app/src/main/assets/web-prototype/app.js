(() => {
  const counter = document.querySelector("#counter");
  const button = document.querySelector("#increment");
  const status = document.querySelector("#status");
  let nextId = 1;

  function send(type) {
    if (!window.gtnHost || typeof window.gtnHost.postMessage !== "function") {
      status.textContent = "The secure local host bridge is unavailable on this device.";
      button.disabled = true;
      return;
    }
    const id = String(nextId++);
    window.gtnHost.postMessage(JSON.stringify({ version: 1, id, type }));
  }

  function applyMessage(message) {
    let response;
    try {
      response = JSON.parse(message.data);
    } catch {
      status.textContent = "The host returned an unreadable response.";
      return;
    }
    if (response.version !== 1) {
      status.textContent = "Unsupported host protocol version.";
      return;
    }
    if (response.type === "error") {
      status.textContent = "Host rejected the request: " + String(response.error || "unknown_error");
      return;
    }
    if (response.type !== "state" || !Number.isSafeInteger(response.state?.counter)) {
      status.textContent = "The host returned an invalid state response.";
      return;
    }
    counter.textContent = String(response.state.counter);
    button.disabled = false;
    status.textContent = "Connected · state supplied by Kotlin.";
  }

  if (window.gtnHost) {
    window.gtnHost.onmessage = applyMessage;
    button.addEventListener("click", () => {
      button.disabled = true;
      send("increment_counter");
    });
    send("read_state");
  } else {
    status.textContent = "Secure host bridge unavailable. This page needs the app prototype host.";
    button.disabled = true;
  }
})();

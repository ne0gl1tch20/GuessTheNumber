"use strict";
const assert = require("node:assert/strict");
const fs = require("node:fs");
const vm = require("node:vm");

const source = fs.readFileSync(
  "app/src/main/assets/libraries/jarrlyyuisdk/components/jarrlyy-ui.js",
  "utf8"
);
const hostSource = fs.readFileSync(
  "app/src/main/assets/libraries/jarrlyyuisdk/components/jarrlyy-ui-host.js",
  "utf8"
);

const listeners = new Map();
const body = {
  nodeType: 1,
  setAttribute() {},
  removeAttribute() {},
  addEventListener(name, handler) {
    const list = listeners.get(name) || [];
    list.push(handler);
    listeners.set(name, list);
  },
  removeEventListener(name, handler) {
    listeners.set(name, (listeners.get(name) || []).filter((item) => item !== handler));
  },
  dispatchEvent(event) {
    (listeners.get(event.type) || []).slice().forEach((handler) => handler(event));
    return true;
  },
  querySelectorAll() { return []; },
};
const document = {
  readyState: "complete",
  body,
  documentElement: { classList: { add() {} } },
  activeElement: null,
  querySelector() { return null; },
  querySelectorAll() { return []; },
  addEventListener() {},
};
const window = {
  document,
  setTimeout,
  setInterval,
  clearInterval,
  CustomEvent,
};

vm.runInNewContext(source, { window, document, console });
assert.equal(typeof window.JarrlyyUI, "object");
assert.equal(typeof window.JarrlyyUI.init, "function");
assert.equal(typeof window.JarrlyyUI.toast, "function");
assert.equal(typeof window.JarrlyyUI.dialog, "function");
assert.equal(Object.isFrozen(window.JarrlyyUI), true);

// The motion primitives remain separate from the stable core API.
assert.equal(typeof window.JarrlyyUIMotion, "object");
assert.equal(typeof window.JarrlyyUIMotion.count, "function");
assert.equal(typeof window.JarrlyyUIMotion.progress, "function");
assert.equal(typeof window.JarrlyyUIMotion.floatText, "function");
const countTarget = { textContent: "0", dataset: {} };
window.JarrlyyUIMotion.count(countTarget, 42, { from: 0, duration: 0 });
assert.equal(countTarget.textContent, "42");

const attributes = {};
const meter = {
  setAttribute(name, value) { attributes[name] = value; },
  getAttribute(name) { return attributes[name] || null; },
  matches() { return false; },
  querySelector() { return null; },
  style: { setProperty(name, value) { attributes[name] = value; } },
};
window.JarrlyyUIMotion.progress(meter, 150);
assert.equal(attributes["aria-valuenow"], "100");
assert.equal(attributes["--jui-progress"], "100%");

// Initialization remains safe when called more than once.
assert.doesNotThrow(() => window.JarrlyyUI.init());
assert.doesNotThrow(() => window.JarrlyyUI.init());

// Load the host adapter as a browser/WebView host would.
vm.runInNewContext(hostSource, { window, document, console });
assert.equal(typeof window.JarrlyyUIHost.mount, "function");
const host = window.JarrlyyUIHost.mount({ root: body, theme: "light" });
assert.equal(typeof host.on, "function");
assert.equal(typeof host.emit, "function");
let received = null;
const unsubscribe = host.on("jui:change", (event) => { received = event.detail.value; });
assert.equal(host.emit("jui:change", { value: "compact" }), true);
assert.equal(received, "compact");
unsubscribe();
received = null;
host.emit("jui:change", { value: "after-unsubscribe" });
assert.equal(received, null);
assert.equal(host.emit("unsafe-event", {}), false);

console.log("Jarrlyy UI SDK core, motion and host adapter runtime checks passed.");

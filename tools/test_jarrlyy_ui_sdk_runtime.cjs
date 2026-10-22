"use strict";
const assert = require("node:assert/strict");
const fs = require("node:fs");
const vm = require("node:vm");

const source = fs.readFileSync(
  "app/src/main/assets/libraries/jarrlyyuisdk/components/jarrlyy-ui.js",
  "utf8"
);

// A deliberately minimal DOM: querySelector returns one node (or null), while
// querySelectorAll returns an array-like collection. This catches accidental
// use of the single-element selector in collection initializers.
const document = {
  readyState: "complete",
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

// Initialization must also be safe when the host calls it after first load.
assert.doesNotThrow(() => window.JarrlyyUI.init());
assert.doesNotThrow(() => window.JarrlyyUI.init());

console.log("Jarrlyy UI SDK runtime initialization passed.");

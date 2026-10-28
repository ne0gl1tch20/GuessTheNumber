import assert from "node:assert/strict";
import {
  humanize,
  prefersReducedMotion,
  setIconButtonLabel,
  setTextContent,
  isJarrlyyEventName,
  mountHost,
  setHostTheme,
  setLoading,
  setProgress,
} from "../app/src/main/assets/libraries/jarrlyyuisdk/typescript/dist/index.js";

let reduceMotion = false;
globalThis.window = {
  matchMedia(query) {
    assert.equal(query, "(prefers-reduced-motion: reduce)");
    return { matches: reduceMotion };
  },
};

assert.equal(humanize("crystal_gardens"), "Crystal Gardens");
assert.equal(humanize("nebula-reach"), "Nebula Reach");
assert.equal(humanize("", "Untitled"), "Untitled");
const textNode = { textContent: "Before" };
assert.equal(setTextContent(textNode, "After"), true);
assert.equal(textNode.textContent, "After");
assert.equal(setTextContent(textNode, "After"), false);
assert.equal(setTextContent(textNode, null, "Fallback"), true);
assert.equal(textNode.textContent, "Fallback");
assert.equal(setTextContent(null, "ignored"), false);
assert.equal(prefersReducedMotion(), false);
reduceMotion = true;
assert.equal(prefersReducedMotion(), true);
assert.equal(typeof setIconButtonLabel, "function");

function makeRoot() {
  const attrs = new Map();
  const listeners = new Map();
  return {
    nodeType: 1,
    attrs,
    listeners,
    setAttribute(key, value) { attrs.set(key, String(value)); },
    removeAttribute(key) { attrs.delete(key); },
    addEventListener(name, handler) {
      const list = listeners.get(name) || [];
      list.push(handler);
      listeners.set(name, list);
    },
    removeEventListener(name, handler) {
      listeners.set(name, (listeners.get(name) || []).filter((item) => item !== handler));
    },
    dispatchEvent(event) {
      for (const handler of listeners.get(event.type) || []) handler(event);
      return true;
    },
  };
}
class FakeCustomEvent {
  constructor(type, options = {}) {
    this.type = type;
    this.detail = options.detail;
    this.bubbles = options.bubbles;
  }
}
const root = makeRoot();
const doc = { body: root, documentElement: root };
let initCount = 0;
const hostWindow = {
  CustomEvent: FakeCustomEvent,
  JarrlyyUI: { init() { initCount++; } },
  JarrlyyUIAdvanced: { init() { initCount++; } },
  JarrlyyUIMotion: { init(target) { assert.equal(target, root); initCount++; } },
};
const host = mountHost({ root, theme: "light" }, hostWindow, doc);
assert.ok(host);
assert.equal(root.attrs.get("data-jui-theme"), "light");
assert.equal(initCount, 3);
assert.equal(isJarrlyyEventName("jui:tab-change"), true);
assert.equal(isJarrlyyEventName("click"), false);
let received;
const off = host.on("jui:change", (event) => { received = event.detail; });
assert.equal(host.emit("jui:change", { value: "compact" }), true);
assert.deepEqual(received, { value: "compact" });
off();
host.emit("jui:change", { value: "ignored" });
assert.deepEqual(received, { value: "compact" });
assert.equal(host.emit("click"), false);
host.setTheme("system");
assert.equal(root.attrs.has("data-jui-theme"), false);
setHostTheme(root, "dark", doc);
assert.equal(root.attrs.get("data-jui-theme"), "dark");

const button = {
  dataset: {},
  disabled: false,
  textContent: "Save",
  attrs: new Map(),
  children: [],
  setAttribute(key, value) { this.attrs.set(key, String(value)); },
  removeAttribute(key) { this.attrs.delete(key); },
  replaceChildren(...children) { this.children = children; this.textContent = ""; },
  append(...children) { this.children.push(...children); },
};
const fakeDocument = {
  createElement(tag) {
    return {
      tagName: tag,
      attrs: new Map(),
      className: "",
      textContent: "",
      setAttribute(key, value) { this.attrs.set(key, String(value)); },
    };
  },
};
setLoading(button, true, "Saving…", fakeDocument);
assert.equal(button.disabled, true);
assert.equal(button.attrs.get("aria-busy"), "true");
assert.equal(button.children.length, 2);
assert.equal(button.children[1].textContent, "Saving…");
setLoading(button, false, undefined, fakeDocument);
assert.equal(button.textContent, "Save");
assert.equal(button.disabled, false);
assert.equal(button.attrs.has("aria-busy"), false);

const progressLabel = { textContent: "" };
const progressNode = {
  style: { values: new Map(), setProperty(key, value) { this.values.set(key, value); } },
  attrs: new Map(),
  setAttribute(key, value) { this.attrs.set(key, String(value)); },
  querySelector(selector) { return selector === ".jui-circular-progress-value" ? progressLabel : null; },
};
assert.equal(setProgress(progressNode, 120), 100);
assert.equal(progressNode.attrs.get("aria-valuenow"), "100");
assert.equal(progressLabel.textContent, "100%");
assert.equal(setProgress(progressNode, -4), 0);
assert.equal(setProgress(null, 50), null);

console.log("Jarrlyy UI TypeScript SDK tests passed.");

"use strict";
const assert = require("node:assert/strict");
const fs = require("node:fs");
const vm = require("node:vm");

const source = fs.readFileSync("app/src/main/assets/web-play/vue-canvas-effects.js", "utf8");
const listeners = new Map();
const calls = { fillRect: 0, clearRect: 0, cancel: 0, resizeDisconnect: 0 };
const rafQueue = new Map();
let nextRaf = 1;
const canvas = {
  style: {},
  getContext() {
    return {
      setTransform() {},
      clearRect() { calls.clearRect += 1; },
      save() {}, restore() {}, translate() {}, rotate() {},
      fillRect() { calls.fillRect += 1; },
    };
  },
};
const mount = {};
const window = {
  innerWidth: 320,
  innerHeight: 640,
  devicePixelRatio: 3,
  matchMedia: () => ({ matches: false }),
  addEventListener(type, fn) {
    const list = listeners.get(type) || [];
    list.push(fn);
    listeners.set(type, list);
  },
  removeEventListener(type, fn) {
    listeners.set(type, (listeners.get(type) || []).filter((item) => item !== fn));
  },
  dispatchEvent(event) {
    (listeners.get(event.type) || []).slice().forEach((fn) => fn(event));
    return true;
  },
};
const documentListeners = new Map();
const document = {
  hidden: false,
  documentElement: { clientWidth: 320, clientHeight: 640 },
  body: {},
  getElementById(id) { return id === "gtnVueCanvasEffects" ? mount : null; },
  addEventListener(type, fn) {
    const list = documentListeners.get(type) || [];
    list.push(fn);
    documentListeners.set(type, list);
  },
  removeEventListener(type, fn) {
    documentListeners.set(type, (documentListeners.get(type) || []).filter((item) => item !== fn));
  },
};
class ResizeObserver {
  constructor(callback) { this.callback = callback; }
  observe() {}
  disconnect() { calls.resizeDisconnect += 1; }
}
window.Vue = {
  h: () => ({}),
  createApp(component) {
    return {
      mount() {
        const instance = { $el: canvas };
        for (const [name, fn] of Object.entries(component.methods)) instance[name] = fn.bind(instance);
        component.render.call(instance);
        component.mounted.call(instance);
        instance.beforeUnmount = component.beforeUnmount.bind(instance);
        window.__component = instance;
        return instance;
      },
    };
  },
};
const context = {
  window,
  document,
  ResizeObserver,
  getComputedStyle: () => ({ getPropertyValue: (name) => ({
    "--accent": "#9b7cff",
    "--accent2": "#62e8ff",
    "--gold": "#ffd66b",
    "--good": "#63f5a4",
    "--danger": "#ff7195",
  }[name] || "") }),
  requestAnimationFrame(callback) {
    const id = nextRaf++;
    rafQueue.set(id, callback);
    return id;
  },
  cancelAnimationFrame(id) {
    calls.cancel += 1;
    rafQueue.delete(id);
  },
  console,
};
vm.runInNewContext(source, context, { filename: "vue-canvas-effects.js" });
assert.equal(window.gtnVueCanvasEffectsReady, true, "Vue effect layer should mount");
assert.equal(canvas.width, 640, "drawing buffer should cap DPR at 2");
assert.equal(canvas.height, 1280, "drawing buffer should cap DPR at 2");

window.dispatchEvent({ type: "gtn:canvas-burst", detail: { x: 90, y: 120 } });
assert.ok(rafQueue.size > 0, "burst should schedule a frame");
const [frameId, frame] = rafQueue.entries().next().value;
rafQueue.delete(frameId);
frame(16);
assert.ok(calls.fillRect > 0, "frame should draw Canvas particles");
assert.ok(rafQueue.size > 0, "remaining particles should schedule another frame");

document.hidden = true;
(documentListeners.get("visibilitychange") || []).forEach((fn) => fn());
assert.equal(window.__component.particles.length, 0, "hidden pages should clear particles");
assert.ok(calls.cancel > 0, "hidden pages should cancel animation work");

window.__component.beforeUnmount();
assert.equal(window.gtnVueCanvasEffectsReady, false, "unmount should clear ready flag");
assert.equal((listeners.get("gtn:canvas-burst") || []).length, 0, "unmount should remove event listener");
assert.equal(calls.resizeDisconnect, 1, "unmount should disconnect ResizeObserver");

const html = fs.readFileSync("app/src/main/assets/web-play/index.html", "utf8");
const vueRuntime = fs.readFileSync("app/src/main/assets/libraries/vue/vue.global.prod.js", "utf8");
const version = fs.readFileSync("app/src/main/assets/libraries/vue/VERSION.txt", "utf8");
const vueTag = html.indexOf('src="../libraries/vue/vue.global.prod.js"');
const effectsTag = html.indexOf('src="vue-canvas-effects.js"');
const appTag = html.indexOf('src="app.js"');
assert.ok(vueTag >= 0 && effectsTag > vueTag && appTag > effectsTag, "Vue runtime and Canvas module must load before app.js");
assert.match(html, /script-src 'self'/, "script CSP must stay local-only");
assert.match(html, /connect-src 'none'/, "runtime network access must remain disabled");
assert.match(vueRuntime, /vue v3\.5\.22/i, "vendored runtime must match the pinned Vue version");
assert.match(version, /Vue 3\.5\.22/, "runtime version metadata must match");
assert.match(html, /id="gtnVueCanvasEffects"/, "Canvas layer mount point must exist");

const noVueWindow = {};
const noVueDocument = { getElementById: () => ({}) };
vm.runInNewContext(source, { window: noVueWindow, document: noVueDocument });
assert.equal(noVueWindow.gtnVueCanvasEffectsReady, false, "missing Vue should fail closed to the legacy celebration");
console.log("Vue Canvas effects lifecycle, reduced workload and cleanup checks passed.");

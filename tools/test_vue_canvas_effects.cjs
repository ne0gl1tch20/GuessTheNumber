"use strict";
const assert = require("node:assert/strict");
const fs = require("node:fs");
const vm = require("node:vm");

const source = fs.readFileSync("app/src/main/assets/libraries/jarrlyyuisdk/components/jarrlyy-particle-effects.js", "utf8");
const html = fs.readFileSync("app/src/main/assets/web-play/index.html", "utf8");
const vueRuntime = fs.readFileSync("app/src/main/assets/libraries/vue/vue.global.prod.js", "utf8");
const vueVersion = fs.readFileSync("app/src/main/assets/libraries/vue/VERSION.txt", "utf8");
const pixiRuntime = fs.readFileSync("app/src/main/assets/libraries/pixi/pixi.min.js", "utf8");
const pixiVersion = fs.readFileSync("app/src/main/assets/libraries/pixi/VERSION.txt", "utf8");
const pixiLicense = fs.readFileSync("app/src/main/assets/libraries/pixi/LICENSE.txt", "utf8");
const styles = fs.readFileSync("app/src/main/assets/web-play/styles.css", "utf8");

function createHarness({ pixi, contextAvailable = true } = {}) {
  const listeners = new Map();
  const documentListeners = new Map();
  const rafQueue = new Map();
  const calls = { fillRect: 0, clearRect: 0, cancel: 0, appDestroyed: 0, resizeRemoved: 0 };
  let nextRaf = 1;
  const context2d = {
    setTransform() {},
    clearRect() { calls.clearRect += 1; },
    save() {}, restore() {}, translate() {}, rotate() {},
    fillRect() { calls.fillRect += 1; },
  };
  const canvas = {
    style: {},
    parentNode: null,
    setAttribute() {},
    getContext() { return contextAvailable ? context2d : null; },
  };
  const mount = {
    children: [],
    appendChild(child) { child.parentNode = this; this.children.push(child); },
    removeChild(child) { this.children = this.children.filter((item) => item !== child); child.parentNode = null; },
    getBoundingClientRect() { return { width: 320, height: 640 }; },
  };
  const window = {
    innerWidth: 320,
    innerHeight: 640,
    devicePixelRatio: 3,
    PIXI: pixi,
    matchMedia: () => ({ matches: false }),
    addEventListener(type, fn) {
      const list = listeners.get(type) || [];
      list.push(fn);
      listeners.set(type, list);
    },
    removeEventListener(type, fn) {
      listeners.set(type, (listeners.get(type) || []).filter((item) => item !== fn));
      if (type === "resize") calls.resizeRemoved += 1;
    },
    dispatchEvent(event) {
      (listeners.get(event.type) || []).slice().forEach((fn) => fn(event));
      return true;
    },
  };
  const document = {
    hidden: false,
    documentElement: { clientWidth: 320, clientHeight: 640 },
    body: {},
    querySelector(selector) { return selector === "[data-jarrlyy-particle-effects]" ? mount : null; },
    createElement(tag) { assert.equal(tag, "canvas"); return canvas; },
    addEventListener(type, fn) {
      const list = documentListeners.get(type) || [];
      list.push(fn);
      documentListeners.set(type, list);
    },
    removeEventListener(type, fn) {
      documentListeners.set(type, (documentListeners.get(type) || []).filter((item) => item !== fn));
    },
  };
  window.Vue = {
    h: () => ({}),
    createApp(component) {
      return {
        mount() {
          const instance = { $el: mount };
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
  return { window, document, mount, canvas, listeners, documentListeners, rafQueue, calls, context };
}

async function main() {
  const vueTag = html.indexOf('src="../libraries/vue/vue.global.prod.js"');
  const adapterTag = html.indexOf('src="vue-jarrlyy-adapter.js"');
  const pixiTag = html.indexOf('src="../libraries/pixi/pixi.min.js"');
  const effectsTag = html.indexOf('src="../libraries/jarrlyyuisdk/components/jarrlyy-particle-effects.js"');
  const appTag = html.indexOf('src="app.js"');
  assert.ok(vueTag >= 0 && adapterTag > vueTag && pixiTag > adapterTag && effectsTag > pixiTag && appTag > effectsTag, "Vue, SDK adapter, local PixiJS and effects module must load before app.js");
  assert.match(html, /script-src 'self'/, "script CSP must stay local-only");
  assert.match(html, /connect-src 'none'/, "runtime network access must remain disabled");
  assert.match(vueRuntime, /vue v3\.5\.22/i, "vendored runtime must match the pinned Vue version");
  assert.match(vueVersion, /Vue 3\.5\.22/, "Vue version metadata must match");
  assert.ok(pixiRuntime.length > 100000, "the generated PixiJS production bundle must be present");
  assert.match(pixiLicense, /MIT License/i, "the vendored PixiJS MIT license must be included");
  assert.match(pixiVersion, /PixiJS 8\.22\.0/, "PixiJS version metadata must match");
  assert.match(html, /data-jarrlyy-particle-effects/, "effects mount point must exist");
  assert.doesNotMatch(html, /cdn\.jsdelivr\.net|unpkg\.com/i, "runtime libraries must not load from a CDN");
  assert.match(styles, /\.jarrlyy-particle-effects\{[^}]*pointer-events:none/, "effect overlay must not block UI input");
  assert.match(styles, /prefers-reduced-motion:reduce/, "effect layer must honor reduced-motion preferences");
  assert.match(source, /preference:'webgl'/, "PixiJS must use its WebGL renderer");

  const fallback = createHarness();
  vm.runInNewContext(source, fallback.context, { filename: "jarrlyy-particle-effects.js" });
  assert.equal(fallback.window.jarrlyyParticleEffectsReady, true, "Canvas fallback should mount without PixiJS");
  assert.equal(fallback.window.jarrlyyParticleEffectsRenderer, "canvas2d", "missing PixiJS should select Canvas 2D");
  assert.equal(fallback.canvas.width, 640, "fallback drawing buffer should cap DPR at 2");
  assert.equal(fallback.canvas.height, 1280, "fallback drawing buffer should cap DPR at 2");
  fallback.window.dispatchEvent({ type: "jarrlyy:particle-burst", detail: { x: 90, y: 120 } });
  assert.ok(fallback.rafQueue.size > 0, "Canvas fallback burst should schedule a frame");
  const [frameId, frame] = fallback.rafQueue.entries().next().value;
  fallback.rafQueue.delete(frameId);
  frame(16);
  assert.ok(fallback.calls.fillRect > 0, "Canvas fallback should draw particles");
  fallback.document.hidden = true;
  (fallback.documentListeners.get("visibilitychange") || []).forEach((fn) => fn());
  assert.equal(fallback.window.__component.particles.length, 0, "hidden pages should clear particles");
  assert.ok(fallback.calls.cancel > 0, "hidden pages should cancel Canvas animation work");
  fallback.window.__component.beforeUnmount();
  assert.equal(fallback.window.jarrlyyParticleEffectsReady, false, "unmount should clear ready flag");
  assert.equal((fallback.listeners.get("jarrlyy:particle-burst") || []).length, 0, "unmount should remove burst listener");
  assert.equal(fallback.mount.children.length, 0, "unmount should remove fallback canvas");

  let tick;
  const pixiCalls = { added: 0, removed: 0, destroyed: 0 };
  class FakeGraphics {
    rect() { return this; }
    fill() { return this; }
    position = { set() {} };
    destroy() {}
  }
  class FakeApplication {
    constructor() {
      this.canvas = { className: "", setAttribute() {}, parentNode: null };
      this.stage = {
        addChild() { pixiCalls.added += 1; },
        removeChild() { pixiCalls.removed += 1; },
      };
      this.ticker = { add(fn) { tick = fn; }, stop() {} };
    }
    async init() {}
    destroy() { pixiCalls.destroyed += 1; }
  }
  const pixi = createHarness({ pixi: { Application: FakeApplication, Graphics: FakeGraphics } });
  vm.runInNewContext(source, pixi.context, { filename: "jarrlyy-particle-effects.js" });
  await new Promise((resolve) => setImmediate(resolve));
  assert.equal(pixi.window.jarrlyyParticleEffectsReady, true, "PixiJS renderer should initialize");
  assert.equal(pixi.window.jarrlyyParticleEffectsRenderer, "pixi", "PixiJS should be the preferred renderer");
  pixi.window.dispatchEvent({ type: "jarrlyy:particle-burst", detail: { x: 100, y: 140 } });
  assert.equal(pixiCalls.added, 30, "PixiJS should create the burst particles");
  tick({ deltaTime: 1 });
  pixi.window.__component.beforeUnmount();
  assert.ok(pixiCalls.destroyed > 0, "unmount should destroy the PixiJS application");
  assert.ok(pixiCalls.removed > 0, "unmount should remove PixiJS particle graphics");

  const noVue = { window: {}, document: { querySelector: () => ({}) } };
  vm.runInNewContext(source, noVue);
  assert.equal(noVue.window.jarrlyyParticleEffectsReady, false, "missing Vue should fail closed");

  const noCanvas = createHarness({ contextAvailable: false });
  vm.runInNewContext(source, noCanvas.context);
  assert.equal(noCanvas.window.jarrlyyParticleEffectsReady, false, "unsupported Canvas fallback should fail closed");
  console.log("PixiJS renderer, Canvas fallback, reduced workload and cleanup checks passed.");
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});

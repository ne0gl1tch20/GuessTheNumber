"use strict";
const assert = require("node:assert/strict");
const fs = require("node:fs");
const vm = require("node:vm");

const source = fs.readFileSync("app/src/main/assets/web-play/vue-jarrlyy-adapter.js", "utf8");
const listeners = new Map();
const root = {
  appendChild() {},
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
};
const document = { querySelector: (selector) => selector === "#mount" ? root : null };
class CustomEvent {
  constructor(type, options = {}) { this.type = type; this.detail = options.detail; this.bubbles = options.bubbles; }
}
const calls = { toast: null, dialog: null, mounted: null, registered: null };
const window = {
  CustomEvent,
  JarrlyyUI: {
    toast(message, duration) { calls.toast = { message, duration }; },
    dialog(title, message, options) { calls.dialog = { title, message, options }; },
  },
  Vue: {
    h(type, props, children) { return { type, props, children }; },
    createApp(component, props) {
      return {
        component(name, definition) { calls.registered = { name, definition }; return this; },
        mount(target) { calls.mounted = { component, props, target }; return target; },
      };
    },
  },
};
vm.runInNewContext(source, { window, document });
const api = window.GTNVueJarrlyy;
assert.equal(api.available, true);
assert.equal(api.version, 1);
assert.equal(api.toast("Hello", 1200), true);
assert.deepEqual(calls.toast, { message: "Hello", duration: 1200 });
let confirmed = false;
assert.equal(api.dialog("Title", "Body", { confirmText: "OK", onConfirm: () => { confirmed = true; } }), true);
assert.equal(calls.dialog.title, "Title");
assert.equal(calls.dialog.message, "Body");
calls.dialog.options.onConfirm();
assert.equal(confirmed, true);

const mounted = api.mount({ name: "Example" }, "#mount", { value: 3 });
assert.equal(mounted, root);
assert.equal(calls.registered.name, "JarrlyyButton");
assert.equal(calls.mounted.target, root);
assert.equal(api.mount({}, "#missing"), null);

let received = null;
const unsubscribe = api.on(root, "jui:change", (event) => { received = event.detail.value; });
assert.equal(api.emit(root, "jui:change", { value: "ok" }), true);
assert.equal(received, "ok");
unsubscribe();
received = null;
api.emit(root, "jui:change", { value: "ignored" });
assert.equal(received, null);
assert.equal(api.emit(root, "unsafe-event", {}), false);

const vnode = api.Button.setup(
  { variant: "danger", type: "submit", disabled: true, ariaLabel: "Confirm" },
  { slots: { default: () => ["Confirm"] }, emit: (name, event) => { calls.click = { name, event }; } },
)();
assert.equal(vnode.type, "button");
assert.deepEqual(vnode.props.class, ["jui-button", "jui-button-danger"]);
assert.equal(vnode.props.type, "submit");
assert.equal(vnode.props.disabled, true);
vnode.props.onClick("click-event");
assert.deepEqual(calls.click, { name: "click", event: "click-event" });

console.log("Vue/Jarrlyy SDK adapter API and event-contract checks passed.");

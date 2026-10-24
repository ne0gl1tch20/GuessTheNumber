'use strict';
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

const listeners = new Map();
const created = [];
const document = {
  createElement(tag) {
    const element = {
      tagName: tag,
      attributes: {},
      style: {},
      contentWindow: {},
      removed: false,
      setAttribute(key, value) { this.attributes[key] = value; },
      remove() { this.removed = true; },
      srcdoc: ''
    };
    created.push(element);
    return element;
  }
};
const window = {
  addEventListener(type, listener) { listeners.set(type, listener); },
  removeEventListener(type) { listeners.delete(type); }
};
vm.runInNewContext(fs.readFileSync('app/src/main/assets/web-play/trait-extension-host.js', 'utf8'), { window, document, Set, TypeError, RangeError, String, JSON });
const updates = [];
const localMessages = [];
const targets = {
  status: { textContent: '' },
  unsafe: { textContent: 'unchanged' }
};
const container = {
  appendChild(frame) { this.frame = frame; },
  querySelector(selector) {
    const match = selector.match(/data-trait-text="([^"]+)"/);
    return match ? targets[match[1]] || null : null;
  }
};
const mounted = window.GTNTraitExtensionHost.mount({
  container,
  source: 'function renderTrait(api) { api.setText("status", "Ready"); }',
  allowedTextIds: ['status'],
  onLocalMessage: value => localMessages.push(value)
});
assert.equal(mounted.frame.attributes.sandbox, 'allow-scripts');
assert.match(mounted.frame.srcdoc, /Content-Security-Policy/);
assert.match(mounted.frame.srcdoc, /connect-src 'none'/);
assert.match(mounted.frame.srcdoc, /const source = "function renderTrait/);
assert.doesNotMatch(mounted.frame.srcdoc, /<script>function renderTrait/);
listeners.get('message')({ source: mounted.frame.contentWindow, data: { __gtnTraitExtension: true, operation: 'setText', id: 'status', value: 'Hello' } });
assert.equal(targets.status.textContent, 'Hello');
listeners.get('message')({ source: mounted.frame.contentWindow, data: { __gtnTraitExtension: true, operation: 'setText', id: 'unsafe', value: 'Injected' } });
assert.equal(targets.unsafe.textContent, 'unchanged');
listeners.get('message')({ source: {}, data: { __gtnTraitExtension: true, operation: 'setText', id: 'status', value: 'Spoofed' } });
assert.equal(targets.status.textContent, 'Hello');
listeners.get('message')({ source: mounted.frame.contentWindow, data: { __gtnTraitExtension: true, operation: 'showLocalMessage', value: 'Local only' } });
assert.deepEqual(localMessages, ['Local only']);
mounted.dispose();
assert.equal(mounted.frame.removed, true);
assert.equal(listeners.has('message'), false);
assert.throws(() => window.GTNTraitExtensionHost.mount({ container, source: 'bad' }), /must expose renderTrait/);
console.log('Trait extension host sandbox tests passed.');

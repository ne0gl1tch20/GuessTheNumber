'use strict';
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

const sourcePath = path.join(__dirname, '../app/src/main/assets/libraries/jarrlyyuisdk/components/jarrlyy-ui-dom-utils.js');
const source = fs.readFileSync(sourcePath, 'utf8');
const window = {};
const context = { window, globalThis: window, document: {
  createElementNS(namespace, tag) { return makeNode(tag); },
  createTextNode(text) { return { nodeType: 3, textContent: text }; }
}};
function makeNode(tag) {
  const attrs = {};
  const children = [];
  return {
    tag, attrs, children,
    setAttribute(key, value) { attrs[key] = value; },
    appendChild(child) { children.push(child); },
    querySelector(selector) { return selector === 'use' ? children.find(child => child.tag === 'use') || null : selector === 'svg' ? children.find(child => child.tag === 'svg') || null : null; },
    replaceChildren(...next) { children.splice(0, children.length, ...next); },
    replaceWith() {},
  };
}
vm.runInNewContext(source, context, { filename: sourcePath });
const api = window.JarrlyyUIDOM;
assert.ok(api, 'utility API is exposed');
assert.equal(api.humanize('crystal_gardens'), 'Crystal Gardens');
assert.equal(api.humanize('nebula-reach'), 'Nebula Reach');
assert.equal(api.humanize('', 'Untitled'), 'Untitled');
const node = makeNode('button');
api.setIconButtonLabel(node, 'icon-home', 'Home');
assert.equal(node.children.length, 2);
assert.equal(node.children[0].tag, 'svg');
assert.equal(node.children[0].children[0].attrs.href, '#icon-home');
assert.equal(node.children[1].textContent, ' Home');
api.setIconButtonLabel(null, 'icon-home', 'Home');
const app = fs.readFileSync(path.join(__dirname, '../app/src/main/assets/web-play/app.js'), 'utf8');
const html = fs.readFileSync(path.join(__dirname, '../app/src/main/assets/web-play/index.html'), 'utf8');
assert.ok(html.indexOf('jarrlyy-ui-dom-utils.js') < html.indexOf('app.js'), 'utility loads before app.js');
assert.ok(!/function\s+humanize\s*\(/.test(app), 'humanize implementation lives in the SDK');
assert.ok(!/function\s+setIconButtonLabel\s*\(/.test(app), 'icon button implementation lives in the SDK');
assert.ok(app.includes('JarrlyyUIDOM.humanize('));
assert.ok(app.includes('JarrlyyUIDOM.setIconButtonLabel('));
console.log('Jarrlyy UI DOM utility tests passed.');

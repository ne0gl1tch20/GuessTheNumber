/* Host-neutral DOM helpers shared by Jarrlyy UI consumers. No game state or storage. */
(function (root) {
  'use strict';

  function humanize(value, fallback = 'unknown realm') {
    return String(value || fallback)
      .split(/[_-]+/)
      .filter(Boolean)
      .map(part => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }

  function setIconButtonLabel(node, iconId, label) {
    if (!node) return;
    let svg = node.querySelector('svg');
    if (!svg) {
      svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
      svg.setAttribute('class', 'ui-icon');
      svg.setAttribute('viewBox', '0 0 24 24');
      svg.setAttribute('aria-hidden', 'true');
      svg.setAttribute('focusable', 'false');
      svg.appendChild(document.createElementNS('http://www.w3.org/2000/svg', 'use'));
    }
    let use = svg.querySelector('use');
    if (!use) {
      use = document.createElementNS('http://www.w3.org/2000/svg', 'use');
      svg.replaceChildren(use);
    }
    use.setAttribute('href', '#' + String(iconId || ''));
    node.replaceChildren(svg, document.createTextNode(' ' + String(label ?? '')));
  }

  root.JarrlyyUIDOM = Object.freeze({ humanize, setIconButtonLabel });
})(typeof window !== 'undefined' ? window : globalThis);

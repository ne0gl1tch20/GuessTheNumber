(function installTraitExtensionHost(global) {
  'use strict';

  const MAX_SOURCE_CHARS = 32768;
  const MAX_MESSAGE_CHARS = 240;
  const ID_PATTERN = /^[a-zA-Z0-9_-]{1,48}$/;

  function mount(options) {
    if (!options || !options.container || typeof options.source !== 'string') {
      throw new TypeError('A container and JavaScript source are required.');
    }
    if (options.source.length === 0 || options.source.length > MAX_SOURCE_CHARS) {
      throw new RangeError('Trait UI module source is empty or too large.');
    }
    if (!options.source.includes('renderTrait') && !options.source.includes('registerTraitView')) {
      throw new TypeError('Trait UI module must expose renderTrait(api) or registerTraitView(api).');
    }

    const supportedCapabilities = new Set(['render_ui', 'show_local_message']);
    const capabilities = new Set(options.capabilities || []);
    for (const capability of capabilities) {
      if (!supportedCapabilities.has(capability)) throw new TypeError('Unsupported trait UI capability: ' + capability);
    }
    const canRender = capabilities.has('render_ui');
    const canShowLocalMessage = capabilities.has('show_local_message');
    const allowedTextIds = new Set((options.allowedTextIds || []).filter(id => ID_PATTERN.test(id)));
    const frame = document.createElement('iframe');
    frame.setAttribute('sandbox', 'allow-scripts');
    frame.setAttribute('title', 'Trait extension');
    frame.setAttribute('referrerpolicy', 'no-referrer');
    frame.setAttribute('aria-label', 'Trait extension content');
    frame.style.width = '100%';
    frame.style.border = '0';
    frame.style.minHeight = '2.5rem';

    const sourceLiteral = JSON.stringify(options.source)
      .replace(/</g, '\\u003c')
      .replace(/\u2028/g, '\\u2028')
      .replace(/\u2029/g, '\\u2029');
    const apiMethods = [];
    if (canRender) {
      apiMethods.push(`setText(id, value) {
        if (typeof id !== 'string' || !/^[a-zA-Z0-9_-]{1,48}$/.test(id)) return;
        send({ operation: 'setText', id, value: String(value).slice(0, ${MAX_MESSAGE_CHARS}) });
      }`);
    }
    if (canShowLocalMessage) {
      apiMethods.push(`showLocalMessage(value) {
        send({ operation: 'showLocalMessage', value: String(value).slice(0, ${MAX_MESSAGE_CHARS}) });
      }`);
    }

    frame.srcdoc = `<!doctype html>
<meta charset="utf-8">
<meta http-equiv="Content-Security-Policy" content="default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; connect-src 'none'; img-src 'none'; media-src 'none'; object-src 'none'; base-uri 'none'; form-action 'none'">
<meta name="viewport" content="width=device-width,initial-scale=1">
<div id="trait-root" role="status"></div>
<script>
(function () {
  'use strict';
  const source = ${sourceLiteral};
  const send = message => parent.postMessage({ __gtnTraitExtension: true, ...message }, '*');
  const api = Object.freeze({ ${apiMethods.join(',')} });
  try {
    const script = document.createElement('script');
    script.textContent = source;
    document.head.appendChild(script);
    if (typeof renderTrait === 'function') renderTrait(api);
    else if (typeof registerTraitView === 'function') registerTraitView(api);
    else send({ operation: 'error', value: 'Trait UI entrypoint was not found.' });
  } catch (_) {
    send({ operation: 'error', value: 'Trait UI module could not be rendered.' });
  }
})();
</script>`;
    const onMessage = event => {
      if (event.source !== frame.contentWindow || !event.data ||
          event.data.__gtnTraitExtension !== true) return;
      const message = event.data;
      if (message.operation === 'setText' && canRender && allowedTextIds.has(message.id)) {
        const target = options.container.querySelector('[data-trait-text="' + message.id + '"]');
        if (target) target.textContent = String(message.value || '').slice(0, MAX_MESSAGE_CHARS);
      } else if (message.operation === 'showLocalMessage' && canShowLocalMessage && typeof options.onLocalMessage === 'function') {
        options.onLocalMessage(String(message.value || '').slice(0, MAX_MESSAGE_CHARS));
      }
    };
    global.addEventListener('message', onMessage);
    options.container.appendChild(frame);
    return {
      frame,
      dispose() {
        global.removeEventListener('message', onMessage);
        frame.remove();
      }
    };
  }

  global.GTNTraitExtensionHost = Object.freeze({ mount });
})(window);

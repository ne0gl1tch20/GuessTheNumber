// Windows-only host integration for the canonical shared HTML player.
(function () {
  'use strict';

  function showDesktopStartupError(title, detail) {
    function render() {
      if (!document.body || document.getElementById('gtnDesktopStartupError')) return;
      var panel = document.createElement('section');
      panel.id = 'gtnDesktopStartupError';
      panel.setAttribute('role', 'alert');
      panel.style.cssText = 'position:fixed;inset:0;z-index:2147483647;overflow:auto;padding:36px;box-sizing:border-box;background:#101522;color:#f4f6ff;font:14px/1.5 Segoe UI, sans-serif;';
      var heading = document.createElement('h1');
      heading.textContent = title || 'GuessTheNumber could not start';
      heading.style.cssText = 'font-size:22px;margin:0 0 12px;';
      var message = document.createElement('pre');
      message.textContent = String(detail || 'No additional error details were provided.');
      message.style.cssText = 'white-space:pre-wrap;overflow-wrap:anywhere;font:13px/1.5 Consolas,monospace;color:#d5def5;';
      panel.appendChild(heading);
      panel.appendChild(message);
      document.body.appendChild(panel);
    }
    if (document.body) render();
    else document.addEventListener('DOMContentLoaded', render, { once: true });
  }

  window.addEventListener('error', function (event) {
    var target = event.target;
    var detail = event.message || (target && (target.src || target.href)) || 'Unknown JavaScript or asset loading error';
    if (event.filename) detail += '\n' + event.filename + (event.lineno ? ':' + event.lineno : '');
    showDesktopStartupError('The shared game interface encountered an error', detail);
  }, true);
  window.addEventListener('unhandledrejection', function (event) {
    var reason = event.reason;
    showDesktopStartupError('The shared game interface failed during startup', reason && (reason.stack || reason.message) || String(reason || 'Unhandled promise rejection'));
  });

  window.gtnDesktopInitialScreen = window.gtnDesktopInitialScreen || 'play';
  window.gtnDesktopDebugTools = false;
  window.gtnDesktopFeatures = Object.freeze({ navigationPosition: true, launchAtStartup: true });
  window.gtnDesktopNavigationPosition = 'LEFT';
  window.gtnDesktopLaunchAtStartup = false;
  var debugEntries = [];
  function debugLog(message) {
    if (!window.gtnDesktopDebugTools) return;
    debugEntries.push(new Date().toLocaleTimeString() + '  ' + String(message));
    if (debugEntries.length > 160) debugEntries.shift();
    var output = document.getElementById('gtnDesktopDebugLog');
    if (output) output.textContent = debugEntries.join('\n');
    var count = document.getElementById('gtnDesktopDebugCount');
    if (count) count.textContent = String(debugEntries.length);
  }

  // Keep native replies until the shared app has installed its handler.
  var responseQueue = [];
  var messageHandler = null;
  function deliverResponse(raw) {
    var message = String(raw);
    debugLog('Kotlin → JS  ' + message.slice(0, 260));
    var parsed = null;
    try { parsed = JSON.parse(message); } catch (_) {}
    if (parsed) window.dispatchEvent(new CustomEvent('gtn-desktop-settings-response', { detail: parsed }));
    if (typeof messageHandler === 'function') {
      messageHandler({ data: message });
    } else {
      responseQueue.push(message);
      if (responseQueue.length > 64) responseQueue.shift();
    }
  }

  window.gtnHost = {
    get onmessage() { return messageHandler; },
    set onmessage(handler) {
      messageHandler = typeof handler === 'function' ? handler : null;
      if (!messageHandler) return;
      var queued = responseQueue.splice(0, responseQueue.length);
      queued.forEach(deliverResponse);
    },
    queue: [],
    postMessage: function (raw) {
      var message = String(raw);
      debugLog('JS → Kotlin  ' + message.slice(0, 220));
      if (window.gtnDesktopBridge) {
        try {
          window.gtnDesktopBridge.postMessage(message);
        } catch (error) {
          debugLog('Bridge post failed: ' + (error && error.message || String(error)));
          this.queue.push(message);
        }
      } else {
        this.queue.push(message);
      }
      if (this.queue.length > 128) this.queue.shift();
    },
    flush: function () {
      if (!window.gtnDesktopBridge) return;
      while (this.queue.length) {
        var message = this.queue.shift();
        try {
          window.gtnDesktopBridge.postMessage(message);
        } catch (error) {
          debugLog('Bridge flush failed: ' + (error && error.message || String(error)));
          this.queue.unshift(message);
          break;
        }
      }
    },
    receive: function (raw) { deliverResponse(raw); }
  };

  function applyNavigationPosition(position) {
    var value = String(position || 'LEFT').toUpperCase();
    if (!['LEFT', 'RIGHT', 'TOP', 'BOTTOM'].includes(value)) value = 'LEFT';
    document.documentElement.dataset.desktopNavigationPosition = value;
    if (document.body) document.body.dataset.desktopNavigationPosition = value;
  }

  function requestPosition(value) {
    applyNavigationPosition(value);
    window.gtnHost.postMessage(JSON.stringify({ version: 1, id: 'desktop-nav-' + Date.now(), type: 'update_setting', key: 'desktopNavigationPosition', value: String(value).toUpperCase() }));
  }

  function installDesktopSettings() {
    if (!window.gtnDesktopFeatures) return;
    var screen = document.querySelector('[data-screen="settings"]');
    if (!screen || screen.querySelector('[data-desktop-capabilities-setting]')) return;
    var group = document.createElement('div');
    group.className = 'settings-group desktop-capabilities-setting';
    group.dataset.desktopCapabilitiesSetting = 'true';
    group.innerHTML = '<h3>Windows Desktop</h3><p class="settings-description">These options are available only in the Windows desktop app.</p>' +
      '<div data-desktop-navigation-setting><label class="jui-field-label" for="desktopNavigationPosition">Navigation Bar Position</label>' +
      '<select id="desktopNavigationPosition" class="jui-field" aria-label="Navigation Bar Position"><option value="LEFT">Left (default)</option><option value="TOP">Top</option><option value="RIGHT">Right</option><option value="BOTTOM">Bottom</option></select></div>' +
      '<label class="desktop-startup-setting" for="desktopLaunchAtStartup"><input id="desktopLaunchAtStartup" type="checkbox" /> Launch GuessTheNumber when Windows starts</label>' +
      '<p class="settings-description">Uses your Windows account startup registration. You can turn it off here at any time.</p>';
    var target = screen.querySelector('.catalog-panel') || screen;
    target.appendChild(group);
    var select = group.querySelector('#desktopNavigationPosition');
    select.value = String(window.gtnDesktopNavigationPosition || 'LEFT').toUpperCase();
    select.addEventListener('change', function () { requestPosition(select.value); });
    var startup = group.querySelector('#desktopLaunchAtStartup');
    startup.checked = window.gtnDesktopLaunchAtStartup === true;
    startup.addEventListener('change', function () {
      window.gtnHost.postMessage(JSON.stringify({ version: 1, id: 'desktop-startup-' + Date.now(), type: 'update_setting', key: 'desktopLaunchAtStartup', value: startup.checked }));
    });
    window.addEventListener('gtn-desktop-settings-response', function (event) {
      var response = event.detail || {};
      if (response.id && response.id.indexOf('desktop-startup-') === 0 && response.type === 'error') {
        startup.checked = !startup.checked;
        startup.setAttribute('aria-invalid', 'true');
        startup.title = response.message || 'Could not update Windows startup setting.';
      } else if (response.id && response.id.indexOf('desktop-startup-') === 0 && response.type === 'state') {
        startup.removeAttribute('aria-invalid');
        startup.title = response.state && response.state.message || '';
      }
    });
  }

  function installDebugOverlay() {
    if (!window.gtnDesktopDebugTools || document.getElementById('gtnDesktopDebugRoot')) return;
    var root = document.createElement('div');
    root.id = 'gtnDesktopDebugRoot';
    root.style.cssText = 'position:fixed;right:14px;top:14px;z-index:2147483000;font:12px/1.45 Consolas,monospace;color:#eef2ff;';
    var toggle = document.createElement('button');
    toggle.textContent = 'DEV';
    toggle.style.cssText = 'border:1px solid #8b7cff;border-radius:9px;background:#17152b;color:#fff;padding:8px 13px;font-weight:800;cursor:pointer;box-shadow:0 4px 18px #0008;';
    var panel = document.createElement('section');
    panel.hidden = true;
    panel.style.cssText = 'position:absolute;right:0;top:42px;width:min(520px,calc(100vw - 36px));max-height:min(72vh,650px);overflow:auto;box-sizing:border-box;padding:13px;border:1px solid #514b7b;border-radius:12px;background:#111522f5;box-shadow:0 12px 40px #000a;';
    panel.innerHTML = '<div style="display:flex;justify-content:space-between;align-items:center;gap:8px"><b style="font:700 14px Segoe UI,sans-serif">Desktop Developer Overlay</b><span id="gtnDesktopDebugCount">0</span></div>' +
      '<p id="gtnDesktopDebugStatus" style="color:#b9c3df;margin:8px 0">Bridge: checking…</p>' +
      '<div style="display:flex;flex-wrap:wrap;gap:6px;margin-bottom:8px"><button data-dev-action="state">Read game state</button><button data-dev-action="page">Page info</button><button data-dev-action="clear">Clear log</button></div>' +
      '<textarea id="gtnDesktopDebugCommand" rows="3" spellcheck="false" placeholder="JavaScript command…" style="box-sizing:border-box;width:100%;resize:vertical;background:#090d17;color:#e9edff;border:1px solid #414a65;border-radius:7px;padding:8px"></textarea>' +
      '<button data-dev-action="run" style="margin:7px 0">Run JavaScript</button><pre id="gtnDesktopDebugResult" style="white-space:pre-wrap;overflow-wrap:anywhere;color:#c8f7d0"></pre>' +
      '<pre id="gtnDesktopDebugLog" style="white-space:pre-wrap;overflow-wrap:anywhere;max-height:240px;overflow:auto;background:#080c14;padding:8px;border-radius:7px"></pre>';
    Array.prototype.forEach.call(panel.querySelectorAll('button'), function (button) {
      button.style.cssText = 'border:1px solid #555d7c;border-radius:7px;background:#242a3d;color:#fff;padding:6px 9px;cursor:pointer';
    });
    toggle.addEventListener('click', function () { panel.hidden = !panel.hidden; });
    panel.addEventListener('click', function (event) {
      var action = event.target && event.target.getAttribute('data-dev-action');
      if (!action) return;
      if (action === 'clear') { debugEntries.length = 0; debugLog('Log cleared'); return; }
      if (action === 'state') { window.gtnHost.postMessage(JSON.stringify({version:1,id:'desktop-dev-state-'+Date.now(),type:'read_state'})); return; }
      if (action === 'page') {
        document.getElementById('gtnDesktopDebugResult').textContent = JSON.stringify({ title:document.title,url:location.href,readyState:document.readyState,host:typeof window.gtnHost,bridge:typeof window.gtnDesktopBridge,activateScreen:typeof window.gtnActivateScreen,screen:window.gtnDesktopInitialScreen },null,2); return;
      }
      if (action === 'run') {
        var source = document.getElementById('gtnDesktopDebugCommand').value;
        try {
          var result = (new Function(source))();
          Promise.resolve(result).then(function (value) { document.getElementById('gtnDesktopDebugResult').textContent = String(value); debugLog('JS eval completed'); }).catch(function (error) { document.getElementById('gtnDesktopDebugResult').textContent = error && (error.stack || error.message) || String(error); debugLog('JS eval rejected'); });
        } catch (error) {
          document.getElementById('gtnDesktopDebugResult').textContent = error && (error.stack || error.message) || String(error); debugLog('JS eval threw');
        }
      }
    });
    root.appendChild(toggle); root.appendChild(panel); document.body.appendChild(root);
    debugLog('Developer overlay attached');
    document.getElementById('gtnDesktopDebugStatus').textContent = 'Bridge: ' + (window.gtnDesktopBridge ? 'connected' : 'waiting') + ' · Kotlin handler: ready when request is received';
  }

  function initialize() {
    try {
      applyNavigationPosition(window.gtnDesktopNavigationPosition);
      installDebugOverlay();
      installDesktopSettings();
      if (typeof window.gtnActivateScreen !== 'function') {
        showDesktopStartupError('The game interface did not initialize', 'app.js did not register window.gtnActivateScreen. Check the earlier script-loading errors and packaged web assets.');
        return;
      }
      window.gtnActivateScreen(window.gtnDesktopInitialScreen || 'play');
      window.gtnHost.flush();
    } catch (error) {
      showDesktopStartupError('The game interface could not open its first screen', error && (error.stack || error.message) || String(error));
    }
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', initialize, { once: true });
  else initialize();
  new MutationObserver(installDesktopSettings).observe(document.documentElement, { childList: true, subtree: true });
  window.gtnDesktopApplyNavigationPosition = applyNavigationPosition;
})();

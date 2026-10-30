// Windows-only host integration for the canonical shared HTML player.
(function () {
  'use strict';

  window.gtnDesktopInitialScreen = window.gtnDesktopInitialScreen || 'play';
  window.gtnDesktopFeatures = Object.freeze({
    navigationPosition: true,
    launchAtStartup: true
  });
  window.gtnDesktopNavigationPosition = 'LEFT';
  window.gtnDesktopLaunchAtStartup = false;
  window.gtnHost = {
    onmessage: null,
    queue: [],
    postMessage: function (raw) {
      var message = String(raw);
      if (window.gtnDesktopBridge) window.gtnDesktopBridge.postMessage(message);
      else this.queue.push(message);
    },
    flush: function () {
      if (!window.gtnDesktopBridge) return;
      while (this.queue.length) window.gtnDesktopBridge.postMessage(this.queue.shift());
    },
    receive: function (raw) {
      var parsed = null;
      try { parsed = JSON.parse(String(raw)); } catch (_) {}
      if (parsed) window.dispatchEvent(new CustomEvent('gtn-desktop-settings-response', { detail: parsed }));
      if (typeof this.onmessage === 'function') this.onmessage({ data: String(raw) });
    }
  };

  function applyNavigationPosition(position) {
    var value = String(position || 'LEFT').toUpperCase();
    if (!['LEFT', 'RIGHT', 'TOP', 'BOTTOM'].includes(value)) value = 'LEFT';
    document.documentElement.dataset.desktopNavigationPosition = value;
    if (document.body) document.body.dataset.desktopNavigationPosition = value;
  }

  function requestPosition(value) {
    applyNavigationPosition(value);
    window.gtnHost.postMessage(JSON.stringify({
      version: 1,
      id: 'desktop-nav-' + Date.now(),
      type: 'update_setting',
      key: 'desktopNavigationPosition',
      value: String(value).toUpperCase()
    }));
  }

  function installDesktopSettings() {
    if (!window.gtnDesktopFeatures) return;
    var screen = document.querySelector('[data-screen="settings"]');
    if (!screen || screen.querySelector('[data-desktop-capabilities-setting]')) return;
    var group = document.createElement('div');
    group.className = 'settings-group desktop-capabilities-setting';
    group.dataset.desktopCapabilitiesSetting = 'true';
    group.innerHTML = '<h3>Windows Desktop</h3>' +
      '<p class="settings-description">These options are available only in the Windows desktop app.</p>' +
      '<div data-desktop-navigation-setting>' +
        '<label class="jui-field-label" for="desktopNavigationPosition">Navigation Bar Position</label>' +
        '<select id="desktopNavigationPosition" class="jui-field" aria-label="Navigation Bar Position">' +
        '<option value="LEFT">Left (default)</option><option value="TOP">Top</option><option value="RIGHT">Right</option><option value="BOTTOM">Bottom</option>' +
        '</select>' +
      '</div>' +
      '<label class="desktop-startup-setting" for="desktopLaunchAtStartup">' +
        '<input id="desktopLaunchAtStartup" type="checkbox" /> Launch GuessTheNumber when Windows starts' +
      '</label>' +
      '<p class="settings-description">Uses your Windows account startup registration. You can turn it off here at any time.</p>';
    var target = screen.querySelector('.catalog-panel') || screen;
    target.appendChild(group);
    var select = group.querySelector('#desktopNavigationPosition');
    select.value = String(window.gtnDesktopNavigationPosition || 'LEFT').toUpperCase();
    select.addEventListener('change', function () { requestPosition(select.value); });
    var startup = group.querySelector('#desktopLaunchAtStartup');
    startup.checked = window.gtnDesktopLaunchAtStartup === true;
    startup.addEventListener('change', function () {
      window.gtnHost.postMessage(JSON.stringify({
        version: 1,
        id: 'desktop-startup-' + Date.now(),
        type: 'update_setting',
        key: 'desktopLaunchAtStartup',
        value: startup.checked
      }));
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

  function initialize() {
    applyNavigationPosition(window.gtnDesktopNavigationPosition);
    installDesktopSettings();
    if (typeof window.gtnActivateScreen === 'function') {
      window.gtnActivateScreen(window.gtnDesktopInitialScreen || 'play');
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initialize, { once: true });
  } else {
    initialize();
  }
  new MutationObserver(installDesktopSettings).observe(document.documentElement, {
    childList: true,
    subtree: true
  });
  window.gtnDesktopApplyNavigationPosition = applyNavigationPosition;
})();

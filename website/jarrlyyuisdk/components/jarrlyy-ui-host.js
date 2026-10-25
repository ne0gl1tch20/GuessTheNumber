/* Jarrlyy UI SDK · host adapter for browser, Android WebView and Windows WebView. */
(function (global) {
  "use strict";
  const doc = global.document;
  if (!doc || global.JarrlyyUIHost) return;

  function resolveRoot(root) {
    if (root && root.nodeType === 9) return root.body || root.documentElement;
    return root || doc.body || doc.documentElement;
  }

  function setTheme(root, theme) {
    const target = resolveRoot(root);
    if (!target || typeof target.setAttribute !== "function") return;
    if (theme === "light" || theme === "dark") target.setAttribute("data-jui-theme", theme);
    else target.removeAttribute("data-jui-theme");
  }

  function validEventName(name) {
    return typeof name === "string" && /^jui:[a-z0-9:-]+$/i.test(name);
  }

  function mount(options = {}) {
    const root = resolveRoot(options.root);
    if (!root) return null;
    setTheme(root, options.theme);
    if (global.JarrlyyUI && typeof global.JarrlyyUI.init === "function") global.JarrlyyUI.init();
    if (global.JarrlyyUIAdvanced && typeof global.JarrlyyUIAdvanced.init === "function") global.JarrlyyUIAdvanced.init();
    if (global.JarrlyyUIMotion && typeof global.JarrlyyUIMotion.init === "function") global.JarrlyyUIMotion.init(root);

    return Object.freeze({
      root,
      setTheme: (theme) => setTheme(root, theme),
      on: (name, handler) => {
        if (!validEventName(name) || typeof handler !== "function") return () => {};
        root.addEventListener(name, handler);
        return () => root.removeEventListener(name, handler);
      },
      emit: (name, detail) => {
        if (!validEventName(name)) return false;
        root.dispatchEvent(new global.CustomEvent(name, { bubbles: true, detail }));
        return true;
      }
    });
  }

  global.JarrlyyUIHost = Object.freeze({ mount, setTheme });
  const ready = () => mount({ root: doc.body });
  if (doc.readyState === "loading") doc.addEventListener("DOMContentLoaded", ready, { once: true });
  else ready();
})(window);

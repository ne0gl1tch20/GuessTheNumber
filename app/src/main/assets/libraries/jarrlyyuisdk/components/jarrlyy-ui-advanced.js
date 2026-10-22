/* Jarrlyy UI SDK · optional interactive component layer */
(function (global) {
  "use strict";
  const doc = global.document;
  if (!doc || global.JarrlyyUIAdvanced) return;
  const $ = (selector, root = doc) => root.querySelector(selector);
  const $$ = (selector, root = doc) => Array.from(root.querySelectorAll(selector));
  const emit = (node, name, detail) => node.dispatchEvent(new global.CustomEvent(name, { bubbles: true, detail }));
  const initialized = new WeakSet();

  function initTabs() {
    $$("[data-jui-tabs]").forEach((root) => {
      if (initialized.has(root)) return;
      initialized.add(root);
      const tabs = $$('[role="tab"][data-jui-tab]', root);
      const panels = $$("[data-jui-tab-panel]", root);
      if (!tabs.length || !panels.length) return;
      const activate = (tab, focus = false) => {
        const key = tab.dataset.juiTab;
        tabs.forEach((item) => {
          const active = item === tab;
          item.setAttribute("aria-selected", String(active));
          item.tabIndex = active ? 0 : -1;
        });
        panels.forEach((panel) => {
          const active = panel.dataset.juiTabPanel === key;
          panel.hidden = !active;
          panel.setAttribute("aria-hidden", String(!active));
        });
        emit(root, "jui:tab-change", { value: key });
        if (focus) tab.focus({ preventScroll: true });
      };
      tabs.forEach((tab, index) => {
        tab.addEventListener("click", () => activate(tab));
        tab.addEventListener("keydown", (event) => {
          let next = index;
          if (event.key === "ArrowRight" || event.key === "ArrowDown") next = (index + 1) % tabs.length;
          else if (event.key === "ArrowLeft" || event.key === "ArrowUp") next = (index - 1 + tabs.length) % tabs.length;
          else if (event.key === "Home") next = 0;
          else if (event.key === "End") next = tabs.length - 1;
          else return;
          event.preventDefault();
          activate(tabs[next], true);
        });
      });
      activate(tabs.find((tab) => tab.getAttribute("aria-selected") === "true") || tabs[0]);
    });
  }

  function initSegmentedControls() {
    $$("[data-jui-segmented]").forEach((root) => {
      if (initialized.has(root)) return;
      initialized.add(root);
      const buttons = $$("[data-jui-segment]", root);
      const activate = (button, focus = false) => {
        if (button.disabled) return;
        buttons.forEach((item) => item.setAttribute("aria-pressed", String(item === button)));
        emit(root, "jui:change", { value: button.dataset.juiSegment });
        if (focus) button.focus({ preventScroll: true });
      };
      buttons.forEach((button, index) => {
        button.addEventListener("click", () => activate(button));
        button.addEventListener("keydown", (event) => {
          let next = index;
          if (event.key === "ArrowRight" || event.key === "ArrowDown") next = (index + 1) % buttons.length;
          else if (event.key === "ArrowLeft" || event.key === "ArrowUp") next = (index - 1 + buttons.length) % buttons.length;
          else if (event.key === "Home") next = 0;
          else if (event.key === "End") next = buttons.length - 1;
          else return;
          event.preventDefault();
          activate(buttons[next], true);
        });
      });
      if (buttons.length && !buttons.some((button) => button.getAttribute("aria-pressed") === "true")) activate(buttons[0]);
    });
  }

  function initRangeGroups() {
    $$("[data-jui-range]").forEach((root) => {
      if (initialized.has(root)) return;
      initialized.add(root);
      const low = $('[data-jui-range-low]', root);
      const high = $('[data-jui-range-high]', root);
      const fill = $(".jui-range-fill", root);
      const lowOut = $('[data-jui-range-value="low"]', root);
      const highOut = $('[data-jui-range-value="high"]', root);
      if (!low || !high) return;
      const min = Number(low.min || 0);
      const max = Number(high.max || 100);
      const paint = (source) => {
        let lo = Number(low.value), hi = Number(high.value);
        if (lo > hi) {
          if (source === low) low.value = String(hi);
          else high.value = String(lo);
          lo = Number(low.value); hi = Number(high.value);
        }
        if (lowOut) lowOut.textContent = String(lo);
        if (highOut) highOut.textContent = String(hi);
        if (fill) {
          fill.style.left = ((lo - min) / (max - min || 1) * 100) + "%";
          fill.style.width = ((hi - lo) / (max - min || 1) * 100) + "%";
        }
        emit(root, "jui:range-change", { min: lo, max: hi });
      };
      low.addEventListener("input", () => paint(low));
      high.addEventListener("input", () => paint(high));
      paint();
    });
  }

  function initOverlays() {
    $$("[data-jui-overlay-open]").forEach((button) => {
      if (initialized.has(button)) return;
      initialized.add(button);
      button.addEventListener("click", () => {
        const selector = button.dataset.juiOverlayOpen;
        let overlay;
        try { overlay = $(selector); } catch (_) { overlay = null; }
        if (!overlay || overlay.tagName !== "DIALOG") return;
        overlay.dataset.juiOpener = "true";
        if (typeof overlay.showModal === "function") overlay.showModal();
        else overlay.setAttribute("open", "");
        const target = $("button:not(:disabled), [href], input:not(:disabled), [tabindex]:not([tabindex='-1'])", overlay);
        if (target) target.focus({ preventScroll: true });
      });
    });
    $$("[data-jui-overlay-close]").forEach((button) => {
      if (initialized.has(button)) return;
      initialized.add(button);
      button.addEventListener("click", () => {
        const overlay = button.closest("dialog");
        if (overlay && typeof overlay.close === "function") overlay.close();
        else if (overlay) overlay.removeAttribute("open");
      });
    });
    $$("dialog.jui-overlay").forEach((overlay) => {
      if (initialized.has(overlay)) return;
      initialized.add(overlay);
      overlay.addEventListener("click", (event) => {
        if (event.target === overlay) {
          const rect = overlay.getBoundingClientRect();
          if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) overlay.close();
        }
      });
      overlay.addEventListener("close", () => {
        const opener = $('[data-jui-overlay-open][data-jui-last-opener="true"]');
        if (opener) { opener.removeAttribute("data-jui-last-opener"); opener.focus({ preventScroll: true }); }
      });
    });
  }

  function initBanners() {
    $$("[data-jui-banner-dismiss]").forEach((button) => {
      if (initialized.has(button)) return;
      initialized.add(button);
      button.addEventListener("click", () => {
        const banner = button.closest(".jui-banner");
        if (!banner) return;
        banner.dispatchEvent(new global.CustomEvent("jui:dismiss", { bubbles: true }));
        banner.remove();
      });
    });
  }

  function setLoading(button, loading, label) {
    if (!button) return;
    if (loading) {
      if (!button.dataset.juiOriginalLabel) button.dataset.juiOriginalLabel = button.textContent.trim();
      if (!button.dataset.juiOriginalDisabled) button.dataset.juiOriginalDisabled = String(button.disabled);
      button.disabled = true;
      button.setAttribute("aria-busy", "true");
      button.replaceChildren();
      const spinner = doc.createElement("span");
      spinner.className = "jui-loading-indicator";
      spinner.setAttribute("aria-hidden", "true");
      const text = doc.createElement("span");
      text.textContent = label || "Loading…";
      button.append(spinner, text);
    } else {
      button.removeAttribute("aria-busy");
      button.textContent = button.dataset.juiOriginalLabel || label || "Done";
      button.disabled = button.dataset.juiOriginalDisabled === "true";
      delete button.dataset.juiOriginalLabel;
      delete button.dataset.juiOriginalDisabled;
    }
  }

  function setProgress(node, value) {
    if (!node) return;
    const progress = Math.max(0, Math.min(100, Number(value) || 0));
    node.style.setProperty("--jui-progress-value", String(progress));
    node.setAttribute("role", "progressbar");
    node.setAttribute("aria-valuemin", "0");
    node.setAttribute("aria-valuemax", "100");
    node.setAttribute("aria-valuenow", String(progress));
    const label = $(".jui-circular-progress-value", node);
    if (label) label.textContent = Math.round(progress) + "%";
  }

  function init() {
    initTabs();
    initSegmentedControls();
    initRangeGroups();
    initOverlays();
    initBanners();
    $$("[data-jui-loading-demo]").forEach((button) => {
      if (initialized.has(button)) return;
      initialized.add(button);
      button.addEventListener("click", () => {
        setLoading(button, true, "Working…");
        global.setTimeout(() => setLoading(button, false), 900);
      });
    });
    $$("[data-jui-progress-demo]").forEach((node) => setProgress(node, Number(node.dataset.juiProgressDemo || 0)));
  }

  global.JarrlyyUIAdvanced = Object.freeze({ init, setLoading, setProgress });
  if (doc.readyState === "loading") doc.addEventListener("DOMContentLoaded", init, { once: true });
  else init();
})(window);

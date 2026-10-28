/* Jarrlyy UI SDK · optional interactive component layer.
 * TypeScript source. Build with tsconfig.runtime.json to preserve the classic-script API.
 */
(() => {
  "use strict";

  type EventDetail = Record<string, unknown>;
  type AdvancedApi = {
    init: () => void;
    setLoading: (button: HTMLButtonElement | null, loading: boolean, label?: string) => void;
    setProgress: (node: HTMLElement | null, value: unknown) => number | null;
  };
  type JarrlyyWindow = Window & { JarrlyyUIAdvanced?: AdvancedApi };

  const global = window as JarrlyyWindow;
  const doc = global.document;
  if (!doc || global.JarrlyyUIAdvanced) return;

  const $ = <T extends Element = HTMLElement>(selector: string, root: ParentNode = doc): T | null =>
    root.querySelector<T>(selector);
  const $$ = <T extends Element = HTMLElement>(selector: string, root: ParentNode = doc): T[] =>
    Array.from(root.querySelectorAll<T>(selector));
  const emit = (node: Element, name: string, detail?: EventDetail): void => {
    node.dispatchEvent(new CustomEvent(name, { bubbles: true, detail }));
  };
  const initialized = new WeakSet<Element>();

  function initTabs(): void {
    $$<HTMLElement>("[data-jui-tabs]").forEach((root) => {
      if (initialized.has(root)) return;
      initialized.add(root);
      const tabs = $$<HTMLButtonElement>('[role="tab"][data-jui-tab]', root);
      const panels = $$<HTMLElement>("[data-jui-tab-panel]", root);
      if (!tabs.length || !panels.length) return;
      const activate = (tab: HTMLButtonElement, focus = false): void => {
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
        tab.addEventListener("keydown", (event: KeyboardEvent) => {
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

  function initSegmentedControls(): void {
    $$<HTMLElement>("[data-jui-segmented]").forEach((root) => {
      if (initialized.has(root)) return;
      initialized.add(root);
      const buttons = $$<HTMLButtonElement>("[data-jui-segment]", root);
      const activate = (button: HTMLButtonElement, focus = false): void => {
        if (button.disabled) return;
        buttons.forEach((item) => item.setAttribute("aria-pressed", String(item === button)));
        emit(root, "jui:change", { value: button.dataset.juiSegment });
        if (focus) button.focus({ preventScroll: true });
      };
      buttons.forEach((button, index) => {
        button.addEventListener("click", () => activate(button));
        button.addEventListener("keydown", (event: KeyboardEvent) => {
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

  function initRangeGroups(): void {
    $$<HTMLElement>("[data-jui-range]").forEach((root) => {
      if (initialized.has(root)) return;
      initialized.add(root);
      const low = $('[data-jui-range-low]', root) as HTMLInputElement | null;
      const high = $('[data-jui-range-high]', root) as HTMLInputElement | null;
      const fill = $(".jui-range-fill", root) as HTMLElement | null;
      const lowOut = $('[data-jui-range-value="low"]', root);
      const highOut = $('[data-jui-range-value="high"]', root);
      if (!low || !high) return;
      const min = Number(low.min || 0);
      const max = Number(high.max || 100);
      const paint = (source?: HTMLInputElement): void => {
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

  function initOverlays(): void {
    $$<HTMLButtonElement>("[data-jui-overlay-open]").forEach((button) => {
      if (initialized.has(button)) return;
      initialized.add(button);
      button.addEventListener("click", () => {
        const selector = button.dataset.juiOverlayOpen;
        let overlay: HTMLDialogElement | null = null;
        try { overlay = selector ? $(selector) as HTMLDialogElement | null : null; } catch { overlay = null; }
        if (!overlay || overlay.tagName !== "DIALOG") return;
        $$<HTMLElement>("[data-jui-overlay-open][data-jui-last-opener]").forEach((item) => item.removeAttribute("data-jui-last-opener"));
        button.setAttribute("data-jui-last-opener", "true");
        if (typeof overlay.showModal === "function") overlay.showModal();
        else overlay.setAttribute("open", "");
        const target = $("button:not(:disabled), [href], input:not(:disabled), [tabindex]:not([tabindex='-1'])", overlay) as HTMLElement | null;
        if (target) target.focus({ preventScroll: true });
      });
    });
    $$<HTMLButtonElement>("[data-jui-overlay-close]").forEach((button) => {
      if (initialized.has(button)) return;
      initialized.add(button);
      button.addEventListener("click", () => {
        const overlay = button.closest("dialog") as HTMLDialogElement | null;
        if (overlay && typeof overlay.close === "function") overlay.close();
        else if (overlay) overlay.removeAttribute("open");
      });
    });
    $$<HTMLDialogElement>("dialog.jui-overlay").forEach((overlay) => {
      if (initialized.has(overlay)) return;
      initialized.add(overlay);
      overlay.addEventListener("click", (event: MouseEvent) => {
        if (event.target === overlay) {
          const rect = overlay.getBoundingClientRect();
          if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) overlay.close();
        }
      });
      overlay.addEventListener("close", () => {
        const opener = $('[data-jui-overlay-open][data-jui-last-opener="true"]') as HTMLElement | null;
        if (opener) { opener.removeAttribute("data-jui-last-opener"); opener.focus({ preventScroll: true }); }
      });
    });
  }

  function initBanners(): void {
    $$<HTMLButtonElement>("[data-jui-banner-dismiss]").forEach((button) => {
      if (initialized.has(button)) return;
      initialized.add(button);
      button.addEventListener("click", () => {
        const banner = button.closest(".jui-banner");
        if (!banner) return;
        emit(banner, "jui:dismiss");
        banner.remove();
      });
    });
  }

  function setLoading(button: HTMLButtonElement | null, loading: boolean, label?: string): void {
    if (!button) return;
    if (loading) {
      if (button.dataset.juiOriginalLabel === undefined) button.dataset.juiOriginalLabel = button.textContent?.trim() ?? "";
      if (button.dataset.juiOriginalDisabled === undefined) button.dataset.juiOriginalDisabled = String(button.disabled);
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

  function setProgress(node: HTMLElement | null, value: unknown): number | null {
    if (!node) return null;
    const numeric = Number(value);
    const progress = Math.max(0, Math.min(100, Number.isFinite(numeric) ? numeric : 0));
    node.style.setProperty("--jui-progress-value", String(progress));
    node.setAttribute("role", "progressbar");
    node.setAttribute("aria-valuemin", "0");
    node.setAttribute("aria-valuemax", "100");
    node.setAttribute("aria-valuenow", String(progress));
    const label = $(".jui-circular-progress-value", node);
    if (label) label.textContent = Math.round(progress) + "%";
    return progress;
  }

  function init(): void {
    initTabs();
    initSegmentedControls();
    initRangeGroups();
    initOverlays();
    initBanners();
    $$<HTMLButtonElement>("[data-jui-loading-demo]").forEach((button) => {
      if (initialized.has(button)) return;
      initialized.add(button);
      button.addEventListener("click", () => {
        setLoading(button, true, "Working…");
        global.setTimeout(() => setLoading(button, false), 900);
      });
    });
    $$<HTMLElement>("[data-jui-progress-demo]").forEach((node) => setProgress(node, Number(node.dataset.juiProgressDemo || 0)));
  }

  global.JarrlyyUIAdvanced = Object.freeze({ init, setLoading, setProgress });
  if (doc.readyState === "loading") doc.addEventListener("DOMContentLoaded", init, { once: true });
  else init();
})();

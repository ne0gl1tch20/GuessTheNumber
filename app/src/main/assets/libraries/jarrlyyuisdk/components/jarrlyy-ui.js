/* Jarrlyy UI SDK: small, dependency-free behavior layer. */
(function (global) {
  "use strict";
  const doc = global.document;
  if (!doc) return;
  const $ = (selector, root = doc) => root.querySelector(selector);
  const $$ = (selector, root = doc) => Array.from(root.querySelectorAll(selector));
  let tourSequence = 0;

  function toast(message, duration = 2800) {
    let region = $(".jui-toast-region");
    if (!region) {
      region = doc.createElement("div");
      region.className = "jui-toast-region";
      region.setAttribute("aria-live", "polite");
      region.setAttribute("aria-atomic", "false");
      doc.body.appendChild(region);
    }
    const item = doc.createElement("div");
    item.className = "jui-toast";
    item.setAttribute("role", "status");
    item.textContent = String(message);
    region.appendChild(item);
    global.setTimeout(() => item.remove(), Math.max(700, Number(duration) || 2800));
  }

  function makeDialog(title, message, options = {}) {
    const previousFocus = doc.activeElement;
    const dialog = doc.createElement("dialog");
    dialog.className = "jui-dialog";
    const heading = doc.createElement("h2");
    heading.textContent = String(title || "Notice");
    const body = doc.createElement("p");
    body.textContent = String(message || "");
    const actions = doc.createElement("div");
    actions.className = "jui-dialog-actions";
    const cancel = doc.createElement("button");
    cancel.type = "button";
    cancel.className = "jui-button";
    cancel.textContent = options.cancelText || "Close";
    cancel.addEventListener("click", () => dialog.close("cancel"));
    actions.appendChild(cancel);
    if (options.confirmText) {
      const confirm = doc.createElement("button");
      confirm.type = "button";
      confirm.className = "jui-button jui-button-primary";
      confirm.textContent = options.confirmText;
      confirm.addEventListener("click", () => {
        dialog.close("confirm");
        if (typeof options.onConfirm === "function") options.onConfirm();
      });
      actions.appendChild(confirm);
    }
    dialog.append(heading, body, actions);
    doc.body.appendChild(dialog);
    dialog.addEventListener("close", () => {
      dialog.remove();
      if (previousFocus && previousFocus.isConnected && typeof previousFocus.focus === "function") {
        previousFocus.focus({ preventScroll: true });
      }
    }, { once: true });
    if (typeof dialog.showModal === "function") dialog.showModal();
    else dialog.setAttribute("open", "");
    cancel.focus({ preventScroll: true });
    return dialog;
  }

  function tour(steps, options = {}) {
    const normalized = Array.isArray(steps) ? steps.filter((step) => step && (step.title || step.message || step.description)) : [];
    if (!normalized.length) return null;
    const previousFocus = doc.activeElement;
    const dialog = doc.createElement("dialog");
    dialog.className = "jui-dialog jui-tour-dialog";
    const heading = doc.createElement("h2");
    heading.id = "jui-tour-title-" + (++tourSequence);
    const body = doc.createElement("p");
    body.id = heading.id + "-description";
    const progress = doc.createElement("p");
    progress.className = "jui-tour-progress";
    progress.setAttribute("aria-live", "polite");
    const actions = doc.createElement("div");
    actions.className = "jui-dialog-actions";
    const skip = doc.createElement("button");
    skip.type = "button";
    skip.className = "jui-button";
    skip.textContent = options.skipText || "Skip tour";
    const previous = doc.createElement("button");
    previous.type = "button";
    previous.className = "jui-button";
    previous.textContent = options.previousText || "Previous";
    const next = doc.createElement("button");
    next.type = "button";
    next.className = "jui-button jui-button-primary";
    let index = 0;
    const render = () => {
      const step = normalized[index];
      heading.textContent = String(step.title || options.title || "Quick tour");
      body.textContent = String(step.message || step.description || "");
      progress.textContent = "Step " + (index + 1) + " of " + normalized.length;
      previous.disabled = index === 0;
      next.textContent = index === normalized.length - 1 ? (options.finishText || "Finish") : (options.nextText || "Next");
    };
    dialog.setAttribute("aria-labelledby", heading.id);
    dialog.setAttribute("aria-describedby", body.id);
    skip.addEventListener("click", () => {
      dialog.close("skip");
      if (typeof options.onSkip === "function") options.onSkip();
    });
    previous.addEventListener("click", () => {
      if (index > 0) index -= 1;
      render();
      previous.focus({ preventScroll: true });
    });
    next.addEventListener("click", () => {
      if (index < normalized.length - 1) {
        index += 1;
        render();
        next.focus({ preventScroll: true });
        return;
      }
      dialog.close("complete");
      if (typeof options.onComplete === "function") options.onComplete();
    });
    dialog.append(heading, body, progress, actions);
    actions.append(skip, previous, next);
    doc.body.appendChild(dialog);
    dialog.addEventListener("close", () => {
      dialog.remove();
      if (previousFocus && previousFocus.isConnected && typeof previousFocus.focus === "function") {
        previousFocus.focus({ preventScroll: true });
      }
    }, { once: true });
    render();
    if (typeof dialog.showModal === "function") dialog.showModal();
    else dialog.setAttribute("open", "");
    next.focus({ preventScroll: true });
    return dialog;
  }

  function initContextMenus() {
    $$("[data-jui-menu-toggle]").forEach((button) => {
      if (button.dataset.juiMenuReady) return;
      const id = button.getAttribute("aria-controls");
      const menu = id ? doc.getElementById(id) : button.parentElement.querySelector(".jui-menu");
      if (!menu) return;
      button.dataset.juiMenuReady = "true";
      button.setAttribute("aria-expanded", "false");
      const close = () => { menu.hidden = true; button.setAttribute("aria-expanded", "false"); };
      button.addEventListener("click", (event) => {
        event.stopPropagation();
        const opening = menu.hidden;
        $$("[data-jui-menu-toggle]").forEach((other) => {
          const otherId = other.getAttribute("aria-controls");
          const otherMenu = otherId ? doc.getElementById(otherId) : other.parentElement.querySelector(".jui-menu");
          if (otherMenu && otherMenu !== menu) {
            otherMenu.hidden = true;
            other.setAttribute("aria-expanded", "false");
          }
        });
        menu.hidden = !opening;
        button.setAttribute("aria-expanded", String(opening));
        if (opening) (menu.querySelector("button:not(:disabled)") || menu).focus({ preventScroll: true });
      });
      menu.addEventListener("click", (event) => {
        if (event.target.closest("button")) close();
      });
      menu.addEventListener("keydown", (event) => {
        const items = $$("button:not(:disabled)", menu);
        if (!items.length) return;
        if (event.key === "Tab") { close(); return; }
        const current = items.indexOf(doc.activeElement);
        let next = current;
        if (event.key === "ArrowDown") next = current < 0 ? 0 : (current + 1) % items.length;
        else if (event.key === "ArrowUp") next = current < 0 ? items.length - 1 : (current - 1 + items.length) % items.length;
        else if (event.key === "Home") next = 0;
        else if (event.key === "End") next = items.length - 1;
        else return;
        event.preventDefault();
        items[next].focus({ preventScroll: true });
      });
      doc.addEventListener("click", (event) => {
        if (!button.contains(event.target) && !menu.contains(event.target)) close();
      });
      doc.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && !menu.hidden) { close(); button.focus(); }
      });
    });
  }

  function initCarousels() {
    $$(".jui-carousel[data-jui-carousel]").forEach((carousel) => {
      if (carousel.dataset.juiCarouselReady) return;
      carousel.dataset.juiCarouselReady = "true";
      const slides = $$("[data-jui-slide]", carousel);
      const count = $("[data-jui-carousel-count]", carousel);
      let index = Math.max(0, slides.findIndex((slide) => !slide.hidden));
      const render = () => {
        slides.forEach((slide, i) => { slide.hidden = i !== index; slide.setAttribute("aria-hidden", String(i !== index)); });
        if (count) count.textContent = slides.length ? (index + 1) + " / " + slides.length : "0 / 0";
        $$("[data-jui-carousel-prev], [data-jui-carousel-next]", carousel).forEach((button) => { button.disabled = slides.length < 2; });
      };
      $("[data-jui-carousel-prev]", carousel)?.addEventListener("click", () => { if (slides.length < 2) return; index = (index + slides.length - 1) % slides.length; render(); });
      $("[data-jui-carousel-next]", carousel)?.addEventListener("click", () => { if (slides.length < 2) return; index = (index + 1) % slides.length; render(); });
      render();
    });
  }

  function initPagination() {
    $$("[data-jui-pagination]").forEach((root) => {
      if (root.dataset.juiPaginationReady) return;
      root.dataset.juiPaginationReady = "true";
      const buttons = $$("[data-jui-page]", root);
      const pages = root.closest("[data-jui-paginated]")?.querySelectorAll("[data-jui-page-content]") || [];
      const render = (page) => {
        buttons.forEach((button) => {
          const active = Number(button.dataset.juiPage) === page;
          if (active) button.setAttribute("aria-current", "page");
          else button.removeAttribute("aria-current");
        });
        pages.forEach((item) => { item.hidden = Number(item.dataset.juiPageContent) !== page; });
      };
      buttons.forEach((button) => button.addEventListener("click", () => render(Number(button.dataset.juiPage))));
      if (buttons.length) render(Number(buttons.find((button) => button.getAttribute("aria-current") === "page")?.dataset.juiPage || buttons[0].dataset.juiPage));
    });
  }

  function initTables() {
    $$("[data-jui-sort-key]").forEach((button) => {
      if (button.dataset.juiSortReady) return;
      button.dataset.juiSortReady = "true";
      button.addEventListener("click", () => {
        const table = button.closest("table");
        const body = table?.tBodies[0];
        if (!body) return;
        const column = Array.from(button.closest("tr").children).indexOf(button.closest("th"));
        const ascending = button.dataset.juiSortDirection !== "asc";
        button.dataset.juiSortDirection = ascending ? "asc" : "desc";
        const heading = button.closest("th");
        if (heading) heading.setAttribute("aria-sort", ascending ? "ascending" : "descending");
        const rows = Array.from(body.rows);
        rows.sort((a, b) => {
          const left = (a.cells[column]?.textContent || "").trim();
          const right = (b.cells[column]?.textContent || "").trim();
          const ln = Number(left.replace(/[^\d.-]/g, ""));
          const rn = Number(right.replace(/[^\d.-]/g, ""));
          const numeric = left !== "" && right !== "" && Number.isFinite(ln) && Number.isFinite(rn) && /[\d]/.test(left) && /[\d]/.test(right);
          const comparison = numeric ? ln - rn : left.localeCompare(right, undefined, { numeric: true, sensitivity: "base" });
          return ascending ? comparison : -comparison;
        });
        rows.forEach((row) => body.appendChild(row));
        $$("[data-jui-sort-key]", table).forEach((other) => {
          if (other !== button) {
            delete other.dataset.juiSortDirection;
            const otherHeading = other.closest("th");
            if (otherHeading) otherHeading.removeAttribute("aria-sort");
          }
        });
      });
    });
  }

  function initCharts() {
    $$("svg[data-jui-chart]").forEach((svg) => {
      if (svg.dataset.juiChartReady) return;
      svg.dataset.juiChartReady = "true";
      const values = String(svg.dataset.values || "").split(",").map(Number).filter(Number.isFinite).slice(0, 12);
      const labels = String(svg.dataset.labels || "").split(",");
      const max = Math.max(1, ...values);
      const width = 420, height = 150, pad = 18, gap = 8;
      const barWidth = values.length ? Math.max(8, (width - pad * 2 - gap * (values.length - 1)) / values.length) : 0;
      svg.setAttribute("viewBox", "0 0 " + width + " " + height);
      svg.replaceChildren();
      values.forEach((value, i) => {
        const barHeight = Math.max(2, (height - 38) * Math.max(0, value) / max);
        const rect = doc.createElementNS("http://www.w3.org/2000/svg", "rect");
        rect.setAttribute("x", String(pad + i * (barWidth + gap)));
        rect.setAttribute("y", String(height - 24 - barHeight));
        rect.setAttribute("width", String(barWidth));
        rect.setAttribute("height", String(barHeight));
        rect.setAttribute("aria-label", (labels[i] || "Item " + (i + 1)) + ": " + value);
        const title = doc.createElementNS("http://www.w3.org/2000/svg", "title");
        title.textContent = (labels[i] || "Item " + (i + 1)) + ": " + value;
        rect.appendChild(title);
        svg.appendChild(rect);
        const text = doc.createElementNS("http://www.w3.org/2000/svg", "text");
        text.setAttribute("x", String(pad + i * (barWidth + gap) + barWidth / 2));
        text.setAttribute("y", String(height - 7));
        text.setAttribute("text-anchor", "middle");
        text.textContent = labels[i] || String(i + 1);
        svg.appendChild(text);
      });
    });
  }

  function initTimers() {
    $$("[data-jui-timer]").forEach((node) => {
      if (node.dataset.juiTimerReady) return;
      node.dataset.juiTimerReady = "true";
      let remaining = Math.max(0, Number(node.dataset.juiTimer) || 0);
      let ended = false;
      const paint = () => {
        const mins = Math.floor(remaining / 60);
        const secs = remaining % 60;
        node.textContent = String(mins).padStart(2, "0") + ":" + String(secs).padStart(2, "0");
        if (remaining <= 0) {
          if (!ended) {
            ended = true;
            node.dispatchEvent(new global.CustomEvent("jui:timer-end", { bubbles: true }));
          }
          return false;
        }
        return true;
      };
      if (!paint()) return;
      const timer = global.setInterval(() => {
        remaining = Math.max(0, remaining - 1);
        if (!paint()) global.clearInterval(timer);
      }, 1000);
    });
  }

  function initDropzones() {
    $$("[data-jui-dropzone]").forEach((zone) => {
      if (zone.dataset.juiDropzoneReady) return;
      zone.dataset.juiDropzoneReady = "true";
      const input = $("input[type=file]", zone);
      const status = $("[data-jui-drop-status]", zone);
      const showFiles = (files) => {
        if (!status) return;
        status.textContent = files && files.length ? Array.from(files).map((file) => file.name).join(", ") : "No file selected";
      };
      input?.addEventListener("change", () => showFiles(input.files));
      ["dragenter", "dragover"].forEach((type) => zone.addEventListener(type, (event) => {
        event.preventDefault(); zone.classList.add("is-over");
      }));
      ["dragleave", "drop"].forEach((type) => zone.addEventListener(type, (event) => {
        event.preventDefault(); zone.classList.remove("is-over");
      }));
      zone.addEventListener("drop", (event) => showFiles(event.dataTransfer?.files));
    });
  }

  function initActions() {
    $$("[data-jui-action]").forEach((button) => {
      if (button.dataset.juiActionReady) return;
      button.dataset.juiActionReady = "true";
      button.addEventListener("click", () => {
        const action = button.dataset.juiAction;
        if (action === "toast") toast(button.dataset.juiMessage || "Nice! That component works.");
        if (action === "dialog") makeDialog(button.dataset.juiTitle || "Quick preview", button.dataset.juiMessage || "This dialog is created by the Jarrlyy UI SDK.", { confirmText: button.dataset.juiConfirm || "", onConfirm: () => toast("Confirmed.") });
        if (action === "update") makeDialog("Update available", button.dataset.juiMessage || "A new version is ready. You can review it before updating.", { cancelText: "Not now", confirmText: "Review update", onConfirm: () => toast("Connect this action to the host update flow.") });
        if (action === "tour") tour([
          { title: "Welcome to Jarrlyy UI", message: button.dataset.juiMessage || "Explore the reusable components in this local demo." },
          { title: "Reusable components", message: "Use buttons, inputs, navigation, game HUDs and feedback components across your app." },
          { title: "Built for your app", message: "Keep game rules and saved progress in the host app. Use the SDK for presentation and accessible interactions." }
        ]);
      });
    });
    $$("[data-jui-color-target]").forEach((input) => {
      if (input.dataset.juiColorReady) return;
      input.dataset.juiColorReady = "true";
      input.addEventListener("input", () => {
        const target = $(input.dataset.juiColorTarget);
        if (target) target.style.setProperty("--jui-preview-color", input.value);
        const output = input.parentElement.querySelector("[data-jui-color-value]");
        if (output) output.textContent = input.value.toUpperCase();
      });
    });
  }

  function init() {
    initContextMenus();
    initCarousels();
    initPagination();
    initTables();
    initCharts();
    initTimers();
    initDropzones();
    initActions();
  }

  global.JarrlyyUI = Object.freeze({ init, toast, dialog: makeDialog, tour });
  if (doc.readyState === "loading") doc.addEventListener("DOMContentLoaded", init, { once: true });
  else init();
})(window);
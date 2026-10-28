/** Framework-free, host-neutral DOM helpers for Jarrlyy UI consumers. */

export type TextFallback = string | number;

export function humanize(value: unknown, fallback = "unknown realm"): string {
  return String(value || fallback)
    .split(/[_-]+/)
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

/** Updates text safely and returns true only when the displayed text changes. */
export function setTextContent(
  node: Pick<Node, "textContent"> | null | undefined,
  value: unknown,
  fallback: TextFallback = 0,
): boolean {
  if (!node) return false;
  const next = String(value ?? fallback);
  if (node.textContent === next) return false;
  node.textContent = next;
  return true;
}

/** Reads the browser's reduced-motion preference without requiring it to exist. */
export function prefersReducedMotion(): boolean {
  return typeof window !== "undefined"
    && typeof window.matchMedia === "function"
    && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

/** Adds/reuses an SVG icon and accessible text in a button. */
export function setIconButtonLabel(
  node: (Element & Pick<HTMLElement, "replaceChildren" | "querySelector">) | null | undefined,
  iconId: string,
  label: string,
): void {
  if (!node || typeof document === "undefined") return;

  let svg = node.querySelector("svg");
  if (!svg) {
    svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    svg.setAttribute("class", "ui-icon");
    svg.setAttribute("viewBox", "0 0 24 24");
    svg.setAttribute("aria-hidden", "true");
    svg.setAttribute("focusable", "false");
    svg.appendChild(document.createElementNS("http://www.w3.org/2000/svg", "use"));
  }

  let use = svg.querySelector("use");
  if (!use) {
    use = document.createElementNS("http://www.w3.org/2000/svg", "use");
    svg.replaceChildren(use);
  }
  use.setAttribute("href", `#${iconId || ""}`);
  node.replaceChildren(svg, document.createTextNode(` ${label ?? ""}`));
}

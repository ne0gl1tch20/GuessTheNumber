/** Typed helpers shared by interactive Jarrlyy UI components. */

export type LoadingButton = HTMLButtonElement & {
  dataset: DOMStringMap & {
    juiOriginalLabel?: string;
    juiOriginalDisabled?: string;
  };
};

export function setLoading(
  button: LoadingButton | null | undefined,
  loading: boolean,
  label?: string,
  doc: Document = document,
): void {
  if (!button) return;

  if (loading) {
    if (!button.dataset.juiOriginalLabel) {
      button.dataset.juiOriginalLabel = button.textContent?.trim() ?? "";
    }
    if (button.dataset.juiOriginalDisabled === undefined) {
      button.dataset.juiOriginalDisabled = String(button.disabled);
    }
    button.disabled = true;
    button.setAttribute("aria-busy", "true");
    button.replaceChildren();

    const spinner = doc.createElement("span");
    spinner.className = "jui-loading-indicator";
    spinner.setAttribute("aria-hidden", "true");

    const text = doc.createElement("span");
    text.textContent = label || "Loading…";
    button.append(spinner, text);
    return;
  }

  button.removeAttribute("aria-busy");
  button.textContent = button.dataset.juiOriginalLabel || label || "Done";
  button.disabled = button.dataset.juiOriginalDisabled === "true";
  delete button.dataset.juiOriginalLabel;
  delete button.dataset.juiOriginalDisabled;
}

export function setProgress(
  node: HTMLElement | null | undefined,
  value: unknown,
): number | null {
  if (!node) return null;

  const numericValue = Number(value);
  const progress = Math.max(0, Math.min(100, Number.isFinite(numericValue) ? numericValue : 0));
  node.style.setProperty("--jui-progress-value", String(progress));
  node.setAttribute("role", "progressbar");
  node.setAttribute("aria-valuemin", "0");
  node.setAttribute("aria-valuemax", "100");
  node.setAttribute("aria-valuenow", String(progress));

  const label = node.querySelector<HTMLElement>(".jui-circular-progress-value");
  if (label) label.textContent = `${Math.round(progress)}%`;

  return progress;
}

/** Typed lifecycle and event adapter for browser and WebView hosts. */

export type JarrlyyTheme = "light" | "dark" | "system";

export type JarrlyyHostRoot = Document | HTMLElement;

export interface JarrlyyHostOptions {
  root?: JarrlyyHostRoot | null;
  theme?: JarrlyyTheme;
}

export interface JarrlyyMountedHost {
  readonly root: HTMLElement;
  setTheme(theme: JarrlyyTheme): void;
  on(name: string, handler: EventListener): () => void;
  emit(name: string, detail?: unknown): boolean;
}

interface OptionalInitializer {
  init?: (...args: unknown[]) => unknown;
}

interface JarrlyyHostWindow extends Window {
  JarrlyyUI?: OptionalInitializer;
  JarrlyyUIAdvanced?: OptionalInitializer;
  JarrlyyUIMotion?: OptionalInitializer;
}

/** Resolves Document roots to a concrete element and falls back to the page root. */
export function resolveHostRoot(
  root?: JarrlyyHostRoot | null,
  doc: Document = document,
): HTMLElement | null {
  if (root && root.nodeType === 9) {
    const documentRoot = root as Document;
    return documentRoot.body ?? documentRoot.documentElement;
  }
  return (root as HTMLElement | undefined) ?? doc.body ?? doc.documentElement;
}

export function setHostTheme(
  root: JarrlyyHostRoot | null | undefined,
  theme: JarrlyyTheme | undefined,
  doc: Document = document,
): void {
  const target = resolveHostRoot(root, doc);
  if (!target) return;

  if (theme === "light" || theme === "dark") {
    target.setAttribute("data-jui-theme", theme);
  } else {
    target.removeAttribute("data-jui-theme");
  }
}

export function isJarrlyyEventName(name: string): boolean {
  return /^jui:[a-z0-9:-]+$/i.test(name);
}

/**
 * Mounts SDK layers and exposes typed theme/event helpers.
 * Host apps still own routing, data, localization and business rules.
 */
export function mountHost(
  options: JarrlyyHostOptions = {},
  hostWindow: JarrlyyHostWindow = window as JarrlyyHostWindow,
  doc: Document = document,
): JarrlyyMountedHost | null {
  const root = resolveHostRoot(options.root, doc);
  if (!root) return null;

  setHostTheme(root, options.theme, doc);

  hostWindow.JarrlyyUI?.init?.();
  hostWindow.JarrlyyUIAdvanced?.init?.();
  hostWindow.JarrlyyUIMotion?.init?.(root);

  return Object.freeze({
    root,
    setTheme: (theme: JarrlyyTheme) => setHostTheme(root, theme, doc),
    on: (name: string, handler: EventListener) => {
      if (!isJarrlyyEventName(name) || typeof handler !== "function") return () => {};
      root.addEventListener(name, handler);
      return () => root.removeEventListener(name, handler);
    },
    emit: (name: string, detail?: unknown) => {
      if (!isJarrlyyEventName(name)) return false;
      root.dispatchEvent(new hostWindow.CustomEvent(name, { bubbles: true, detail }));
      return true;
    },
  });
}

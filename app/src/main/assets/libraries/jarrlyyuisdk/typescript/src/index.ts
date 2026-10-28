export {
  humanize,
  prefersReducedMotion,
  setIconButtonLabel,
  setTextContent,
} from "./dom-utils.js";

export type { TextFallback } from "./dom-utils.js";

export {
  isJarrlyyEventName,
  mountHost,
  resolveHostRoot,
  setHostTheme,
} from "./host-adapter.js";

export type {
  JarrlyyHostOptions,
  JarrlyyHostRoot,
  JarrlyyMountedHost,
  JarrlyyTheme,
} from "./host-adapter.js";

export { setLoading, setProgress } from "./advanced-utils.js";
export type { LoadingButton } from "./advanced-utils.js";

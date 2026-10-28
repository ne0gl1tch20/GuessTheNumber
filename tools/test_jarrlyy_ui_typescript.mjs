import assert from "node:assert/strict";
import {
  humanize,
  prefersReducedMotion,
  setTextContent,
} from "../app/src/main/assets/libraries/jarrlyyuisdk/typescript/dist/index.js";

let reduceMotion = false;
globalThis.window = {
  matchMedia(query) {
    assert.equal(query, "(prefers-reduced-motion: reduce)");
    return { matches: reduceMotion };
  },
};

assert.equal(humanize("crystal_gardens"), "Crystal Gardens");
assert.equal(humanize("nebula-reach"), "Nebula Reach");
assert.equal(humanize("", "Untitled"), "Untitled");

const node = { textContent: "Before" };
assert.equal(setTextContent(node, "After"), true);
assert.equal(node.textContent, "After");
assert.equal(setTextContent(node, "After"), false);
assert.equal(setTextContent(node, null, "Fallback"), true);
assert.equal(node.textContent, "Fallback");
assert.equal(setTextContent(null, "ignored"), false);

assert.equal(prefersReducedMotion(), false);
reduceMotion = true;
assert.equal(prefersReducedMotion(), true);

console.log("Compiled Jarrlyy UI TypeScript API tests passed.");

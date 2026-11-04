"use strict";

const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const packageRoot = path.join(root, "tools", "pixi-vendor", "node_modules", "pixi.js");
const outputRoot = path.join(root, "app", "src", "main", "assets", "libraries", "pixi");
const version = "8.22.0";
const bundle = path.join(packageRoot, "dist", "pixi.min.js");
const licenseCandidates = ["LICENSE.md", "LICENSE"];

if (!fs.existsSync(bundle)) {
  throw new Error("PixiJS bundle is missing. Run npm install --prefix tools/pixi-vendor first.");
}

fs.mkdirSync(outputRoot, { recursive: true });
fs.copyFileSync(bundle, path.join(outputRoot, "pixi.min.js"));
const license = licenseCandidates.map((name) => path.join(packageRoot, name)).find(fs.existsSync);
if (!license) throw new Error("PixiJS license file is missing from the installed package.");
fs.copyFileSync(license, path.join(outputRoot, "LICENSE.txt"));
fs.writeFileSync(path.join(outputRoot, "VERSION.txt"), "PixiJS " + version + "\n");
console.log("Vendored PixiJS " + version + " into " + path.relative(root, outputRoot));

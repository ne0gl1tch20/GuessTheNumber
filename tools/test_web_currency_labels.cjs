"use strict";
const assert = require("node:assert/strict");
const fs = require("node:fs");

const app = fs.readFileSync("app/src/main/assets/web-play/app.js", "utf8");
const html = fs.readFileSync("app/src/main/assets/web-play/index.html", "utf8");

const start = app.indexOf("function updateCurrencyBalances(s)");
assert.notEqual(start, -1, "currency balance updater should exist");
const end = app.indexOf("function renderDailyRewardPage(s)", start);
assert.ok(end > start, "currency balance updater should end before the next function");
const updater = app.slice(start, end);

assert.match(updater, /\['upgradeCurrencyBalance','Money',s\.money\]/,
  "regular upgrades must display the Money balance");
assert.match(updater, /\['shopCurrencyBalance','Nebula',s\.nebula\]/,
  "the shop must continue displaying Nebula");
assert.match(updater, /\['prestigeUpgradeCurrencyBalance','Prestige',s\.prestige\]/,
  "Prestige upgrades must retain their own currency");
assert.match(updater, /\['ultraUpgradeCurrencyBalance','Ultra',s\.ultra\]/,
  "Ultra upgrades must retain their own currency");
assert.match(html, /id="upgradeCurrencyBalance"[^>]*>Money: <strong>0<\/strong>/,
  "initial upgrades markup should show Money before state synchronization");
assert.match(html, /id="shopCurrencyBalance"[^>]*>Nebula: <strong>0<\/strong>/,
  "initial shop markup should continue showing Nebula");

console.log("Web currency labels correctly distinguish Money, Nebula, Prestige and Ultra.");

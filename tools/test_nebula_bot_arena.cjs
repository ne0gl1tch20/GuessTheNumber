const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const read = file => fs.readFileSync(path.join(root, file), 'utf8');
const html = read('app/src/main/assets/web-play/index.html');
const js = read('app/src/main/assets/web-play/app.js');
const canvas = read('app/src/main/assets/web-play/vue-bot-arena.js');
const vm = read('app/src/main/java/com/jarrlyyy/guessthenumber/ui/viewmodel/GameViewModel.kt');
const screen = read('app/src/main/java/com/jarrlyyy/guessthenumber/ui/screens/ProductionWebPlayScreen.kt');
const state = read('shared/src/jvmAndAndroidMain/kotlin/com/jarrlyyy/guessthenumber/domain/model/GameState.kt');
const catalog = JSON.parse(read('app/src/main/assets/game/shop_items.json'));
const ids = new Set(catalog.shopItems.map(item => item.id));
for (const id of ['auto_clicker_basic', 'bot_byte', 'bot_pixel', 'bot_speed_upgrade', 'bot_logic_upgrade']) {
  if (!ids.has(id)) throw new Error('Missing bot shop item: ' + id);
}
for (const marker of ['id="botArena"', 'id="gtnBotArenaCanvasHost"', 'id="playControlsDrawer"', 'vue-bot-arena.js']) {
  if (!html.includes(marker)) throw new Error('Missing HTML integration: ' + marker);
}
for (const marker of ['function renderBotArena(s)', 'gtn:bot-state', 'renderBotArena(s)']) {
  if (!js.includes(marker)) throw new Error('Missing bot state integration: ' + marker);
}
for (const marker of ['createApp(component)', 'gtn:bot-state', 'TOO_LOW', 'TOO_HIGH', 'CORRECT', 'beforeUnmount']) {
  if (!canvas.includes(marker)) throw new Error('Missing Canvas behavior: ' + marker);
}
for (const marker of ['lastBotGuess', 'lastBotFeedback', 'lastBotId', 'botActionSequence']) {
  if (!state.includes(marker) || !screen.includes(marker)) throw new Error('Missing persisted/bridged bot field: ' + marker);
}
for (const marker of ['bot_byte', 'bot_pixel', 'bot_speed_upgrade', 'bot_logic_upgrade', 'botActionSequence']) {
  if (!vm.includes(marker)) throw new Error('Missing authoritative bot behavior: ' + marker);
}
if (!screen.includes('.put("autoClickerActive", game.autoClickerActive)')) throw new Error('Missing automation state bridge field.');
if (!/<details\b[^>]*\bclass="play-controls-drawer"/.test(html) || !/<\/details>/.test(html)) throw new Error('Pull-up drawer structure missing.');
console.log('Nebula bot arena integration checks passed.');

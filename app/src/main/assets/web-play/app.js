(()=>{'use strict';
const host=window.gtnHost;
const $=id=>document.getElementById(id);
const el={
 range:$('rangeText'),feedback:$('feedback'),attempts:$('attempts'),correct:$('correct'),streak:$('streak'),best:$('bestStreak'),
 money:$('money'),income:$('income'),progress:$('progressFill'),input:$('guessInput'),button:$('guessButton'),
 status:$('statusLabel'),micro:$('microcopy'),toast:$('toast'),hero:document.querySelector('.hero-card'),
 prestige:$('prestige'),prestigeCount:$('prestigeCount'),ultra:$('ultra'),ultraCount:$('ultraCount'),nebula:$('nebula'),
 worldCount:$('worldCount'),activeWorld:$('activeWorld'),realmSubtext:$('realmSubtext'),bossVictories:$('bossVictories'),
 homeBase:$('homeBase'),riftTier:$('riftTier'),defeatedBosses:$('defeatedBosses'),
 bossPanel:$('bossPanel'),bossName:$('bossName'),bossHealthText:$('bossHealthText'),bossHealthFill:$('bossHealthFill'),
 claimedQuests:$('claimedQuests'),dailyGuesses:$('dailyGuesses'),dailyCorrect:$('dailyCorrect'),dailyBestStreak:$('dailyBestStreak'),
 questMessage:$('questMessage'),commonChests:$('commonChests'),rareChests:$('rareChests'),epicChests:$('epicChests')
};
let sequence=0,pending=null,toastTimer=0;
const requestId=()=>`webplay_${Date.now()}_${++sequence}`;
const reducedMotion=()=>window.matchMedia&&window.matchMedia('(prefers-reduced-motion: reduce)').matches;
function toast(message){el.toast.textContent=message;el.toast.classList.add('show');window.clearTimeout(toastTimer);toastTimer=window.setTimeout(()=>el.toast.classList.remove('show'),1700)}
function bump(node){const card=node.closest('.stat-card,.currency-tile,.progression-mini-grid>div,.quest-metrics>div,.loot-items>div');if(!card||reducedMotion())return;card.classList.remove('bump');void card.offsetWidth;card.classList.add('bump')}
function setText(node,value){if(!node)return;const next=String(value??0);if(node.textContent!==next){bump(node);node.textContent=next}}
function humanize(value){return String(value||'unknown realm').split(/[_-]+/).filter(Boolean).map(part=>part.charAt(0).toUpperCase()+part.slice(1)).join(' ')}
function burst(){if(reducedMotion())return;const field=document.createElement('div');field.className='celebration';for(let i=0;i<14;i++){const particle=document.createElement('i');const angle=Math.PI*2*i/14;particle.style.setProperty('--dx',`${Math.cos(angle)*(45+Math.random()*42)}px`);particle.style.setProperty('--dy',`${Math.sin(angle)*(45+Math.random()*42)}px`);particle.textContent=i%3===0?'✦':'·';field.appendChild(particle)}document.body.appendChild(field);window.setTimeout(()=>field.remove(),850)}
function render(state){
 if(!state||typeof state!=='object')return;
 setText(el.range,`${state.rangeMin} — ${state.rangeMax}`);
 setText(el.attempts,state.attempts);setText(el.correct,state.correctGuesses);setText(el.streak,state.streak);
 setText(el.best,`BEST ${state.bestStreak??0}`);setText(el.money,state.money);setText(el.income,state.incomePerSecond);
 const best=Math.max(1,Number(state.bestStreak??state.streak??1));
 el.progress.style.width=`${Math.min(100,Math.max(0,Number(state.streak??0))/best*100)}%`;
 setText(el.prestige,state.prestige);setText(el.prestigeCount,`${state.prestigeCount??0} ascensions`);
 setText(el.ultra,state.ultra);setText(el.ultraCount,`${state.ultraCount??0} ultra resets`);setText(el.nebula,state.nebula);
 setText(el.worldCount,`${state.unlockedWorldCount??1} WORLDS`);setText(el.activeWorld,humanize(state.activeWorldId));
 setText(el.realmSubtext,`${state.correctGuesses??0} correct guesses in your journey`);
 setText(el.bossVictories,state.worldBossVictories);setText(el.homeBase,`Lv. ${state.homeBaseLevel??0}`);
 setText(el.riftTier,`Tier ${state.endlessRiftBestTier??0}`);setText(el.defeatedBosses,state.defeatedBossCount);
 el.bossPanel.hidden=!state.activeBossBattle;
 if(state.activeBossBattle){
  setText(el.bossName,state.activeBossName||'Active boss');
  const total=Math.max(1,Number(state.activeBossHpTotal??1)),remaining=Math.max(0,Math.min(total,Number(state.activeBossHpRemaining??0)));
  setText(el.bossHealthText,`${remaining} / ${total} HP`);el.bossHealthFill.style.width=`${remaining/total*100}%`;
 }
 setText(el.claimedQuests,`${state.claimedDailyQuestCount??0} CLAIMED`);
 setText(el.dailyGuesses,state.dailyQuestGuesses);setText(el.dailyCorrect,state.dailyQuestCorrectGuesses);setText(el.dailyBestStreak,state.dailyQuestBestStreak);
 setText(el.questMessage,Number(state.dailyQuestCorrectGuesses??0)>0?'Nice run. Keep the momentum going.':'Every guess builds your run.');
 setText(el.commonChests,state.lootCommonChests);setText(el.rareChests,state.lootRareChests);setText(el.epicChests,state.lootEpicChests);
 const status=String(state.lastGuessStatus||'');
 const messages={CORRECT:'✨ Perfect hit. The run is accelerating.',TOO_LOW:'📈 Too low. Push the number higher.',TOO_HIGH:'📉 Too high. Bring it down.',rate_limited:'⏳ The engine needs a tiny breather.'};
 if(status){el.feedback.textContent=messages[status]||'Run updated.';el.feedback.classList.remove('pop');void el.feedback.offsetWidth;el.feedback.classList.add('pop');if(status==='CORRECT'){burst();el.hero.classList.add('success');window.setTimeout(()=>el.hero.classList.remove('success'),650)}}
}
function send(type,guess){if(!host||typeof host.postMessage!=='function'||pending)return false;const id=requestId();pending=id;el.button.disabled=true;const request={version:1,id,type};if(guess!==undefined)request.guess=guess;host.postMessage(JSON.stringify(request));return true}
function receive(raw){let message;try{message=JSON.parse(raw)}catch{return}if(!message||message.id!==pending)return;pending=null;el.button.disabled=false;if(message.type==='error'){el.status.textContent='READY';el.micro.textContent='The game engine rejected that request.';toast('Try that move again.');return}if(message.type!=='state'||!message.state)return;el.status.textContent='LIVE';render(message.state);if(message.state.accepted===false)toast('Guess rate-limited')}
if(host&&typeof host.postMessage==='function'){
 host.onmessage=event=>receive(event.data);
 $('guessForm').addEventListener('submit',event=>{event.preventDefault();const value=el.input.value.trim();if(!/^-?[0-9]+$/.test(value)||value.length>20){toast('Enter a whole number');return}if(send('submit_guess',value))el.input.value=''});
 $('beyondButton').addEventListener('click',()=>el.progressionPanel?.scrollIntoView({behavior:reducedMotion()?'auto':'smooth',block:'center'}));
 $('questButton').addEventListener('click',()=>$('questPanel')?.scrollIntoView({behavior:reducedMotion()?'auto':'smooth',block:'center'}));
 send('read_state');
}else{el.status.textContent='OFFLINE';el.micro.textContent='Game engine unavailable.'}
})();
(()=>{'use strict';
const mount=document.getElementById('gtnBotArenaCanvasHost');
const VueRuntime=window.Vue;
if(!mount||!VueRuntime||typeof VueRuntime.createApp!=='function'){window.gtnBotArenaReady=false;return;}
const {createApp,h}=VueRuntime;
const component={
 render(){return h('canvas',{class:'gtn-bot-arena-canvas','aria-hidden':'true'})},
 mounted(){
  this.canvas=this.$el;this.context=this.canvas.getContext('2d');this.width=1;this.height=1;
  this.state={bots:[],activeBotId:'',guess:'',feedback:'',sequence:0,active:false,reducedMotion:false};
  this.displayGuess='';this.typingIndex=0;this.typingStartedAt=0;this.frame=0;this.resizeObserver=null;
  this.onResize=()=>this.resize();this.onState=e=>this.acceptState(e&&e.detail||{});
  this.resizeObserver=typeof ResizeObserver==='function'?new ResizeObserver(this.onResize):null;
  if(this.resizeObserver)this.resizeObserver.observe(mount);else window.addEventListener('resize',this.onResize,{passive:true});
  window.addEventListener('gtn:bot-state',this.onState);document.addEventListener('visibilitychange',this.onVisibility);
  this.resize();this.draw();window.gtnBotArenaReady=true;
 },
 beforeUnmount(){
  window.gtnBotArenaReady=false;window.removeEventListener('gtn:bot-state',this.onState);window.removeEventListener('resize',this.onResize);
  document.removeEventListener('visibilitychange',this.onVisibility);if(this.resizeObserver)this.resizeObserver.disconnect();
  if(this.frame)cancelAnimationFrame(this.frame);this.frame=0;
 },
 methods:{
  resize(){if(!this.canvas||!this.context)return;const r=mount.getBoundingClientRect();this.width=Math.max(1,r.width);this.height=Math.max(1,r.height);const dpr=Math.min(2,Math.max(1,window.devicePixelRatio||1));this.canvas.width=Math.round(this.width*dpr);this.canvas.height=Math.round(this.height*dpr);this.context.setTransform(dpr,0,0,dpr,0,0);this.draw();},
  onVisibility(){if(document.hidden&&this.frame){cancelAnimationFrame(this.frame);this.frame=0;}else this.draw();},
  acceptState(next){
   const oldSequence=this.state.sequence;this.state={...this.state,...next};
   if(this.state.sequence!==oldSequence&&this.state.guess&&this.state.active&&!this.state.reducedMotion&&!document.hidden){this.displayGuess='';this.typingIndex=0;this.typingStartedAt=0;this.animate();}
   else{this.displayGuess=String(this.state.guess||'');this.typingIndex=this.displayGuess.length;this.draw();}
  },
  animate(){if(this.frame||document.hidden)return;this.frame=requestAnimationFrame(now=>this.tick(now));},
  tick(now){
   this.frame=0;if(document.hidden){this.draw();return;}
   if(!this.typingStartedAt)this.typingStartedAt=now;
   const elapsed=Math.max(0,now-this.typingStartedAt);this.typingIndex=Math.min(String(this.state.guess||'').length,Math.floor(elapsed/95));
   this.displayGuess=String(this.state.guess||'').slice(0,this.typingIndex);this.draw();
   if(this.typingIndex<String(this.state.guess||'').length)this.frame=requestAnimationFrame(next=>this.tick(next));
  },
  draw(){
   if(!this.context)return;const c=this.context,w=this.width,hgt=this.height;c.clearRect(0,0,w,hgt);
   c.fillStyle='#0b1227';c.fillRect(0,0,w,hgt);
   c.strokeStyle='rgba(119,145,255,.10)';c.lineWidth=1;
   for(let x=0;x<w;x+=28){c.beginPath();c.moveTo(x,0);c.lineTo(x,hgt);c.stroke();}
   for(let y=0;y<hgt;y+=28){c.beginPath();c.moveTo(0,y);c.lineTo(w,y);c.stroke();}
   for(let i=0;i<18;i++){const x=(i*67+13)%Math.max(1,w),y=(i*37+17)%Math.max(1,hgt);c.fillStyle='rgba(191,207,255,.55)';c.fillRect(x,y,2,2);}
   const bots=Array.isArray(this.state.bots)?this.state.bots:[];if(!bots.length)return;
   const gap=w/(bots.length+1),activeId=String(this.state.activeBotId||'');
   bots.slice(0,4).forEach((bot,index)=>{
    const x=gap*(index+1),baseY=hgt*.68,isActive=bot.id===activeId;
    const bob=isActive&&this.state.active?Math.sin(Date.now()/170)*2:0;
    const color=bot.id==='bot_byte'?'#53e0b3':bot.id==='bot_pixel'?'#ffb86b':'#a99aff';
    c.save();c.translate(x,baseY+bob);
    c.fillStyle='rgba(0,0,0,.22)';c.beginPath();c.ellipse(0,35,27,6,0,0,Math.PI*2);c.fill();
    c.fillStyle=color;c.beginPath();c.roundRect(-22,-31,44,42,12);c.fill();
    c.fillStyle='#10172c';c.beginPath();c.roundRect(-16,-24,32,21,7);c.fill();
    c.fillStyle='#dff6ff';c.beginPath();c.arc(-7,-14,3,0,Math.PI*2);c.arc(7,-14,3,0,Math.PI*2);c.fill();
    c.strokeStyle=color;c.lineWidth=4;c.beginPath();c.moveTo(-15,11);c.lineTo(-20,22);c.moveTo(15,11);c.lineTo(20,22);c.stroke();
    c.fillStyle=color;c.beginPath();c.roundRect(-16,13,10,18,4);c.roundRect(6,13,10,18,4);c.fill();
    c.strokeStyle=color;c.lineWidth=3;c.beginPath();c.moveTo(0,-31);c.lineTo(0,-40);c.stroke();c.beginPath();c.arc(0,-43,3,0,Math.PI*2);c.fill();
    c.fillStyle='#e8edff';c.font='bold 10px sans-serif';c.textAlign='center';c.fillText(bot.name||bot.id,0,48);
    if(isActive){
     const text=this.displayGuess||'…',bubbleW=Math.max(52,Math.min(120,text.length*9+20)),bubbleY=-66;
     c.fillStyle='#f3f5ff';c.beginPath();c.roundRect(-bubbleW/2,bubbleY,bubbleW,22,8);c.fill();
     c.fillStyle='#171d35';c.font='bold 11px monospace';c.fillText(this.typingIndex<String(this.state.guess||'').length?'⌨ '+text:text,0,bubbleY+15);
     c.fillStyle='#f3f5ff';c.beginPath();c.moveTo(-4,bubbleY+22);c.lineTo(3,bubbleY+22);c.lineTo(0,bubbleY+28);c.fill();
     if(this.typingIndex>=String(this.state.guess||'').length&&this.state.feedback){c.fillStyle=this.state.feedback==='CORRECT'?'#63f5a4':this.state.feedback==='TOO_LOW'?'#62e8ff':'#ffb86b';c.font='bold 9px sans-serif';c.fillText(this.state.feedback.replace('_',' '),0,-75);}
    }
    c.restore();
   });
  }
 }
};
createApp(component).mount(mount);
})();

(()=>{'use strict';
const mount=document.querySelector('[data-jarrlyy-particle-effects]');
const VueRuntime=window.Vue;
if(!mount||!VueRuntime||typeof VueRuntime.createApp!=='function'){
 window.jarrlyyParticleEffectsReady=false;
 return;
}
const {createApp,h}=VueRuntime;
const reducedMotion=()=>!!(window.matchMedia&&window.matchMedia('(prefers-reduced-motion: reduce)').matches);
const component={
 render(){return h('div',{class:'jarrlyy-particle-effects__surface','aria-hidden':'true'})},
 mounted(){
  this.host=this.$el;
  this.particles=[];
  this.destroyed=false;
  this.app=null;
  this.canvas=null;
  this.context=null;
  this.frame=0;
  this.lastFrame=0;
  this.onBurst=event=>this.burst(event);
  this.onVisibility=()=>this.visibilityChanged();
  this.onResize=()=>this.resizeFallback();
  window.addEventListener('jarrlyy:particle-burst',this.onBurst);
  document.addEventListener('visibilitychange',this.onVisibility);
  this.initializeRenderer();
 },
 beforeUnmount(){
  this.destroyed=true;
  window.jarrlyyParticleEffectsReady=false;
  window.removeEventListener('jarrlyy:particle-burst',this.onBurst);
  window.removeEventListener('resize',this.onResize);
  document.removeEventListener('visibilitychange',this.onVisibility);
  if(this.frame)cancelAnimationFrame(this.frame);
  this.frame=0;
  this.particles.forEach(p=>{if(p.view){if(this.app)this.app.stage.removeChild(p.view);p.view.destroy();}});
  this.particles.length=0;
  if(this.app){
   this.app.ticker.stop();
   this.app.destroy({removeView:true},{children:true});
   this.app=null;
  }
  if(this.canvas&&this.canvas.parentNode)this.canvas.parentNode.removeChild(this.canvas);
 },
 methods:{
  async initializeRenderer(){
   const PIXI=window.PIXI;
   if(PIXI&&typeof PIXI.Application==='function'&&typeof PIXI.Graphics==='function'){
    let app=null;
    try{
     app=new PIXI.Application();
     await app.init({
      backgroundAlpha:0,
      antialias:false,
      autoDensity:true,
      resolution:Math.min(2,Math.max(1,window.devicePixelRatio||1)),
      resizeTo:window,
      preference:'webgl'
     });
     if(this.destroyed){app.destroy({removeView:true},{children:true});return;}
     this.app=app;
     app.canvas.className='jarrlyy-particle-effects__canvas';
     app.canvas.setAttribute('aria-hidden','true');
     app.canvas.setAttribute('aria-label','');
     this.host.appendChild(app.canvas);
     this.onTick=ticker=>this.drawPixi(ticker);
     app.ticker.add(this.onTick);
     window.jarrlyyParticleEffectsRenderer='pixi';
     window.jarrlyyParticleEffectsReady=true;
     return;
    }catch(_error){
     if(app)app.destroy({removeView:true},{children:true});
     this.app=null;
    }
   }
   this.initializeCanvasFallback();
  },
  initializeCanvasFallback(){
   if(this.destroyed)return;
   const canvas=document.createElement('canvas');
   canvas.className='jarrlyy-particle-effects__canvas';
   canvas.setAttribute('aria-hidden','true');
   this.host.appendChild(canvas);
   this.canvas=canvas;
   this.context=canvas.getContext('2d',{alpha:true});
   if(!this.context){this.host.removeChild(canvas);this.canvas=null;window.jarrlyyParticleEffectsReady=false;return;}
   window.addEventListener('resize',this.onResize,{passive:true});
   this.resizeFallback();
   window.jarrlyyParticleEffectsRenderer='canvas2d';
   window.jarrlyyParticleEffectsReady=true;
  },
  resizeFallback(){
   if(!this.canvas||!this.context)return;
   const width=Math.max(1,window.innerWidth||document.documentElement.clientWidth);
   const height=Math.max(1,window.innerHeight||document.documentElement.clientHeight);
   const dpr=Math.min(2,Math.max(1,window.devicePixelRatio||1));
   this.canvas.width=Math.round(width*dpr);
   this.canvas.height=Math.round(height*dpr);
   this.context.setTransform(dpr,0,0,dpr,0,0);
   this.width=width;this.height=height;
  },
  palette(){
   const style=getComputedStyle(document.body);
   const colors=['--accent','--accent2','--gold','--good','--danger']
    .map(key=>style.getPropertyValue(key).trim()).filter(Boolean);
   return colors.length?colors:['#9b7cff','#62e8ff','#ffd66b','#63f5a4'];
  },
  burst(event){
   if(reducedMotion()||document.hidden)return;
   if(!this.app&&!this.context)return;
   const detail=event&&event.detail||{};
   const bounds=this.host.getBoundingClientRect();
   const x=Number.isFinite(detail.x)?detail.x:(window.innerWidth||bounds.width)/2;
   const y=Number.isFinite(detail.y)?detail.y:(window.innerHeight||bounds.height)/2;
   const palette=this.palette();
   const count=30;
   for(let i=0;i<count;i++){
    const angle=(Math.PI*2*i/count)+(Math.random()-.5)*.18;
    const speed=2.2+Math.random()*4.4;
    const size=3+Math.random()*4;
    const p={
     x,y,vx:Math.cos(angle)*speed,vy:Math.sin(angle)*speed-1.4,
     gravity:.11+Math.random()*.08,life:38+Math.random()*20,
     maxLife:58,size,rotation:Math.random()*Math.PI,
     spin:(Math.random()-.5)*.22,color:palette[i%palette.length],view:null
    };
    if(this.app){
     p.view=new window.PIXI.Graphics().rect(-size/2,-size/2,size,size).fill(p.color);
     p.view.position.set(x,y);
     this.app.stage.addChild(p.view);
    }
    this.particles.push(p);
   }
   if(this.particles.length>120){
    const removed=this.particles.splice(0,this.particles.length-120);
    removed.forEach(p=>{if(p.view){this.app.stage.removeChild(p.view);p.view.destroy();}});
   }
   if(this.context&&!this.frame){this.lastFrame=0;this.frame=requestAnimationFrame(now=>this.drawCanvas(now));}
  },
  drawPixi(ticker){
   if(document.hidden||!this.app)return;
   const dt=Math.min(2,Math.max(0,ticker.deltaTime||1));
   const alive=[];
   for(const p of this.particles){
    p.x+=p.vx*dt;p.y+=p.vy*dt;p.vy+=p.gravity*dt;p.vx*=Math.pow(.992,dt);
    p.rotation+=p.spin*dt;p.life-=dt;
    if(p.life<=0||p.y>(window.innerHeight||0)+24){
     if(p.view){this.app.stage.removeChild(p.view);p.view.destroy();}
     continue;
    }
    p.view.position.set(p.x,p.y);
    p.view.rotation=p.rotation;
    p.view.alpha=Math.max(0,Math.min(1,p.life/p.maxLife));
    alive.push(p);
   }
   this.particles=alive;
  },
  drawCanvas(now){
   this.frame=0;
   if(document.hidden||!this.context||!this.particles.length)return;
   const dt=this.lastFrame?Math.min(2,(now-this.lastFrame)/16.67):1;
   this.lastFrame=now;
   this.context.clearRect(0,0,this.width,this.height);
   const alive=[];
   for(const p of this.particles){
    p.x+=p.vx*dt;p.y+=p.vy*dt;p.vy+=p.gravity*dt;p.vx*=Math.pow(.992,dt);
    p.rotation+=p.spin*dt;p.life-=dt;
    if(p.life<=0||p.y>this.height+24)continue;
    alive.push(p);
    this.context.save();
    this.context.globalAlpha=Math.max(0,Math.min(1,p.life/p.maxLife));
    this.context.translate(p.x,p.y);this.context.rotate(p.rotation);
    this.context.fillStyle=p.color;
    this.context.fillRect(-p.size/2,-p.size/2,p.size,p.size);
    this.context.restore();
   }
   this.particles=alive;
   if(alive.length)this.frame=requestAnimationFrame(next=>this.drawCanvas(next));
   else this.lastFrame=0;
  },
  visibilityChanged(){
   if(!document.hidden)return;
   if(this.frame)cancelAnimationFrame(this.frame);
   this.frame=0;this.lastFrame=0;
   this.particles.forEach(p=>{if(p.view&&this.app){this.app.stage.removeChild(p.view);p.view.destroy();}});
   this.particles.length=0;
   if(this.context)this.context.clearRect(0,0,this.width,this.height);
  }
 }
};
try{createApp(component).mount(mount)}
catch(_error){window.jarrlyyParticleEffectsReady=false;return}
})();
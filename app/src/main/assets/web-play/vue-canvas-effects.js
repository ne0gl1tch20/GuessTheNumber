(()=>{'use strict';
const mount=document.getElementById('gtnVueCanvasEffects');
const VueRuntime=window.Vue;
if(!mount||!VueRuntime||typeof VueRuntime.createApp!=='function'){
 window.gtnVueCanvasEffectsReady=false;
 return;
}
const {createApp,h}=VueRuntime;
const reducedMotion=()=>!!(window.matchMedia&&window.matchMedia('(prefers-reduced-motion: reduce)').matches);
const component={
 render(){return h('canvas',{class:'gtn-vue-canvas-effects__canvas', 'aria-hidden':'true'})},
 mounted(){
  this.canvas=this.$el;
  this.particles=[];
  this.frame=0;
  this.lastFrame=0;
  this.resizeObserver=null;
  this.onResize=null;
  this.onBurst=null;
  this.onVisibility=null;
  this.context=this.canvas.getContext('2d',{alpha:true});
  if(!this.context){window.gtnVueCanvasEffectsReady=false;return;}
  this.resizeObserver=typeof ResizeObserver==='function'?new ResizeObserver(()=>this.resize()):null;
  this.onResize=()=>this.resize();
  this.onBurst=event=>this.burst(event);
  this.onVisibility=()=>this.visibilityChanged();
  if(this.resizeObserver)this.resizeObserver.observe(document.documentElement);
  else window.addEventListener('resize',this.onResize,{passive:true});
  window.addEventListener('gtn:canvas-burst',this.onBurst);
  document.addEventListener('visibilitychange',this.onVisibility);
  this.resize();
  window.gtnVueCanvasEffectsReady=true;
 },
 beforeUnmount(){
  window.gtnVueCanvasEffectsReady=false;
  window.removeEventListener('gtn:canvas-burst',this.onBurst);
  window.removeEventListener('resize',this.onResize);
  document.removeEventListener('visibilitychange',this.onVisibility);
  if(this.resizeObserver)this.resizeObserver.disconnect();
  if(this.frame)cancelAnimationFrame(this.frame);
  this.frame=0;
  this.particles.length=0;
 },
 methods:{
  resize(){
   if(!this.canvas||!this.context)return;
   const width=Math.max(1,window.innerWidth||document.documentElement.clientWidth);
   const height=Math.max(1,window.innerHeight||document.documentElement.clientHeight);
   const dpr=Math.min(2,Math.max(1,window.devicePixelRatio||1));
   this.canvas.width=Math.round(width*dpr);
   this.canvas.height=Math.round(height*dpr);
   this.canvas.style.width=width+'px';
   this.canvas.style.height=height+'px';
   this.context.setTransform(dpr,0,0,dpr,0,0);
   this.width=width;this.height=height;
  },
  burst(event){
   if(reducedMotion()||document.hidden||!this.context)return;
   const detail=event&&event.detail||{};
   const x=Number.isFinite(detail.x)?detail.x:this.width/2;
   const y=Number.isFinite(detail.y)?detail.y:this.height/2;
   const style=getComputedStyle(document.body);
   const colors=['--accent','--accent2','--gold','--good','--danger']
    .map(key=>style.getPropertyValue(key).trim()).filter(Boolean);
   const palette=colors.length?colors:['#9b7cff','#62e8ff','#ffd66b','#63f5a4'];
   const count=30;
   for(let i=0;i<count;i++){
    const angle=(Math.PI*2*i/count)+(Math.random()-.5)*.18;
    const speed=2.2+Math.random()*4.4;
    this.particles.push({
     x,y,vx:Math.cos(angle)*speed,vy:Math.sin(angle)*speed-1.4,
     gravity:.11+Math.random()*.08,life:38+Math.random()*20,
     maxLife:58,size:3+Math.random()*4,rotation:Math.random()*Math.PI,
     spin:(Math.random()-.5)*.22,color:palette[i%palette.length]
    });
   }
   if(this.particles.length>120)this.particles.splice(0,this.particles.length-120);
   if(!this.frame){this.lastFrame=0;this.frame=requestAnimationFrame(now=>this.draw(now));}
  },
  draw(now){
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
   if(alive.length)this.frame=requestAnimationFrame(next=>this.draw(next));
   else this.lastFrame=0;
  },
  visibilityChanged(){
   if(document.hidden){
    if(this.frame)cancelAnimationFrame(this.frame);
    this.frame=0;this.lastFrame=0;this.particles.length=0;
    if(this.context)this.context.clearRect(0,0,this.width,this.height);
   }
  }
 }
};
try{createApp(component).mount(mount)}
catch(_error){window.gtnVueCanvasEffectsReady=false;return}
})();

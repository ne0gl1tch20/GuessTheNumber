(()=>{'use strict';
const global=window,VueRuntime=global.Vue,sdk=global.JarrlyyUI;
const unavailable=reason=>Object.freeze({available:false,reason});
if(!VueRuntime||typeof VueRuntime.createApp!=='function'||!sdk){
 global.GTNVueJarrlyy=unavailable('dependencies-unavailable');
 return;
}
const allowedVariants=new Set(['primary','secondary','accent','danger']);
const JarrlyyButton={
 name:'JarrlyyButton',
 props:{
  variant:{type:String,default:'primary'},
  type:{type:String,default:'button'},
  disabled:{type:Boolean,default:false},
  ariaLabel:{type:String,default:''}
 },
 emits:['click'],
 setup(props,{slots,emit}){
  return ()=>VueRuntime.h('button',{
   class:['jui-button','jui-button-'+(allowedVariants.has(props.variant)?props.variant:'primary')],
   type:props.type==='submit'?'submit':'button',
   disabled:props.disabled,
   'aria-label':props.ariaLabel||undefined,
   onClick:event=>emit('click',event)
  },slots.default?slots.default():[]);
 }
};
function resolveRoot(target){
 if(typeof target==='string')return document.querySelector(target);
 return target&&typeof target.appendChild==='function'?target:null;
}
function validEvent(name){return typeof name==='string'&&/^jui:[a-z0-9:-]{1,64}$/i.test(name)}
const api={
 available:true,
 version:1,
 Button:JarrlyyButton,
 createApp(component,props){return VueRuntime.createApp(component,props||{})},
 mount(component,target,props){
  const root=resolveRoot(target);
  if(!root||!component)return null;
  const app=VueRuntime.createApp(component,props||{});
  app.component('JarrlyyButton',JarrlyyButton);
  app.mount(root);
  return app;
 },
 toast(message,duration){
  if(typeof sdk.toast!=='function')return false;
  sdk.toast(String(message==null?'':message),duration);
  return true;
 },
 dialog(title,message,options){
  if(typeof sdk.dialog!=='function')return false;
  const safe=options&&typeof options==='object'?options:{};
  sdk.dialog(String(title||'Notice'),String(message||''),{
   cancelText:String(safe.cancelText||'Close'),
   confirmText:String(safe.confirmText||''),
   onConfirm:typeof safe.onConfirm==='function'?safe.onConfirm:undefined
  });
  return true;
 },
 on(root,name,handler){
  const target=resolveRoot(root);
  if(!target||!validEvent(name)||typeof handler!=='function')return ()=>{};
  target.addEventListener(name,handler);
  return ()=>target.removeEventListener(name,handler);
 },
 emit(root,name,detail){
  const target=resolveRoot(root);
  if(!target||!validEvent(name))return false;
  target.dispatchEvent(new global.CustomEvent(name,{bubbles:true,detail}));
  return true;
 }
};
global.GTNVueJarrlyy=Object.freeze(api);
})();

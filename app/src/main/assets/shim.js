(function(){
if(!window.AndroidBridge)return;
var c={readText:function(){return Promise.resolve(AndroidBridge.paste())},writeText:function(t){AndroidBridge.copy(t);return Promise.resolve()}};
try{Object.defineProperty(navigator,'clipboard',{value:c,configurable:true})}catch(e){}
var last=null,oc=URL.createObjectURL;
URL.createObjectURL=function(b){last=b;return oc.call(URL,b)};
var ac=HTMLAnchorElement.prototype.click;
HTMLAnchorElement.prototype.click=function(){
if(this.download&&last){var n=this.download,r=new FileReader();r.onload=function(){AndroidBridge.save(n,String(r.result).split(',')[1])};r.readAsDataURL(last);return}
return ac.call(this)};
})();

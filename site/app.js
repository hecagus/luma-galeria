const button=document.getElementById('copy-hash');
const feedback=document.getElementById('feedback');
let timer;
button.addEventListener('click',async()=>{
  let message;
  try{await navigator.clipboard.writeText(document.getElementById('apk-hash').textContent.trim());message='SHA-256 copiado.';}
  catch{message='No se pudo copiar. Puedes seleccionar el valor en pantalla.';}
  clearTimeout(timer);feedback.textContent=message;feedback.hidden=false;timer=setTimeout(()=>{feedback.hidden=true;},3500);
});

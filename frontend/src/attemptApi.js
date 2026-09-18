export async function api(path,options={}) {
 let response;
 try {
  const method=options.method||'GET';
  if(!['GET','HEAD','OPTIONS'].includes(method)){
   const tokenResponse=await fetch('/api/v1/auth/csrf',{credentials:'same-origin',signal:options.signal});
   if(!tokenResponse.ok)throw new Error('No se pudo preparar el formulario. Volvé a intentar.');
   const csrf=await tokenResponse.json();options={...options,headers:{...options.headers,[csrf.headerName]:csrf.token}};
  }
  response=await fetch('/api/v1'+path,{credentials:'same-origin',...options,headers:{'Content-Type':'application/json',...options.headers}});
 } catch(error){
  if(error.name==='AbortError')throw error;
  if(error instanceof TypeError)throw new Error('No hay conexión con el servidor. Revisá la conexión y volvé a intentar.');
  throw error;
 }
 const body=await response.json().catch(()=>null);
 if(!response.ok){
  const error=new Error(body?.detail||'No se pudo completar la operación. Intentá nuevamente.');error.status=response.status;
  if(response.status===401&&!path.startsWith('/auth/')&&typeof window!=='undefined')window.dispatchEvent(new Event('session-expired'));
  throw error;
 }
 if(body==null)throw new Error('El servidor devolvió una respuesta inesperada.');
 return body;
}
export const answeredCount=attempt=>attempt.questions.filter(q=>q.selectedOption!==null).length;

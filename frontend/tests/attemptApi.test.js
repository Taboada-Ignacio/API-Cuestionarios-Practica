import test from 'node:test';
import assert from 'node:assert/strict';
import {api} from '../src/attemptApi.js';
test('obtiene CSRF para escrituras y conserva las cabeceras',async()=>{
 const original=globalThis.fetch;const calls=[];
 globalThis.fetch=async(url,options)=>{calls.push({url,options});return calls.length===1?new Response(JSON.stringify({headerName:'X-CSRF-TOKEN',token:'protected'})):new Response(JSON.stringify({id:'saved'}));};
 try{assert.deepEqual(await api('/bancos',{method:'POST',headers:{'X-Extra':'extra'},body:'{}'}),{id:'saved'});assert.equal(calls[0].url,'/api/v1/auth/csrf');assert.equal(calls[1].options.headers['X-CSRF-TOKEN'],'protected');assert.equal(calls[1].options.headers['X-Extra'],'extra');}finally{globalThis.fetch=original;}
});
test('las lecturas no solicitan CSRF y los errores conservan su estado',async()=>{
 const original=globalThis.fetch;let calls=0;globalThis.fetch=async()=>{calls++;return new Response(JSON.stringify({detail:'No disponible'}),{status:404});};
 try{await assert.rejects(api('/bancos/unknown'),error=>error.status===404&&error.message==='No disponible');assert.equal(calls,1);}finally{globalThis.fetch=original;}
});

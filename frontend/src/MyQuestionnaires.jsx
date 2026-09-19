import React,{useEffect,useState} from 'react';
import {api} from './attemptApi';
const labels={BORRADOR:'Borrador',PRIVADO:'Privado',PUBLICO:'Público'};
const descriptions={BORRADOR:'Solo vos podés verlo mientras lo preparás.',PRIVADO:'Listo para practicar de forma privada.',PUBLICO:'Disponible para que otros usuarios practiquen.'};
function EditIcon(){return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m4 20 4.2-1 10.6-10.6a2 2 0 0 0-2.8-2.8L5.4 16.2 4 20Z"/><path d="m14.5 7.1 2.8 2.8"/></svg>}
export default function MyQuestionnaires(){
 const [list,setList]=useState(null),[page,setPage]=useState(0),[error,setError]=useState(''),[busy,setBusy]=useState(null),[retry,setRetry]=useState(0);
 useEffect(()=>{const c=new AbortController();setError('');api('/bancos?page='+page+'&size=12',{signal:c.signal}).then(setList).catch(e=>{if(e.name!=='AbortError')setError(e.message);});return()=>c.abort();},[page,retry]);
 async function status(bank,next){setBusy(bank.id);setError('');try{const saved=await api('/bancos/'+bank.id+'/estado',{method:'PUT',body:JSON.stringify({status:next})});setList(l=>({...l,items:l.items.map(x=>x.id===saved.id?saved:x)}));}catch(e){setError(e.message);}finally{setBusy(null);}}
 const counts=list?.items.reduce((result,item)=>({...result,[item.status]:(result[item.status]||0)+1}),{})||{};
 return <div className="questionnaires-page">
  <div className="questionnaires-hero"><div><p className="eyebrow">Biblioteca personal</p><h1>Mis cuestionarios</h1><p>Organizá tus contenidos, retomá la edición y elegí quién puede utilizarlos.</p></div><a className="bank-primary" href="#cuestionarios/nuevo"><span aria-hidden="true">＋</span> Nuevo cuestionario</a></div>
  {error&&<div className="bank-message error" role="alert">{error}<button className="text-button" onClick={()=>setRetry(x=>x+1)}>Volver a cargar</button></div>}
  {!list?<div className="questionnaires-loading" role="status">Cargando tus cuestionarios…</div>:!list.total?<section className="questionnaires-empty"><span aria-hidden="true">?</span><h2>Tu biblioteca está lista para empezar</h2><p>Creá tu primer cuestionario y agregá preguntas manualmente o desde Excel.</p><a className="bank-primary" href="#cuestionarios/nuevo">Crear mi primer cuestionario</a></section>:<>
   <div className="questionnaire-stats" aria-label="Resumen de cuestionarios"><div><strong>{list.total}</strong><span>Total</span></div><div><strong>{counts.BORRADOR||0}</strong><span>Borradores en esta página</span></div><div><strong>{(counts.PRIVADO||0)+(counts.PUBLICO||0)}</strong><span>Listos para practicar</span></div></div>
   <section className="questionnaire-grid" aria-label="Listado de cuestionarios">{list.items.map(bank=><article className="questionnaire-card" key={bank.id}>
    <div className="questionnaire-card-top"><span className={'status-badge '+bank.status.toLowerCase()}><i />{bank.active?labels[bank.status]:'Desactivado'}</span><a className="edit-questionnaire" href={'#cuestionarios/editar/'+bank.id} aria-label={'Editar '+bank.name}><EditIcon/> Editar</a></div>
    <div className="questionnaire-symbol" aria-hidden="true">?</div><h2>{bank.name}</h2><p>{descriptions[bank.status]}</p>
    <div className="questionnaire-card-footer"><label>Visibilidad<select aria-label={'Visibilidad de '+bank.name} value={bank.status} disabled={!bank.active||!!busy} onChange={e=>status(bank,e.target.value)}>{Object.entries(labels).map(([key,label])=><option key={key} value={key}>{label}</option>)}</select></label><a href={'#cuestionarios/editar/'+bank.id}>Abrir editor →</a></div>
   </article>)}</section>
   {list.total>12&&<nav className="questionnaire-pagination" aria-label="Paginación"><button className="bank-secondary" disabled={page===0||!!busy} onClick={()=>setPage(p=>p-1)}>← Anterior</button><span>Página {page+1} de {Math.ceil(list.total/12)}</span><button className="bank-secondary" disabled={(page+1)*12>=list.total||!!busy} onClick={()=>setPage(p=>p+1)}>Siguiente →</button></nav>}
  </>}
 </div>;
}

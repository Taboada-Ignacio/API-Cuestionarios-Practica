import React,{useEffect,useState} from 'react';
import {api} from './attemptApi';
import AcademicPicker from './AcademicPicker';
const special=[{id:'NONE',name:'No posee'},{id:'MISSING',name:'No encuentro mi opción'}];
export const emptyAcademic=()=>({university:'',faculty:'',career:'',note:''});
export default function AcademicFields({value,onChange,disabled}){
 const [universities,setUniversities]=useState([]),[faculties,setFaculties]=useState([]),[careers,setCareers]=useState([]);
 const [error,setError]=useState(''),[retry,setRetry]=useState(0);
 const [loading,setLoading]=useState({university:true,faculty:false,career:false});
 useEffect(()=>{const c=new AbortController();setLoading(s=>({...s,university:true}));setError('');api('/academia/universidades',{signal:c.signal}).then(setUniversities).catch(e=>{if(e.name!=='AbortError')setError(e.message);}).finally(()=>{if(!c.signal.aborted)setLoading(s=>({...s,university:false}));});return()=>c.abort();},[retry]);
 useEffect(()=>{setFaculties([]);if(!value.university||special.some(x=>x.id===value.university))return;const c=new AbortController();setLoading(s=>({...s,faculty:true}));api('/academia/facultades?university='+encodeURIComponent(value.university),{signal:c.signal}).then(setFaculties).catch(e=>{if(e.name!=='AbortError')setError(e.message);}).finally(()=>{if(!c.signal.aborted)setLoading(s=>({...s,faculty:false}));});return()=>c.abort();},[value.university,retry]);
 useEffect(()=>{setCareers([]);if(!value.faculty||special.some(x=>x.id===value.faculty))return;const c=new AbortController();setLoading(s=>({...s,career:true}));api('/academia/carreras?faculty='+encodeURIComponent(value.faculty),{signal:c.signal}).then(setCareers).catch(e=>{if(e.name!=='AbortError')setError(e.message);}).finally(()=>{if(!c.signal.aborted)setLoading(s=>({...s,career:false}));});return()=>c.abort();},[value.faculty,retry]);
 function change(key,id){const next={...value,[key]:id};const isSpecial=special.some(x=>x.id===id);if(key==='university'){next.faculty=isSpecial?id:'';next.career=isSpecial?id:'';}if(key==='faculty')next.career=isSpecial?id:'';if(![next.university,next.faculty,next.career].includes('MISSING'))next.note='';onChange(next);}
 function field(key,label,entries,locked){return <AcademicPicker label={label} value={value[key]} entries={entries} disabled={disabled||locked} loading={loading[key]} onSelect={id=>change(key,id)} />;}
 return <fieldset className="academic-fields" disabled={disabled}><legend>Tu formación académica</legend><p className="field-help">Los tres datos son obligatorios. Elegí «No posee» si no corresponde. Catálogo consultado en la Guía oficial SIU; puede haber opciones faltantes.</p>{error&&<div role="alert" className="bank-message error">{error}<button type="button" className="text-button" onClick={()=>setRetry(x=>x+1)}>Reintentar</button></div>}
 {field('university','Universidad',universities,false)}{field('faculty','Facultad o unidad académica',faculties,!value.university||special.some(x=>x.id===value.university))}{field('career','Carrera universitaria',careers,!value.faculty||special.some(x=>x.id===value.faculty))}
 {Object.values(value).includes('MISSING')&&<label className="field">Indicá la institución, facultad o carrera que falta<textarea required maxLength={1000} value={value.note||''} onChange={e=>onChange({...value,note:e.target.value})} /></label>}</fieldset>;
}

import React,{useEffect,useState} from 'react';
import {api} from './attemptApi';
import AcademicPicker from './AcademicPicker';
import {addUniqueTag,normalizeAcademicName} from './classificationRules';

const evaluationTypes=[['PARCIAL','Parcial'],['TRABAJO_PRACTICO','Trabajo práctico'],['FINAL','Final'],['RECUPERATORIO','Recuperatorio'],['PRACTICA','Práctica'],['OTRO','Otro']];

export default function QuestionnaireClassification({value,onChange,disabled}){
 const [universities,setUniversities]=useState([]),[faculties,setFaculties]=useState([]),[careers,setCareers]=useState([]);
 const [suggestions,setSuggestions]=useState([]),[search,setSearch]=useState(''),[loadError,setLoadError]=useState(''),[loading,setLoading]=useState({university:true,faculty:false,career:false});
 useEffect(()=>{const controller=new AbortController();api('/academia/universidades',{signal:controller.signal}).then(setUniversities).catch(error=>{if(error.name!=='AbortError')setLoadError('No se pudo cargar el catálogo académico.');}).finally(()=>{if(!controller.signal.aborted)setLoading(state=>({...state,university:false}));});return()=>controller.abort();},[]);
 useEffect(()=>{setFaculties([]);if(!value.academicUniversityId)return;const controller=new AbortController();setLoading(state=>({...state,faculty:true}));api('/academia/facultades?university='+encodeURIComponent(value.academicUniversityId),{signal:controller.signal}).then(setFaculties).catch(error=>{if(error.name!=='AbortError')setLoadError('No se pudieron cargar las facultades.');}).finally(()=>{if(!controller.signal.aborted)setLoading(state=>({...state,faculty:false}));});return()=>controller.abort();},[value.academicUniversityId]);
 useEffect(()=>{setCareers([]);if(!value.academicFacultyId)return;const controller=new AbortController();setLoading(state=>({...state,career:true}));api('/academia/carreras?faculty='+encodeURIComponent(value.academicFacultyId),{signal:controller.signal}).then(setCareers).catch(error=>{if(error.name!=='AbortError')setLoadError('No se pudieron cargar las carreras.');}).finally(()=>{if(!controller.signal.aborted)setLoading(state=>({...state,career:false}));});return()=>controller.abort();},[value.academicFacultyId]);
 useEffect(()=>{const controller=new AbortController();const timer=setTimeout(()=>api('/etiquetas?search='+encodeURIComponent(search),{signal:controller.signal}).then(setSuggestions).catch(error=>{if(error.name!=='AbortError')setSuggestions([]);}),250);return()=>{clearTimeout(timer);controller.abort();};},[search]);
 function change(patch){onChange({...value,...patch});}
 function chooseUniversity(id){change({academicUniversityId:id,academicFacultyId:'',academicCareerId:'',studyYear:''});}
 function chooseFaculty(id){change({academicFacultyId:id,academicCareerId:'',studyYear:''});}
 function chooseCareer(id){change({academicCareerId:id,studyYear:''});}
 function addTag(name){if(value.tags.length>=30)return;change({tags:addUniqueTag(value.tags,name)});setSearch('');}
 function keyDown(event){if((event.key==='Enter'||event.key===',')&&search.trim()){event.preventDefault();addTag(search);}}
 const selectedCareer=careers.find(career=>career.id===value.academicCareerId);
 const visible=suggestions.filter(tag=>!value.tags.some(selected=>selected.toLowerCase()===tag.name.toLowerCase()));
 return <section className="bank-box academic-classification" aria-labelledby="academic-classification-title">
  <div className="box-title"><div><h2 id="academic-classification-title">Información académica</h2><p>Opcional · usa el mismo catálogo disponible al registrar usuarios</p></div><span className="optional-badge">Opcional</span></div>
  {loadError&&<p className="classification-warning" role="status">{loadError}</p>}
  <div className="classification-grid academic-catalog-grid">
   <AcademicPicker label="Universidad" value={value.academicUniversityId} entries={universities} onSelect={chooseUniversity} disabled={disabled} loading={loading.university} specialOptions={false} required={false} />
   <AcademicPicker label="Facultad o unidad académica" value={value.academicFacultyId} entries={faculties} onSelect={chooseFaculty} disabled={disabled||!value.academicUniversityId} loading={loading.faculty} specialOptions={false} required={false} />
   <AcademicPicker label="Carrera" value={value.academicCareerId} entries={careers} onSelect={chooseCareer} disabled={disabled||!value.academicFacultyId} loading={loading.career} specialOptions={false} required={false} />
   <label className="field">Año de carrera<select value={value.studyYear} disabled={disabled||!selectedCareer?.durationYears} onChange={event=>change({studyYear:event.target.value})}><option value="">Sin especificar</option>{Array.from({length:selectedCareer?.durationYears||0},(_,index)=><option value={index+1} key={index+1}>{index+1}º año</option>)}</select><small>{value.academicCareerId&&!selectedCareer?.durationYears?'La fuente oficial no informa una duración para esta carrera.':selectedCareer?.durationYears?'Duración publicada: '+selectedCareer.durationYears+' años.':'Primero seleccioná una carrera.'}</small></label>
   <label className="field">Materia<input value={value.academicSubjectName} disabled={disabled} maxLength={200} onChange={event=>change({academicSubjectName:event.target.value,subjectId:''})} onBlur={()=>value.academicSubjectName&&change({academicSubjectName:normalizeAcademicName(value.academicSubjectName),subjectId:''})} placeholder="Ej.: Ingeniería y Sociedad" /><small>Podés escribirla manualmente. El nombre se guardará con mayúsculas y minúsculas normalizadas.</small></label>
   <label className="field">Tipo de evaluación<select value={value.evaluationType} disabled={disabled} onChange={event=>change({evaluationType:event.target.value,evaluationNumber:event.target.value?value.evaluationNumber:''})}><option value="">Sin especificar</option>{evaluationTypes.map(([key,label])=><option value={key} key={key}>{label}</option>)}</select></label>
   <label className="field">Número<input type="number" min="1" step="1" inputMode="numeric" value={value.evaluationNumber} disabled={disabled||!value.evaluationType} onChange={event=>change({evaluationNumber:event.target.value})} placeholder="Ej.: 1" /></label>
  </div>
  <div className="tag-field"><label htmlFor="questionnaire-tags">Etiquetas <span>Temas o conceptos</span></label>
   {!!value.tags.length&&<div className="tag-chips" aria-label="Etiquetas seleccionadas">{value.tags.map(tag=><span key={tag}>{tag}<button type="button" disabled={disabled} onClick={()=>change({tags:value.tags.filter(item=>item!==tag)})} aria-label={'Quitar etiqueta '+tag}>×</button></span>)}</div>}
   <input id="questionnaire-tags" value={search} disabled={disabled||value.tags.length>=30} maxLength={100} onChange={event=>setSearch(event.target.value)} onKeyDown={keyDown} placeholder="Buscar o agregar etiqueta…" autoComplete="off" aria-describedby="tag-help" />
   {search&&<div className="tag-suggestions" role="listbox">{visible.slice(0,6).map(tag=><button type="button" role="option" key={tag.id} onClick={()=>addTag(tag.name)}>{tag.name}</button>)}{!visible.some(tag=>tag.name.toLowerCase()===search.trim().toLowerCase())&&<button type="button" className="create-tag" onClick={()=>addTag(search)}>＋ Crear “{search.trim()}”</button>}</div>}
   <small id="tag-help">Presioná Enter o coma para agregar. Máximo 30 etiquetas.</small>
  </div>
 </section>;
}

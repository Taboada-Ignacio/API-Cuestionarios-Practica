import React,{useEffect,useId,useRef,useState} from 'react';
const special=[{id:'NONE',name:'No posee'},{id:'MISSING',name:'No encuentro mi opción'}];
const normalize=s=>s.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase('es');
export default function AcademicPicker({label,value,entries,onSelect,disabled,loading}){
 const id=useId(),root=useRef(null),[query,setQuery]=useState(''),[open,setOpen]=useState(false),[active,setActive]=useState(-1);
 const selected=[...special,...entries].find(e=>e.id===value);
 const selectedName=selected?.name||(value?'Selección guardada':'');
 const matches=entries.filter(e=>normalize(e.name).includes(normalize(query)));
 const options=[...matches.slice(0,60),...special];
 useEffect(()=>{if(value){setQuery('');setOpen(false);setActive(-1);}},[value]);
 function choose(option){onSelect(option.id);setOpen(false);setQuery('');setActive(-1);}
 function keyDown(e){
  if(e.key==='ArrowDown'||e.key==='ArrowUp'){e.preventDefault();setOpen(true);setActive(i=>e.key==='ArrowDown'?Math.min(i+1,options.length-1):Math.max(i-1,0));}
  if(e.key==='Escape'){setOpen(false);setActive(-1);}
  if(e.key==='Enter'&&open){e.preventDefault();if(active>=0&&options[active])choose(options[active]);}
 }
 useEffect(()=>{if(active>=0&&open)root.current?.querySelector('[data-active="true"]')?.scrollIntoView({block:'nearest'});},[active,open]);
 return <div className="academic-field academic-picker" ref={root} onBlur={e=>{if(!e.currentTarget.contains(e.relatedTarget)){setOpen(false);setActive(-1);}}}>
  <label className="field" htmlFor={id}>{label}</label>
  <input id={id} type="text" role="combobox" aria-autocomplete="list" aria-expanded={open&&!disabled} aria-controls={id+'-list'} aria-activedescendant={open&&active>=0?id+'-option-'+active:undefined} aria-describedby={id+'-help'} autoComplete="off" required={!disabled} disabled={disabled} value={open?query:selectedName||query} placeholder={loading?'Cargando opciones…':'Escribí el nombre para buscar'}
   onFocus={()=>{setQuery(value?selectedName:query);setOpen(true);setActive(-1);}}
   onChange={e=>{setQuery(e.target.value);setOpen(true);setActive(-1);if(value)onSelect('');}} onKeyDown={keyDown} />
  {open&&!disabled&&<div className="academic-results"><ul id={id+'-list'} role="listbox" aria-label={'Coincidencias de '+label.toLowerCase()}>
   {loading?<li role="status" className="picker-notice">Cargando opciones…</li>:<>{options.map((option,i)=><li key={option.id} role="presentation"><button type="button" role="option" id={id+'-option-'+i} aria-selected={value===option.id} data-active={active===i} tabIndex={-1} onMouseDown={e=>e.preventDefault()} onClick={()=>choose(option)}>{option.name}<span aria-hidden="true">{value===option.id?'✓':'→'}</span></button></li>)}</>}
  </ul>{!loading&&<p className="picker-notice" role="status">{matches.length?matches.length>60?'Mostrando 60 de '+matches.length+' coincidencias. Escribí más para filtrar.':matches.length+' coincidencias.':'No hay coincidencias. Podés elegir «No encuentro mi opción».'}</p>}</div>}
  <small id={id+'-help'} className="picker-help">{disabled?'Primero seleccioná la opción anterior, si corresponde.':value?'Selección confirmada. Podés escribir para cambiarla.':'Seleccioná un resultado o elegí «No posee».'}</small>
 </div>;
}

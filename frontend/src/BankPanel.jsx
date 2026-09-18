import React, { useState, useRef } from 'react';
import { LETTERS, validateQuestion, parseBankExcel } from './bankExcel';
import './bank.css';
import {api} from './attemptApi';

const emptyQuestion=()=>({statement:'',explanation:'',options:[{text:'',correct:false},{text:'',correct:false}]});

export default function BankPanel() {
  const [name,setName]=useState('');
  const [status,setStatus]=useState('BORRADOR');
  const [mode,setMode]=useState('manual');
  const [draft,setDraft]=useState(emptyQuestion);
  const [questions,setQuestions]=useState([]);
  const [editing,setEditing]=useState(null);
  const [error,setError]=useState('');
  const [notice,setNotice]=useState('');
  const [busy,setBusy]=useState(false);
  const [saved,setSaved]=useState(null);
  const upload=useRef(null);
  const locked=busy || !!saved;

  function addQuestion(event) {
    event.preventDefault(); setError(''); setNotice('');
    const problem=validateQuestion(draft);
    if(problem) { setError(problem); return; }
    if(editing===null && questions.length>=1000) {setError('El cuestionario admite hasta 1.000 preguntas por carga.');return;}
    const cleaned={statement:draft.statement.trim(),explanation:draft.explanation.trim(),
      options:draft.options.map(o=>({...o,text:o.text.trim()}))};
    setQuestions(current=>editing===null?[...current,cleaned]:current.map((q,i)=>i===editing?cleaned:q));
    setNotice(editing===null?'Pregunta agregada a la revisión.':'Pregunta actualizada.');
    setDraft(emptyQuestion()); setEditing(null);
  }
  function editQuestion(index) {
    setDraft(structuredClone(questions[index])); setEditing(index); setMode('manual');setError('');setNotice('');
    requestAnimationFrame(()=>document.getElementById('enunciado')?.focus());
  }
  function removeQuestion(index) {
    setQuestions(q=>q.filter((_,i)=>i!==index));
    if(editing===index) {setDraft(emptyQuestion());setEditing(null);}
    else if(editing!==null && editing>index) setEditing(editing-1);
  }
  async function importFile(file) {
    if(!file || locked) return;
    setError('');setNotice('');setBusy(true);
    try {
      const imported=await parseBankExcel(file);
      if(questions.length+imported.length>1000) throw new Error('La carga completa no puede superar las 1.000 preguntas.');
      setQuestions(current=>[...current,...imported]);
      setNotice(imported.length+(imported.length===1?' pregunta importada. Revisala antes de guardar.':' preguntas importadas. Revisalas antes de guardar.'));
    } catch(e) {setError(e.message);}
    finally {setBusy(false);if(upload.current) upload.current.value='';}
  }
  async function saveBank() {
    setError('');setNotice('');
    if(!name.trim() || name.trim().length>200) {setError('Ingresá un nombre de cuestionario de hasta 200 caracteres.');document.getElementById('bank-name')?.focus();return;}
    if(!questions.length) {setError('Agregá al menos una pregunta antes de guardar.');return;}
    if(draft.statement.trim() || draft.explanation.trim() || draft.options.some(o=>o.text.trim())) {
      setError('Hay una pregunta en edición. Agregala a la revisión o descartala antes de guardar.');return;
    }
    setBusy(true);
    try {
      const body=await api('/bancos/carga',{method:'POST',body:JSON.stringify({name:name.trim(),questions,status})});
      setSaved(body);setNotice('Cuestionario guardado con '+questions.length+' preguntas.');
    } catch(e) {setError(e instanceof TypeError?'No hay conexión con el backend. Las preguntas siguen en este panel; verificá la conexión antes de reintentar.':e.message);}
    finally {setBusy(false);}
  }
  function reset() {
    setName('');setStatus('BORRADOR');setQuestions([]);setDraft(emptyQuestion());setEditing(null);setSaved(null);setError('');setNotice('');setMode('manual');
  }
  function optionText(index,text) {
    setDraft(q=>({...q,options:q.options.map((o,i)=>i===index?{...o,text}:o)}));
  }

  return <div className="bank-panel">
    <a href="#bienvenida" className="back-link">← Volver a bienvenida</a>
    <div className="bank-heading"><div><p className="eyebrow">Cuestionarios</p><h1>Cargá lo que querés practicar.</h1><p>Creá un cuestionario y sumá tus preguntas, una a una o desde un Excel.</p></div><span className="bank-heading-mark" aria-hidden="true">?</span></div>
    <div className="bank-layout">
      <div className="bank-workspace">
        <section className="bank-box" aria-labelledby="bank-data-title">
          <h2 id="bank-data-title">Datos del cuestionario</h2>
          <label className="field">Nombre del cuestionario <span>Obligatorio</span>
            <input id="bank-name" value={name} onChange={e=>setName(e.target.value)} maxLength={200} disabled={locked} placeholder="Por ejemplo: Java · Conceptos básicos" />
          </label>
          <p className="field-help">Elegí un nombre que te ayude a reconocer el tema.</p>
          <label className="field">Estado del cuestionario<select aria-label="Estado del cuestionario" value={status} disabled={locked} onChange={e=>setStatus(e.target.value)}><option value="BORRADOR">Borrador · preparar preguntas</option><option value="PRIVADO">Privado · solo para mí</option><option value="PUBLICO">Público · otros usuarios pueden practicar</option></select></label>
          <p className="field-help">Podés cambiar el estado después desde Mis cuestionarios.</p>
        </section>
        <section className="bank-box" aria-labelledby="load-title">
          <div className="box-title"><h2 id="load-title">Agregar preguntas</h2><span className="field-help">Una única respuesta correcta</span></div>
          <div className="mode-switch" aria-label="Método de carga">
            <button type="button" aria-pressed={mode==='manual'} className={mode==='manual'?'active':''} onClick={()=>setMode('manual')} disabled={locked}>Carga manual</button>
            <button type="button" aria-pressed={mode==='excel'} className={mode==='excel'?'active':''} onClick={()=>setMode('excel')} disabled={locked}>Importar Excel</button>
          </div>
          {mode==='manual'?<form onSubmit={addQuestion}>
            <fieldset disabled={locked}>
              <label className="field">Enunciado
                <textarea id="enunciado" value={draft.statement} onChange={e=>setDraft(q=>({...q,statement:e.target.value}))} maxLength={20000} rows={3} placeholder="Escribí la pregunta que querés hacer." required />
              </label>
              <div className="options-heading"><h3>Opciones de respuesta</h3><span>Marcá la correcta</span></div>
              <div className="manual-options">{draft.options.map((option,index)=><div className={'manual-option '+(option.correct?'is-correct':'')} key={index}>
                <label className="correct-choice"><input type="radio" name="correct-option" aria-label={'Marcar opción '+LETTERS[index]+' como correcta'} checked={option.correct} onChange={()=>setDraft(q=>({...q,options:q.options.map((o,i)=>({...o,correct:i===index}))}))} /><span>{LETTERS[index]}</span></label>
                <input aria-label={'Texto de opción '+LETTERS[index]} value={option.text} onChange={e=>optionText(index,e.target.value)} placeholder={'Opción '+LETTERS[index]} maxLength={1000} required />
                <button type="button" className="remove-option" aria-label={'Quitar opción '+LETTERS[index]} disabled={draft.options.length<=2} onClick={()=>setDraft(q=>({...q,options:q.options.filter((_,i)=>i!==index)}))}>×</button>
              </div>)}</div>
              <button className="text-button" type="button" disabled={draft.options.length>=10} onClick={()=>setDraft(q=>({...q,options:[...q.options,{text:'',correct:false}]}))}>+ Agregar opción</button>
              <label className="field explanation-field">Explicación <span>Opcional · se muestra al finalizar el intento</span>
                <textarea value={draft.explanation} onChange={e=>setDraft(q=>({...q,explanation:e.target.value}))} maxLength={20000} rows={2} placeholder="Explicá por qué la respuesta es correcta." />
              </label>
              <div className="form-actions"><button type="submit" className="bank-primary">{editing===null?'+ Agregar a la revisión':'Guardar cambios en la pregunta'}</button>
                <button type="button" className="text-button" onClick={()=>{setDraft(emptyQuestion());setEditing(null);setError('');}}>Descartar edición</button></div>
            </fieldset>
          </form>:<div className="excel-panel">
            <div className="template-bar"><div><h3>Empezá con el modelo</h3><p>Incluye una hoja para completar y un ejemplo separado.</p></div><a href="/modelo-banco-preguntas.xlsx" download="modelo-cuestionario.xlsx" className="bank-secondary">↓ Descargar modelo Excel</a></div>
            <div className="upload-area">
              <span className="upload-symbol" aria-hidden="true">↑</span><h3>Importá tus preguntas</h3><p>Archivo .xlsx · Hasta 5 MB y 1.000 preguntas</p>
              <input type="file" accept=".xlsx" id="excel-file" ref={upload} onChange={e=>importFile(e.target.files?.[0])} disabled={locked} className="file-control" />
              <label htmlFor="excel-file" className={'bank-primary file-label '+(locked?'disabled':'')}>{busy?'Leyendo archivo…':'Seleccionar Excel'}</label>
            </div>
            <div className="excel-instructions"><h3>Cómo completar el archivo</h3><ul><li>Usá la hoja <strong>Preguntas</strong>, desde la fila 2.</li><li>Completá el enunciado y entre 2 y 10 opciones consecutivas, de A a J.</li><li>En <strong>correcta</strong>, escribí la letra de una sola opción.</li><li>La explicación es opcional. Conservá los encabezados y usá texto sin fórmulas.</li></ul><p>Las preguntas se suman a tu revisión. Si alguna fila tiene errores, el archivo no se incorpora.</p></div>
          </div>}
        </section>
        {error && <div role="alert" className="bank-message error"><strong>Revisá la carga</strong><p>{error}</p></div>}
        {notice && <div role="status" className="bank-message success">{notice}</div>}
        <section className="bank-box review-box" aria-labelledby="review-title">
          <div className="box-title"><h2 id="review-title">Revisión de preguntas</h2><span className="count-pill">{questions.length}</span></div>
          {!questions.length?<div className="review-empty"><span aria-hidden="true">☰</span><p>Todavía no agregaste preguntas.</p><small>Las preguntas manuales e importadas aparecerán acá.</small></div>:<ol className="review-list">{questions.map((q,index)=><li key={index}>
            <div className="review-item-heading"><span className="question-number">{index+1}</span><h3>{q.statement}</h3></div>
            <details><summary>Ver opciones y explicación</summary><ul>{q.options.map((o,i)=><li key={i} className={o.correct?'review-correct':''}>{LETTERS[i]}. {o.text}{o.correct?' · Correcta':''}</li>)}</ul>{q.explanation&&<p>{q.explanation}</p>}</details>
            <div className="review-actions"><button type="button" onClick={()=>editQuestion(index)} disabled={locked}>Editar</button><button type="button" onClick={()=>removeQuestion(index)} disabled={locked}>Quitar</button></div>
          </li>)}</ol>}
        </section>
      </div>
      <aside className="bank-summary">
        <p className="eyebrow">Tu nuevo cuestionario</p><h2>{name.trim() || 'Un tema, muchas preguntas.'}</h2>
        <div className="summary-count"><strong>{questions.length}</strong><span>{questions.length===1?'pregunta lista':'preguntas listas'}</span></div>
        <p>Podés combinar la carga manual con preguntas importadas desde Excel.</p>
        {saved?<><p className="saved-label">✓ Cuestionario guardado</p><button className="bank-primary" onClick={reset}>Crear otro cuestionario</button></>:<button className="bank-primary" disabled={busy} onClick={saveBank}>{busy?'Procesando…':'Guardar cuestionario'}</button>}
        <small>{saved?'Tus preguntas ya forman parte del cuestionario.':'Las preguntas se guardan juntas al confirmar. Los cambios sin guardar se pierden al salir o recargar.'}</small>
      </aside>
    </div>
  </div>;
}

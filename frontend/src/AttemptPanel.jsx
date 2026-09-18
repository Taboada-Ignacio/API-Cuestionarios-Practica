import React, {useState,useEffect,useRef} from 'react';
import {api,answeredCount} from './attemptApi';
import './attempts.css';
const letter=index=>String.fromCharCode(65+index);

export default function AttemptPanel({user}) {
  const KEY='cuestionarios.currentAttempt.'+user.id;
  function remember(id){try{id?localStorage.setItem(KEY,id):localStorage.removeItem(KEY);}catch{}}
  function stored(){try{return localStorage.getItem(KEY);}catch{return null;}}

  const [stage,setStage]=useState('select');
  const [page,setPage]=useState(0);
  const [list,setList]=useState(null);
  const [selected,setSelected]=useState(null);
  const [availability,setAvailability]=useState(null);
  const [participant,setParticipant]=useState([user.name,user.lastName].filter(Boolean).join(' '));
  const [count,setCount]=useState(10);
  const [attempt,setAttempt]=useState(null);
  const [index,setIndex]=useState(0);
  const [error,setError]=useState('');
  const [busy,setBusy]=useState(false);
  const [loading,setLoading]=useState(true);
  const [finishConfirm,setFinishConfirm]=useState(false);
  const [resumeId,setResumeId]=useState(stored);
  const [reload,setReload]=useState(0);
  const inFlight=useRef(false);
  const heading=useRef(null);

  useEffect(()=>{
    const controller=new AbortController();
    setLoading(true);setError('');
    if(resumeId) {
      api('/intentos/'+resumeId,{signal:controller.signal})
        .then(data=>{setAttempt(data);setStage(data.status==='FINALIZADO'?'results':'answer');setIndex(0);})
        .catch(e=>{if(e.name==='AbortError')return;if(e.status===404){remember(null);setResumeId(null);}else setError(e.message);})
        .finally(()=>{if(!controller.signal.aborted)setLoading(false);});
    } else {
      api('/bancos?scope=practice&page='+page+'&size=12',{signal:controller.signal})
        .then(setList).catch(e=>{if(e.name!=='AbortError')setError(e.message);})
        .finally(()=>{if(!controller.signal.aborted)setLoading(false);});
    }
    return ()=>controller.abort();
  },[page,resumeId,reload]);

  useEffect(()=>{
    if(!selected)return;
    const controller=new AbortController();
    setAvailability(null);setError('');
    api('/bancos/'+selected.id+'/disponibilidad',{signal:controller.signal})
      .then(data=>{setAvailability(data);setCount(Math.min(10,data.questionCount));})
      .catch(e=>{if(e.name!=='AbortError')setError(e.message);});
    return ()=>controller.abort();
  },[selected,reload]);

  useEffect(()=>{heading.current?.focus();window.scrollTo(0,0);},[stage]);
  useEffect(()=>{
    if(!attempt || attempt.status==='FINALIZADO')return;
    const warn=e=>{e.preventDefault();e.returnValue='';};
    window.addEventListener('beforeunload',warn);
    return ()=>window.removeEventListener('beforeunload',warn);
  },[attempt]);

  async function run(operation) {
    if(inFlight.current)return;
    inFlight.current=true;setBusy(true);setError('');
    try {await operation();}catch(e){setError(e.message);}
    finally {inFlight.current=false;setBusy(false);}
  }
  function prepare(e) {
    e.preventDefault();setError('');
    if(!availability?.active || availability.questionCount<1) {setError('El cuestionario no tiene preguntas disponibles. Elegí otro.');return;}
    if(!Number.isInteger(Number(count)) || count<1 || count>Math.min(1000,availability.questionCount)) {
      setError('Elegí entre 1 y '+Math.min(1000,availability.questionCount)+' preguntas.');return;
    }
    setStage('confirm');
  }
  function start() {
    run(async()=>{
      const data=await api('/bancos/'+selected.id+'/intentos',{method:'POST',
        body:JSON.stringify({questionCount:Number(count)})});
      remember(data.id);setAttempt(data);setIndex(0);setStage('answer');
    });
  }
  function answer(optionIndex) {
    const question=attempt.questions[index];
    if(question.selectedOption===optionIndex)return;
    run(async()=>{
      const data=await api('/intentos/'+attempt.id+'/respuestas/'+question.id,
        {method:'PUT',body:JSON.stringify({optionIndex})});
      setAttempt(data);
    });
  }
  function finish() {
    run(async()=>{
      const data=await api('/intentos/'+attempt.id+'/finalizar',{method:'POST'});
      setAttempt(data);setFinishConfirm(false);setStage('results');
    });
  }
  function reset() {
    remember(null);setResumeId(null);setAttempt(null);setStage('select');setSelected(null);
    setAvailability(null);setParticipant([user.name,user.lastName].filter(Boolean).join(' '));setPage(0);setError('');setFinishConfirm(false);setReload(n=>n+1);
  }
  const title=stage==='select'?'Elegí tu próximo desafío.':stage==='confirm'?'Todo listo para practicar.':stage==='results'?'Cada respuesta te deja algo.':'Una pregunta a la vez.';
  const question=attempt?.questions[index];
  const answered=attempt?answeredCount(attempt):0;
  return <div className="attempt-panel">
    <div className="bank-heading"><div><p className="eyebrow">Intentos · {stage==='results'?'Resultados':stage==='answer'?'En curso':'Nueva práctica'}</p>
      <h1 ref={heading} tabIndex={-1}>{title}</h1>
      <p>{stage==='select'?'Seleccioná un cuestionario y prepará una sesión a tu medida.':stage==='confirm'?'Revisá los datos antes de confirmar el inicio.':stage==='answer'?'Podés revisar tus respuestas hasta finalizar.': 'Revisá lo aprendido y volvé a intentarlo cuando quieras.'}</p>
    </div></div>
    {error&&<div className="bank-message error" role="alert"><p>{error}</p>
      {(stage==='select'||(resumeId&&!attempt))&&<button type="button" className="text-button" onClick={()=>setReload(n=>n+1)}>Volver a cargar</button>}</div>}
    {loading?<div className="bank-box" role="status">Cargando {resumeId?'tu intento':'cuestionarios'}…</div>:<>
    {stage==='select'&&!resumeId&&<div className="attempt-setup">
      <section className="bank-box">
        <div className="box-title"><h2>Cuestionarios cargados</h2>{list&&<span className="count-pill">{list.total}</span>}</div>
        {list?.items.length===0?<div className="review-empty"><p>Todavía no hay cuestionarios cargados.</p><a className="bank-primary" href="#cuestionarios/nuevo">Cargar un cuestionario</a></div>:
        <div className="questionnaire-list">{list?.items.map(bank=><label className={'questionnaire-choice '+(selected?.id===bank.id?'chosen':'')+(!bank.active?' inactive':'')} key={bank.id}>
          <input type="radio" name="questionnaire" checked={selected?.id===bank.id} disabled={!bank.active||busy} onChange={()=>setSelected(bank)} />
          <span><strong>{bank.name}</strong><small>{bank.active?(bank.status==='PUBLICO'?'Público · disponible para practicar':'Privado · tu cuestionario'):'Desactivado'}</small></span><span aria-hidden="true">→</span>
        </label>)}</div>}
        {list&&list.total>12&&<div className="list-pagination"><button className="bank-secondary" disabled={page===0||busy} onClick={()=>setPage(p=>p-1)}>Anterior</button><span>Página {page+1} de {Math.ceil(list.total/12)}</span><button className="bank-secondary" disabled={(page+1)*12>=list.total||busy} onClick={()=>setPage(p=>p+1)}>Siguiente</button></div>}
      </section>
      <section className="bank-box attempt-config"><p className="eyebrow">Antes de empezar</p><h2>Prepará tu intento</h2>
        {!selected?<p className="config-empty">Elegí un cuestionario de la lista para definir tu práctica.</p>:<form onSubmit={prepare}>
          <div className="selected-questionnaire"><strong>{selected.name}</strong><span>{availability?availability.questionCount+' preguntas disponibles':'Consultando disponibilidad…'}</span></div>
          <label className="field">Participante<input value={participant} readOnly /><span>Se utiliza el nombre de tu cuenta.</span></label>
          <label className="field count-field">Cantidad de preguntas<input type="number" min={1} max={Math.min(1000,availability?.questionCount||1000)} value={count} onChange={e=>setCount(e.target.value)} required /></label>
          <p className="field-help">Se seleccionan al azar, sin repetir preguntas dentro del intento.</p>
          <button className="bank-primary" type="submit" disabled={!availability?.active||availability.questionCount<1||busy}>Revisar y continuar →</button>
        </form>}
      </section>
    </div>}
    {stage==='confirm'&&<section className="bank-box confirmation-box">
      <p className="eyebrow">Confirmación del intento</p><h2>{selected.name}</h2>
      <dl className="confirm-data"><div><dt>Participante</dt><dd>{participant.trim()||'Sin nombre'}</dd></div><div><dt>Preguntas</dt><dd>{count}</dd></div><div><dt>Selección</dt><dd>Aleatoria, sin repetición</dd></div></dl>
      <ul className="attempt-rules"><li>Una única respuesta correcta por pregunta.</li><li>Un punto por acierto. Los errores y las omisiones valen cero.</li><li>La corrección aparece al finalizar. Después, las respuestas quedan bloqueadas.</li></ul>
      <div className="form-actions"><button className="bank-primary" disabled={busy} onClick={start}>{busy?'Iniciando…':'Confirmar y comenzar intento'}</button><button className="bank-secondary" disabled={busy} onClick={()=>setStage('select')}>Modificar datos</button></div>
    </section>}
    {stage==='answer'&&attempt&&<div className="answer-layout">
      <section className="bank-box answer-card">
        <div className="box-title"><span className="eyebrow">Pregunta {index+1} de {attempt.total}</span><span className="count-pill">{question.selectedOption===null?'Sin responder':'Respuesta guardada'}</span></div>
        <h2>{question.statement}</h2>
        <fieldset disabled={busy}><legend className="sr-only">Elegí una respuesta</legend>
          {question.options.map(option=><label className={'answer-option '+(question.selectedOption===option.index?'picked':'')} key={option.index}>
            <input type="radio" name={'answer-'+question.id} checked={question.selectedOption===option.index} onChange={()=>answer(option.index)} />
            <span className="answer-letter">{letter(option.index)}</span><span>{option.text}</span>
          </label>)}
        </fieldset>
        <p className="answer-save-status" role="status">{busy?'Guardando…':'Las respuestas seleccionadas se guardan automáticamente.'}</p>
        <div className="question-navigation"><button className="bank-secondary" disabled={index===0||busy} onClick={()=>setIndex(i=>i-1)}>← Anterior</button><span>{index+1} / {attempt.total}</span><button className="bank-primary" disabled={index===attempt.total-1||busy} onClick={()=>setIndex(i=>i+1)}>Siguiente →</button></div>
      </section>
      <aside className="bank-summary attempt-progress"><p className="eyebrow">Tu práctica</p><h2>{attempt.quizName}</h2>{attempt.participantName&&<p>{attempt.participantName}</p>}
        <div className="summary-count"><strong>{answered}/{attempt.total}</strong><span>respondidas</span></div><progress value={answered} max={attempt.total} aria-label="Preguntas respondidas" />
        <div className="question-map" aria-label="Ir a una pregunta">{attempt.questions.map((q,i)=><button key={q.id} aria-label={'Ir a pregunta '+(i+1)+(q.selectedOption!==null?', respondida':', sin responder')} aria-current={index===i?'step':undefined} className={(q.selectedOption!==null?'done ':'')+(index===i?'current':'')} disabled={busy} onClick={()=>setIndex(i)}>{i+1}</button>)}</div>
        {!finishConfirm?<button className="bank-primary" disabled={busy} onClick={()=>setFinishConfirm(true)}>Finalizar intento</button>:<div className="finish-confirm" role="region" aria-label="Confirmar finalización"><p>{attempt.total-answered? (attempt.total-answered===1?'Queda 1 pregunta sin responder. Contará como incorrecta.':'Quedan '+(attempt.total-answered)+' preguntas sin responder. Contarán como incorrectas.'):'Respondidas todas las preguntas.'}</p><p>Al finalizar ya no podrás cambiar las respuestas.</p><button className="bank-primary" disabled={busy} onClick={finish}>{busy?'Finalizando…':'Confirmar finalización'}</button><button className="text-button" disabled={busy} onClick={()=>setFinishConfirm(false)}>Seguir revisando</button></div>}
      </aside>
    </div>}
    {stage==='results'&&attempt&&<div className="results-panel">
      <section className="result-overview"><div><p className="eyebrow">Intento finalizado</p><h2>{attempt.quizName}</h2>{attempt.participantName&&<p className="result-participant">{attempt.participantName}</p>}<p className="field-help">{new Date(attempt.finishedAt).toLocaleString('es-AR')}</p></div>
        <div className="result-score"><strong>{attempt.percentage.toLocaleString('es-AR',{maximumFractionDigits:1})}%</strong><span>{attempt.score} de {attempt.total} respuestas correctas</span></div>
        <div className="result-counts"><span><strong>{attempt.score}</strong> Correctas</span><span><strong>{answered-attempt.score}</strong> Incorrectas</span><span><strong>{attempt.total-answered}</strong> Sin responder</span></div>
      </section>
      <div className="results-actions"><h2>Revisión de respuestas</h2><button className="bank-primary" onClick={reset}>Volver al inicio de Intentos</button></div>
      <ol className="result-questions">{attempt.questions.map((q,i)=>{
        const correct=q.selectedOption===q.correctOption;
        return <li className="bank-box" key={q.id}><div className="box-title"><span className="eyebrow">Pregunta {i+1}</span><span className={'result-badge '+(correct?'correct':'incorrect')}>{q.selectedOption===null?'Sin responder':correct?'Correcta':'Incorrecta'}</span></div>
          <h3>{q.statement}</h3><ul>{q.options.map(option=><li className={(option.index===q.correctOption?'correct-result ':'')+(option.index===q.selectedOption&& !correct?'wrong-result':'')} key={option.index}>
            <span>{letter(option.index)}</span><p>{option.text}</p><small>{option.index===q.correctOption?'Respuesta correcta':option.index===q.selectedOption?'Tu respuesta':''}</small>
          </li>)}</ul>{q.explanation&&<div className="result-explanation"><strong>Explicación</strong><p>{q.explanation}</p></div>}
        </li>;
      })}</ol><button className="bank-primary" onClick={reset}>Volver al inicio de Intentos</button>
    </div>}
    </>}
  </div>;
}

import React, { useState, useEffect } from 'react';
import BankPanel from './BankPanel';
import AttemptPanel from './AttemptPanel';
import AuthPanel from './AuthPanel';
import MyQuestionnaires from './MyQuestionnaires';
import {SessionProvider,useSession} from './session';
import { createRoot } from 'react-dom/client';
import './styles.css';

function Icon({kind, ...props}) {
  const paths = {
    book: <><path d="M4 4h6a3 3 0 0 1 3 3v14a4 4 0 0 0-4-2H4z" /><path d="M20 4h-4a3 3 0 0 0-3 3v14a4 4 0 0 1 4-2h3z" /></>,
    list: <><rect x="5" y="3" width="14" height="18" rx="2" /><path d="m8 8 1 1 2-2m2 1h3m-8 6 1 1 2-2m2 1h3" /></>,
    chart: <><path d="M4 3v17h17M8 16v-4m5 4V8m5 8V5" /></>,
    arrow: <path d="M5 12h14m-6-6 6 6-6 6" />,
    check: <path d="m5 12 4 4L19 6" />
  };
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" {...props}>{paths[kind]}</svg>;
}

function App() {
  const {user,loading,error,refresh,logout}=useSession();
  const [sessionError,setSessionError]=useState('');
  const [route,setRoute]=useState(()=>{if(!window.location.hash)window.history.replaceState(null,'','#bienvenida');return window.location.hash;});
  useEffect(()=>{
    const update=()=>{setRoute(window.location.hash);if(window.location.hash!=='#tu-recorrido')window.scrollTo(0,0);};
    window.addEventListener('hashchange',update);
    return ()=>window.removeEventListener('hashchange',update);
  },[]);
  const isBank=(route==='#cuestionarios/nuevo' || route==='#bancos/nuevo');
  const isAttempt=route==='#intentos';
  const isMine=route==='#cuestionarios';
  const isAuth=route.startsWith('#ingresar')||route==='#registro'||route==='#perfil';
  const requiresLogin=(isBank||isAttempt||isMine)&&!user;
  const showAuth=isAuth||requiresLogin||user&&!user.profileComplete&&(isBank||isAttempt||isMine);
  async function signOut(){try{await logout();setSessionError('');}catch(e){setSessionError(e.message);}}
  useEffect(()=>{document.title=isAuth?'Cuenta · Cuestionarios Práctica':isMine?'Mis cuestionarios · Cuestionarios Práctica':isAttempt?'Intentos · Cuestionarios Práctica':isBank?'Nuevo cuestionario · Cuestionarios Práctica':'Bienvenida · Cuestionarios Práctica';},[isBank,isAttempt,isMine,isAuth]);
  const modules = [
    {kind:'book', title:'Cuestionarios', description:'Organizá los temas y las preguntas que querés practicar.'},
    {kind:'list', title:'Intentos', description:'Elegí un tema y prepará una sesión a tu medida.'},
    {kind:'chart', title:'Resultados', description:'Revisá tus respuestas y descubrí qué seguir repasando.'}
  ];
  return <>
    <a className="skip-link" href="#contenido">Ir al contenido</a>
    <header className="header">
      <a className="brand" href="#bienvenida" aria-label="Cuestionarios Práctica, inicio">
        <span className="brand-mark"><Icon kind="check" /></span>
        <span>Cuestionarios<span className="brand-sub">Práctica</span></span>
      </a>
      <nav aria-label="Navegación principal"><a href="#bienvenida" className={!isBank&&!isAttempt&&!isMine&&!isAuth?"nav-current":""} aria-current={!isBank&&!isAttempt&&!isMine&&!isAuth?"page":undefined}>Bienvenida</a><a href="#cuestionarios" className={isBank||isMine?"nav-current":""} aria-current={isBank||isMine?"page":undefined}>Cuestionarios</a><a href="#intentos" className={isAttempt?"nav-current":""} aria-current={isAttempt?"page":undefined}>Intentos</a>{!user&&<a className="header-login" href="#ingresar">Iniciar sesión</a>}</nav>
    </header>
    {user&&<div className="session-bar"><span>{[user.name,user.lastName].filter(Boolean).join(' ')}</span><a href="#perfil">Mi perfil</a><button className="text-button" onClick={signOut}>Cerrar sesión</button></div>}
    <main id="contenido">
      {sessionError&&<p className="bank-message error" role="alert">{sessionError}</p>}
      {error&&<p className="bank-message error" role="alert">{error}<button className="text-button" onClick={refresh}>Reintentar</button></p>}
      {loading?<p role="status">Cargando tu sesión…</p>:error?null:showAuth?<AuthPanel key={route+String(user?.id)} mode={user&&!user.profileComplete||route==='#perfil'?'profile':route==='#registro'?'register':'login'} />:isMine?<MyQuestionnaires />:isBank?<BankPanel />:isAttempt?<AttemptPanel user={user} />:<><section className="hero" aria-labelledby="welcome-title">
        <div className="hero-copy">
          <p className="eyebrow"><span /> Aprender, una pregunta a la vez</p>
          <h1 id="welcome-title">Lo que sabés.<br /><span>Lo que vas a aprender.</span></h1>
          <p className="intro">Bienvenido a tu espacio de práctica. Organizá tus preguntas, poné a prueba lo que aprendiste y volvé sobre lo que necesite un poco más de atención.</p>
          <a className="primary-link" href="#tu-recorrido">Conocé tu espacio <Icon kind="arrow" /></a>
          <p className="hero-note">A tu ritmo. Sin presión.</p>
        </div>
        <div className="question-scene" aria-label="Ejemplo ilustrativo de una pregunta de práctica">
          <div className="scene-label"><span className="small-line" /> Una pausa para pensar</div>
          <div className="question-card">
            <div className="card-top"><span>Pregunta de ejemplo</span><span className="example-tag">Práctica</span></div>
            <h2>¿Qué hace que una sesión de estudio valga la pena?</h2>
            <div className="sample-option"><span>A</span> Volver a leer sin detenerse.</div>
            <div className="sample-option selected"><span>B</span> Practicar y entender cada respuesta.<Icon kind="check" /></div>
            <div className="sample-option"><span>C</span> Recordar todo a la primera.</div>
            <p className="card-caption">Equivocarse también es parte de aprender.</p>
          </div>
          <div className="scene-foot"><span className="foot-dot" /> Cada intento es un nuevo punto de partida.</div>
        </div>
      </section>
      <section className="journey" id="tu-recorrido" aria-labelledby="journey-title">
        <div className="section-heading"><div><p className="eyebrow">Un lugar para cada paso</p><h2 id="journey-title">Tu recorrido de práctica</h2></div><p>Prepará tus temas, practicá y revisá lo aprendido.</p></div>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
          {modules.map(module=><article className="module" key={module.title}>
            <div className="module-top"><span className="module-icon"><Icon kind={module.kind} /></span><span className={module.kind!=='chart'?"soon available":"soon"}>{module.kind!=='chart'?"Disponible":"Próximamente"}</span></div>
            <h3>{module.title}</h3><p>{module.description}</p>{module.kind==='book'&&<a href="#cuestionarios/nuevo" className="module-link">Crear cuestionario →</a>}{module.kind==='list'&&<a href="#intentos" className="module-link">Realizar un intento →</a>}
          </article>)}
        </div>
      </section>
      <aside className="study-note"><Icon kind="book" /><p><strong>No hace falta saberlo todo para empezar.</strong> La práctica te ayuda a encontrar el próximo paso.</p></aside></>}
    </main>
    <footer><span>Cuestionarios Práctica</span><span>Un espacio para aprender y volver a intentar.</span></footer>
  </>;
}
createRoot(document.getElementById('root')).render(<React.StrictMode><SessionProvider><App /></SessionProvider></React.StrictMode>);

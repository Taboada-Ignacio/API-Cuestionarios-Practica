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
    dashboard: <><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" /></>,
    book: <><path d="M4 4h6a3 3 0 0 1 3 3v14a4 4 0 0 0-4-2H4z" /><path d="M20 4h-4a3 3 0 0 0-3 3v14a4 4 0 0 1 4-2h3z" /></>,
    list: <><rect x="5" y="3" width="14" height="18" rx="2" /><path d="m8 8 1 1 2-2m2 1h3m-8 6 1 1 2-2m2 1h3" /></>,
    chart: <><path d="M4 3v17h17M8 16v-4m5 4V8m5 8V5" /></>,
    plus: <path d="M12 5v14M5 12h14" />,
    user: <><circle cx="12" cy="8" r="4" /><path d="M4 21a8 8 0 0 1 16 0" /></>,
    logout: <><path d="M10 5H5v14h5M14 8l4 4-4 4m4-4H9" /></>,
    chevron: <path d="m15 18-6-6 6-6" />,
    arrow: <path d="M5 12h14m-6-6 6 6-6 6" />,
    check: <path d="m5 12 4 4L19 6" />
  };
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" {...props}>{paths[kind]}</svg>;
}

function Dashboard({user}) {
  const firstName=user.name?.trim().split(/\s+/)[0]||'estudiante';
  const cards=[
    {kind:'book',title:'Mis cuestionarios',description:'Administrá tus cuestionarios, borradores y contenidos publicados.',href:'#cuestionarios',action:'Ver cuestionarios'},
    {kind:'plus',title:'Crear cuestionario',description:'Cargá preguntas manualmente o importalas desde una planilla Excel.',href:'#cuestionarios/nuevo',action:'Crear ahora'},
    {kind:'list',title:'Realizar un intento',description:'Elegí un cuestionario y poné a prueba lo que aprendiste.',href:'#intentos',action:'Empezar práctica'},
    {kind:'chart',title:'Resultados',description:'Próximamente vas a poder revisar tu evolución y tus áreas de mejora.',disabled:true,action:'Próximamente'}
  ];
  return <section className="dashboard" aria-labelledby="dashboard-title">
    <div className="dashboard-welcome"><div><p className="eyebrow">Panel general</p><h1 id="dashboard-title">Hola, {firstName}</h1><p>Todo lo que necesitás para organizar y continuar tu práctica.</p></div><a className="bank-primary" href="#cuestionarios/nuevo"><Icon kind="plus" /> Nuevo cuestionario</a></div>
    <div className="dashboard-grid">{cards.map(card=>card.disabled?<article className="dashboard-card disabled" key={card.title} aria-disabled="true"><DashboardCard card={card} /></article>:<a className="dashboard-card" href={card.href} key={card.title}><DashboardCard card={card} /></a>)}</div>
    <aside className="dashboard-tip"><Icon kind="book" /><div><strong>Tu espacio de aprendizaje</strong><p>Creá contenido, practicá a tu ritmo y volvé cuando quieras.</p></div></aside>
  </section>;
}

function DashboardCard({card}) {return <><span className="dashboard-card-icon"><Icon kind={card.kind} /></span><h2>{card.title}</h2><p>{card.description}</p><span className="dashboard-card-action">{card.action}{!card.disabled&&' →'}</span></>}

function Sidebar({user,route,onLogout}) {
  const [collapsed,setCollapsed]=useState(()=>localStorage.getItem('sidebar-collapsed')==='true');
  function toggle(){setCollapsed(value=>{localStorage.setItem('sidebar-collapsed',String(!value));return !value;});}
  const items=[
    {href:'#dashboard',label:'Inicio',kind:'dashboard',active:route==='#dashboard'},
    {href:'#cuestionarios',label:'Mis cuestionarios',kind:'book',active:route==='#cuestionarios'||route.startsWith('#cuestionarios/')||route==='#bancos/nuevo'},
    {href:'#intentos',label:'Intentos',kind:'list',active:route==='#intentos'},
    {href:'#perfil',label:'Mi perfil',kind:'user',active:route==='#perfil'}
  ];
  return <aside className={'sidebar '+(collapsed?'collapsed':'')} aria-label="Navegación de la cuenta">
    <div className="sidebar-head"><a className="brand" href="#dashboard" aria-label="Cuestionarios Práctica, panel general"><span className="brand-mark"><Icon kind="check" /></span><span className="sidebar-label">Cuestionarios<span className="brand-sub">Práctica</span></span></a><button className="sidebar-toggle" onClick={toggle} aria-label={collapsed?'Expandir barra lateral':'Contraer barra lateral'} aria-expanded={!collapsed}><Icon kind="chevron" /></button></div>
    <nav className="sidebar-nav">{items.map(item=><a href={item.href} className={item.active?'active':''} aria-current={item.active?'page':undefined} key={item.href} title={collapsed?item.label:undefined}><Icon kind={item.kind} /><span className="sidebar-label">{item.label}</span></a>)}</nav>
    <div className="sidebar-account"><span className="sidebar-avatar">{user.name?.[0]}{user.lastName?.[0]}</span><div className="sidebar-user sidebar-label"><strong>{[user.name,user.lastName].filter(Boolean).join(' ')}</strong><span>{user.email}</span></div><button onClick={onLogout} aria-label="Cerrar sesión" title="Cerrar sesión"><Icon kind="logout" /></button></div>
  </aside>;
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
  const editMatch=route.match(/^#cuestionarios\/editar\/([0-9a-f-]+)$/i);
  const isBank=(route==='#cuestionarios/nuevo' || route==='#bancos/nuevo'||!!editMatch);
  const isAttempt=route==='#intentos';
  const isMine=route==='#cuestionarios';
  const isDashboard=route==='#dashboard';
  const isAuth=route.startsWith('#ingresar')||route==='#registro'||route==='#perfil';
  const requiresLogin=(isBank||isAttempt||isMine||isDashboard)&&!user;
  const showAuth=isAuth||requiresLogin||user&&!user.profileComplete&&(isBank||isAttempt||isMine||isDashboard);
  async function signOut(){try{await logout();setSessionError('');}catch(e){setSessionError(e.message);}}
  useEffect(()=>{if(loading||!user)return;if(!user.profileComplete&&route!=='#perfil')window.location.hash='#perfil';else if(user.profileComplete&&(route==='#bienvenida'||route==='#ingresar'||route==='#registro'||!route))window.location.hash='#dashboard';},[loading,user,route]);
  useEffect(()=>{document.title=isAuth?'Cuenta · Cuestionarios Práctica':isDashboard?'Panel general · Cuestionarios Práctica':isMine?'Mis cuestionarios · Cuestionarios Práctica':isAttempt?'Intentos · Cuestionarios Práctica':isBank?'Nuevo cuestionario · Cuestionarios Práctica':'Bienvenida · Cuestionarios Práctica';},[isBank,isAttempt,isMine,isAuth,isDashboard]);
  const modules = [
    {kind:'book', title:'Cuestionarios', description:'Organizá los temas y las preguntas que querés practicar.'},
    {kind:'list', title:'Intentos', description:'Elegí un tema y prepará una sesión a tu medida.'},
    {kind:'chart', title:'Resultados', description:'Revisá tus respuestas y descubrí qué seguir repasando.'}
  ];
  return <>
    <a className="skip-link" href="#contenido">Ir al contenido</a>
    {!user&&<header className="header">
      <a className="brand" href="#bienvenida" aria-label="Cuestionarios Práctica, inicio">
        <span className="brand-mark"><Icon kind="check" /></span>
        <span>Cuestionarios<span className="brand-sub">Práctica</span></span>
      </a>
      <nav aria-label="Navegación principal"><a href="#bienvenida" className={!isBank&&!isAttempt&&!isMine&&!isAuth?"nav-current":""} aria-current={!isBank&&!isAttempt&&!isMine&&!isAuth?"page":undefined}>Bienvenida</a><a href="#cuestionarios">Cuestionarios</a><a href="#intentos">Intentos</a><a className="header-login" href="#ingresar">Iniciar sesión</a></nav>
    </header>}
    <div className={user?'app-shell':''}>
    {user&&<Sidebar user={user} route={route} onLogout={signOut} />}
    <main id="contenido" className={user?'app-content':''}>
      {sessionError&&<p className="bank-message error" role="alert">{sessionError}</p>}
      {error&&<p className="bank-message error" role="alert">{error}<button className="text-button" onClick={refresh}>Reintentar</button></p>}
      {loading?<p role="status">Cargando tu sesión…</p>:error?null:showAuth?<AuthPanel key={route+String(user?.id)} mode={user&&!user.profileComplete||route==='#perfil'?'profile':route==='#registro'?'register':'login'} />:isDashboard?<Dashboard user={user} />:isMine?<MyQuestionnaires />:isBank?<BankPanel key={editMatch?.[1]||'new'} bankId={editMatch?.[1]} user={user} />:isAttempt?<AttemptPanel user={user} />:<><section className="hero" aria-labelledby="welcome-title">
        <div className="hero-copy">
          <p className="eyebrow"><span /> Aprender, una pregunta a la vez</p>
          <h1 id="welcome-title">Lo que sabés.<br /><span>Lo que vas a aprender.</span></h1>
          <p className="intro">Bienvenido a tu espacio de práctica. Organizá tus preguntas, poné a prueba lo que aprendiste y volvé sobre lo que necesite un poco más de atención.</p>
          <a className="primary-link" href="#ingresar">Comienza ahora <Icon kind="arrow" /></a>
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
            <h3>{module.title}</h3><p>{module.description}</p>
          </article>)}
        </div>
      </section>
      <aside className="study-note"><Icon kind="book" /><p><strong>No hace falta saberlo todo para empezar.</strong> La práctica te ayuda a encontrar el próximo paso.</p></aside></>}
    </main>
    </div>
    {!user&&<footer><span>Cuestionarios Práctica</span><span>Un espacio para aprender y volver a intentar.</span></footer>}
  </>;
}
createRoot(document.getElementById('root')).render(<React.StrictMode><SessionProvider><App /></SessionProvider></React.StrictMode>);

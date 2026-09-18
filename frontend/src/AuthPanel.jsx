import React,{useEffect,useState} from 'react';
import {api} from './attemptApi';
import {useSession} from './session';
import AcademicFields,{emptyAcademic} from './AcademicFields';
import './auth.css';
import {passwordRules} from './passwordRules';
export default function AuthPanel({mode='login'}){
 const {user,setUser}=useSession();const profile=!!user&&(mode==='profile'||!user.profileComplete);const register=mode==='register'&&!profile;
 const [name,setName]=useState(user?.name||''),[lastName,setLastName]=useState(user?.lastName||''),[email,setEmail]=useState(''),[password,setPassword]=useState('');
 const rules=passwordRules(password);
 const [academic,setAcademic]=useState(()=>user?{university:user.university||'',faculty:user.faculty||'',career:user.career||'',note:user.academicNote||''}:emptyAcademic());
 const [busy,setBusy]=useState(false),[error,setError]=useState(window.location.hash.includes('google=error')?'No se pudo ingresar con Google. Si ya tenés una cuenta con ese email, ingresá con contraseña y vinculá Google desde tu perfil.':''),[config,setConfig]=useState(null),[notice,setNotice]=useState('');
 useEffect(()=>{api('/auth/config').then(setConfig).catch(()=>setConfig({googleEnabled:false}));},[]);
 async function submit(e){e.preventDefault();if(busy)return;setError('');setNotice('');if((profile||register)&&(!academic.university||!academic.faculty||!academic.career)){setError('Seleccioná universidad, facultad y carrera de los resultados, o elegí «No posee».');return;}
 if(register&&!rules.every(r=>r.met)){setError('Completá todos los requisitos de la contraseña.');return;}
 setBusy(true);try{
 const result=await api(profile?'/auth/perfil':register?'/auth/registro':'/auth/ingresar',{method:profile?'PUT':'POST',body:JSON.stringify(profile?{name,lastName,academic}:register?{name,lastName,email,password,academic}:{email,password})});setUser(result);window.location.hash='#bienvenida';
 }catch(e){setError(e.message);}finally{setBusy(false);}}
 async function link(){setError('');setBusy(true);try{const data=await api('/auth/google/vincular',{method:'POST'});window.location.assign(data.url);}catch(e){setError(e.message);setBusy(false);}}
 return <div className="auth-layout"><aside className="auth-note"><p className="eyebrow">Tu espacio de práctica</p><h1>{profile?'Una cuenta, tu recorrido.':register?'Empezá con lo que sabés.':'Volvé a tu práctica.'}</h1><p>{profile?'Completá tu formación para cargar cuestionarios y realizar intentos.': 'Tus cuestionarios, tus intentos y lo que vas aprendiendo, en un mismo lugar.'}</p><div className="auth-study"><span>Cuestionarios</span><span>Práctica</span><span>Aprendizaje</span></div></aside>
 <section className="bank-box auth-form"><h2>{profile?'Datos académicos':register?'Crear una cuenta':'Iniciar sesión'}</h2>{user&&profile&&<p>{user.name} · {user.email}</p>}{error&&<div className="bank-message error" role="alert">{error}</div>}{notice&&<p role="status">{notice}</p>}
 <form onSubmit={submit}>{(register||profile)&&<div className="person-fields"><label className="field">Nombre<input required maxLength={200} autoComplete="given-name" value={name} onChange={e=>setName(e.target.value)} /></label><label className="field">Apellido<input required maxLength={200} autoComplete="family-name" value={lastName} onChange={e=>setLastName(e.target.value)} /></label></div>}{!profile&&<><label className="field">Email<input required type="email" maxLength={254} autoComplete="email" value={email} onChange={e=>setEmail(e.target.value)} /></label><label className="field">Contraseña<input required type="password" minLength={register?8:undefined} aria-describedby={register?'password-requirements':undefined} maxLength={72} autoComplete={register?'new-password':'current-password'} value={password} onChange={e=>setPassword(e.target.value)} /></label>{register&&<div id="password-requirements" className="password-requirements"><p>Requisitos de la contraseña</p><ul>{rules.map(rule=><li key={rule.id} className={rule.met?'met':''}><span aria-hidden="true">{rule.met?'✓':'○'}</span>{rule.label}<span className="sr-only">{rule.met?', cumplido':', pendiente'}</span></li>)}</ul><small role="status" aria-live="polite">{rules.filter(r=>r.met).length} de {rules.length} requisitos cumplidos</small></div>}</>}
 {(register||profile)&&<AcademicFields value={academic} onChange={setAcademic} disabled={busy} />}
 <button className="bank-primary" disabled={busy} type="submit">{busy?'Guardando…':profile?'Guardar y continuar':register?'Crear cuenta':'Ingresar'}</button></form>
 {!profile&&<><div className="auth-divider">o</div>{config?.googleEnabled?<a className="bank-secondary google-button" href="/api/v1/auth/google/authorize/google">Continuar con Google</a>:<><button className="bank-secondary google-button" disabled>Continuar con Google</button><p className="field-help">El acceso con Google estará disponible cuando se configure.</p></>}<p className="auth-switch">{register?'¿Ya estás registrado?':'¿No estás registrado?'} <a className={register?'':'register-link'} href={register?'#ingresar':'#registro'}>{register?'Iniciar sesión':'Registrate acá'}</a></p></>}
 {profile&&config?.googleEnabled&&!user.googleLinked&&<button className="bank-secondary" disabled={busy} onClick={link}>Vincular mi cuenta con Google</button>}{profile&&user.googleLinked&&<p className="field-help">Tu cuenta está vinculada con Google.</p>}
 </section></div>;
}

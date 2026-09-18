import React,{createContext,useContext,useEffect,useState} from 'react';
import {api} from './attemptApi';
const Session=createContext(null);
export function SessionProvider({children}){
 const [user,setUser]=useState(null),[loading,setLoading]=useState(true),[error,setError]=useState('');
 async function refresh(){setError('');setLoading(true);try{setUser(await api('/auth/me'));}catch(e){if(e.status===401)setUser(null);else setError(e.message);}finally{setLoading(false);}}
 useEffect(()=>{refresh();const expired=()=>{setUser(null);window.location.hash='#ingresar';};window.addEventListener('session-expired',expired);return()=>window.removeEventListener('session-expired',expired);},[]);
 async function logout(){await api('/auth/salir',{method:'POST'});setUser(null);window.location.hash='#bienvenida';}
 return <Session.Provider value={{user,setUser,loading,error,refresh,logout}}>{children}</Session.Provider>;
}
export const useSession=()=>useContext(Session);

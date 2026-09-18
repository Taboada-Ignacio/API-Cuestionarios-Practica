package ar.com.cuestionarios.backend;
import org.springframework.stereotype.Component;
import java.util.*;
import static org.springframework.http.HttpStatus.*;
@Component
class LoginThrottle {
 record Window(long until,int count){}
 private final Map<String,Window> windows=new HashMap<>();
 synchronized void check(String key,int limit){
  long now=System.currentTimeMillis();windows.entrySet().removeIf(e->e.getValue().until()<now);
  Window w=windows.get(key);if(w==null)w=new Window(now+900000,0);
  if(w.count()>=limit||windows.size()>=10000&&!windows.containsKey(key))throw new BackendException(TOO_MANY_REQUESTS,"Demasiados intentos. Esperá 15 minutos antes de volver a intentar.");
  windows.put(key,new Window(w.until(),w.count()+1));
 }
}

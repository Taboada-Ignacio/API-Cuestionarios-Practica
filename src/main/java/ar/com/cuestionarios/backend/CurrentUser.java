package ar.com.cuestionarios.backend;
import org.springframework.stereotype.Component;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.springframework.http.HttpStatus.*;
import java.util.UUID;
@Component
class CurrentUser {
 private final BackendRepository repository;
 CurrentUser(BackendRepository repository){this.repository=repository;}
 UUID id(){
  var auth=SecurityContextHolder.getContext().getAuthentication();
  if(auth==null || !(auth.getPrincipal() instanceof LocalPrincipal p))throw new BackendException(UNAUTHORIZED,"Iniciá sesión para continuar");
  return p.id();
 }
 UserAccount account(){UserAccount u=repository.get(UserAccount.class,id());if(!u.active)throw new BackendException(UNAUTHORIZED,"La cuenta está desactivada");return u;}
 UserAccount complete(){UserAccount u=account();if(u.university==null||u.faculty==null||u.career==null||u.lastName==null||u.lastName.isBlank())throw new BackendException(FORBIDDEN,"Completá tu nombre, apellido y datos académicos para continuar");return u;}
}

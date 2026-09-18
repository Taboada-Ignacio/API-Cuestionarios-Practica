package ar.com.cuestionarios.backend;
import jakarta.validation.Valid;
import jakarta.servlet.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import static ar.com.cuestionarios.backend.AuthModels.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/auth")
class AuthController {
 private final AuthService service;private final CurrentUser current;private final LoginThrottle throttle;
 @Value("${app.google.enabled:false}") boolean googleEnabled;
 AuthController(AuthService s,CurrentUser c,LoginThrottle t){service=s;current=c;throttle=t;}
 @GetMapping("/csrf") Map<String,String> csrf(CsrfToken token){return Map.of("token",token.getToken(),"headerName",token.getHeaderName());}
 @GetMapping("/config") Map<String,Boolean> config(){return Map.of("googleEnabled",googleEnabled);}
 @GetMapping("/me") UserView me(){return service.me();}
 @PostMapping("/registro") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
 UserView register(@Valid @RequestBody RegisterInput i,HttpServletRequest request,HttpServletResponse response){throttle.check("register:"+request.getRemoteAddr(),30);UserAccount u=service.register(i);signIn(u,request,response);return service.view(u);}
 @PostMapping("/ingresar") UserView login(@Valid @RequestBody LoginInput i,HttpServletRequest request,HttpServletResponse response){throttle.check("login:"+request.getRemoteAddr()+":"+service.email(i.email()),10);throttle.check("ip:"+request.getRemoteAddr(),100);UserAccount u=service.login(i);signIn(u,request,response);return service.view(u);}
 @PutMapping("/perfil") UserView profile(@Valid @RequestBody ProfileInput i){return service.profile(i);}
 @PostMapping("/google/vincular") Map<String,String> link(HttpServletRequest request){if(!googleEnabled)throw new BackendException(org.springframework.http.HttpStatus.CONFLICT,"Google todavía no está configurado");request.getSession().setAttribute("googleLinkUser",current.id());return Map.of("url","/api/v1/auth/google/authorize/google");}
 static void signIn(UserAccount u,HttpServletRequest request,HttpServletResponse response){
  if(request.getSession(false)!=null)request.changeSessionId();else request.getSession();
  new HttpSessionCsrfTokenRepository().saveToken(null,request,response);
  var context=SecurityContextHolder.createEmptyContext();context.setAuthentication(new UsernamePasswordAuthenticationToken(new LocalPrincipal(u.id),null,List.of(new SimpleGrantedAuthority("ROLE_USER"))));SecurityContextHolder.setContext(context);
  new HttpSessionSecurityContextRepository().saveContext(context,request,response);
 }
}

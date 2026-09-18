package ar.com.cuestionarios.backend;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.springframework.http.HttpStatus.*;
import static ar.com.cuestionarios.backend.AuthModels.*;
import java.util.*;
import java.time.Instant;
@Service @Transactional
class AuthService {
 private final BackendRepository repository;private final PasswordEncoder encoder;private final AcademicCatalog catalog;private final CurrentUser current;
 private final String dummy;
 AuthService(BackendRepository r,PasswordEncoder e,AcademicCatalog c,CurrentUser u){repository=r;encoder=e;catalog=c;current=u;dummy=e.encode("constant-timing-placeholder");}
 String email(String value){return value.trim().toLowerCase(Locale.ROOT);}
 Optional<UserAccount> byEmail(String email){return repository.userByEmail(email);}
 UserAccount register(RegisterInput i){
  catalog.validate(i.academic());
  if(i.lastName()==null||i.lastName().isBlank())throw new BackendException(BAD_REQUEST,"Ingresá tu apellido");
  if(i.password().codePointCount(0,i.password().length())<8||!i.password().codePoints().anyMatch(Character::isUpperCase)||!i.password().codePoints().anyMatch(Character::isLowerCase)||i.password().chars().noneMatch(c->c>='0'&&c<='9'))throw new BackendException(BAD_REQUEST,"La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula y un número");
  if(i.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)throw new BackendException(BAD_REQUEST,"La contraseña supera los 72 bytes permitidos");
  if(byEmail(email(i.email())).isPresent())throw new BackendException(CONFLICT,"No se pudo registrar ese email. Probá iniciar sesión.");
  UserAccount u=new UserAccount();u.name=i.name().trim();u.lastName=i.lastName().trim();u.email=email(i.email());u.passwordHash=encoder.encode(i.password());apply(u,i.academic());repository.save(u);repository.flush();return u;
 }
 UserAccount login(LoginInput i){
  if(i.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)throw new BackendException(UNAUTHORIZED,"Email o contraseña incorrectos");
  UserAccount u=byEmail(email(i.email())).orElse(null);
  boolean matches=encoder.matches(i.password(),u==null||u.passwordHash==null?dummy:u.passwordHash);
  if(u==null||u.passwordHash==null||!u.active||!matches)throw new BackendException(UNAUTHORIZED,"Email o contraseña incorrectos");return u;
 }
 UserView me(){return view(current.account());}
 UserView profile(ProfileInput i){catalog.validate(i.academic());UserAccount u=current.account();u.name=i.name().trim();u.lastName=i.lastName().trim();apply(u,i.academic());return view(u);}
 void apply(UserAccount u,AcademicInput i){u.university=i.university();u.faculty=i.faculty();u.career=i.career();u.academicNote=i.note()==null?null:i.note().trim();u.updatedAt=Instant.now();}
 UserView view(UserAccount u){return new UserView(u.id,u.name,u.lastName,u.email,u.university,u.faculty,u.career,u.academicNote,u.university!=null&&u.faculty!=null&&u.career!=null&&u.lastName!=null&&!u.lastName.isBlank(),u.googleSubject!=null,u.createdAt);}
 UserAccount google(String subject,String email,String name,String lastName,boolean verified,UUID link){
  if(!verified||subject==null||email==null)throw new BackendException(UNAUTHORIZED,"Google no devolvió una identidad verificada");
  var existing=repository.userByGoogle(subject);
  if(link!=null){
   UserAccount u=repository.get(UserAccount.class,link);if(!u.active||!u.email.equals(email(email)))throw new BackendException(CONFLICT,"El email de Google debe coincidir con tu cuenta");
   if(existing.isPresent()&&!existing.get().id.equals(link))throw new BackendException(CONFLICT,"La identidad de Google ya está vinculada");
   if(u.googleSubject!=null&&!u.googleSubject.equals(subject))throw new BackendException(CONFLICT,"Tu cuenta ya tiene otra identidad de Google");
   u.googleSubject=subject;u.updatedAt=Instant.now();return u;
  }
  if(existing.isPresent()){if(!existing.get().active)throw new BackendException(UNAUTHORIZED,"Cuenta desactivada");return existing.get();}
  if(byEmail(email(email)).isPresent())throw new BackendException(CONFLICT,"Iniciá sesión con contraseña y vinculá Google desde tu perfil");
  UserAccount u=new UserAccount();u.email=email(email);u.name=name==null||name.isBlank()?"Usuario Google":name.substring(0,Math.min(name.length(),200));u.lastName=lastName==null?"":lastName.substring(0,Math.min(lastName.length(),200)).trim();u.googleSubject=subject;repository.save(u);repository.flush();return u;
 }
}

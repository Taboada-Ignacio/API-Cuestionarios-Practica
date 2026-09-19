package ar.com.cuestionarios.backend;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.password.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
@Configuration
class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}
 @Bean SecurityFilterChain security(HttpSecurity http,AuthService service,
 @Value("${app.google.enabled:false}") boolean googleEnabled,
 @Value("${app.google.client-id:}") String clientId,@Value("${app.google.client-secret:}") String clientSecret) throws Exception {
  http.authorizeHttpRequests(a->a.requestMatchers("/api/v1/health","/api/v1/academia/**","/api/v1/auth/csrf","/api/v1/auth/config","/api/v1/auth/registro","/api/v1/auth/ingresar","/api/v1/auth/me","/api/v1/auth/google/authorize/**","/api/v1/auth/google/callback/**","/error").permitAll().anyRequest().authenticated());
  http.requestCache(c->c.disable());
  http.exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->error(res,401,"Iniciá sesión para continuar")).accessDeniedHandler((req,res,ex)->error(res,403,"Acceso denegado o sesión de formulario vencida. Volvé a intentar.")));
  http.logout(l->l.logoutUrl("/api/v1/auth/salir").invalidateHttpSession(true).clearAuthentication(true).deleteCookies("JSESSIONID").logoutSuccessHandler((req,res,auth)->{res.setContentType("application/json");res.getWriter().write("{\"ok\":true}");}));
  http.headers(h->h.contentTypeOptions(c->{}).frameOptions(f->f.deny()).referrerPolicy(p->p.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)));
  if(googleEnabled){
   if(clientId.isBlank()||clientSecret.isBlank())throw new IllegalStateException("Google habilitado sin credenciales");
   var registration=ClientRegistration.withRegistrationId("google").clientId(clientId).clientSecret(clientSecret).authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE).redirectUri("{baseUrl}/api/v1/auth/google/callback/{registrationId}").scope("openid","profile","email").authorizationUri("https://accounts.google.com/o/oauth2/v2/auth").tokenUri("https://oauth2.googleapis.com/token").jwkSetUri("https://www.googleapis.com/oauth2/v3/certs").issuerUri("https://accounts.google.com").userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo").userNameAttributeName("sub").clientName("Google").build();
   http.oauth2Login(o->o.clientRegistrationRepository(new InMemoryClientRegistrationRepository(registration)).authorizationEndpoint(a->a.baseUri("/api/v1/auth/google/authorize")).redirectionEndpoint(e->e.baseUri("/api/v1/auth/google/callback/*"))
    .successHandler((req,res,auth)->{
     Object pending=req.getSession().getAttribute("googleLinkUser");req.getSession().removeAttribute("googleLinkUser");
     try {OidcUser oidc=(OidcUser)auth.getPrincipal();UserAccount u=service.google(oidc.getSubject(),oidc.getEmail(),oidc.getGivenName(),oidc.getFamilyName(),Boolean.TRUE.equals(oidc.getEmailVerified()),pending instanceof UUID id?id:null);AuthController.signIn(u,req,res);res.sendRedirect("/#dashboard");}
     catch(BackendException ex){req.getSession().invalidate();org.springframework.security.core.context.SecurityContextHolder.clearContext();res.sendRedirect("/#ingresar?google=error");}
    }).failureHandler((req,res,ex)->{if(req.getSession(false)!=null)req.getSession().removeAttribute("googleLinkUser");res.sendRedirect("/#ingresar?google=error");}));
  }
  return http.build();
 }
 private static void error(HttpServletResponse res,int status,String detail) throws java.io.IOException {res.setStatus(status);res.setContentType("application/problem+json;charset=UTF-8");res.getWriter().write("{\"status\":"+status+",\"detail\":\""+detail+"\"}");}
}

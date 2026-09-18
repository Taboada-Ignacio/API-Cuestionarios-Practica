package ar.com.cuestionarios.backend;
import ar.com.cuestionarios.PostgresIntegrationSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import java.net.*;
import java.net.http.*;
import java.util.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
class SecurityIntegrationTests extends PostgresIntegrationSupport {
 final JsonMapper mapper=new JsonMapper();
 class Client {
  final HttpClient client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
  String base="http://localhost:"+((WebServerApplicationContext)context).getWebServer().getPort()+"/api/v1";
  HttpResponse<String> send(String method,String path,Object body,boolean csrf) throws Exception {
   var builder=HttpRequest.newBuilder(URI.create(base+path)).header("Content-Type","application/json");
   if(csrf){var token=send("GET","/auth/csrf",null,false);JsonNode node=mapper.readTree(token.body());builder.header(node.path("headerName").asText(),node.path("token").asText());}
   return client.send(builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
  }
  JsonNode ok(String method,String path,Object body,int status) throws Exception{var response=send(method,path,body,!method.equals("GET"));assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(status);return mapper.readTree(response.body());}
  JsonNode register(String email) throws Exception {return ok("POST","/auth/registro",Map.of("name","Ana","lastName","Pérez","email",email,"password","Correct-horse-battery1","academic",Map.of("university","NONE","faculty","NONE","career","NONE")),201);}
 }
 @Test void sesionesCsrfPropiedadEstadosEIntentos() throws Exception {
  Client owner=new Client(),other=new Client(),anonymous=new Client();
  assertThat(anonymous.send("GET","/bancos",null,false).statusCode()).isEqualTo(401);
  assertThat(anonymous.send("POST","/auth/registro",Map.of(),false).statusCode()).isEqualTo(403);
  String email="owner-"+UUID.randomUUID()+"@example.com";
  JsonNode user=owner.register(email);other.register("other-"+UUID.randomUUID()+"@example.com");
  assertThat(user.has("passwordHash")).isFalse();assertThat(user.path("profileComplete").asBoolean()).isTrue();
  assertThat(owner.ok("GET","/auth/me",null,200).path("id").asText()).isEqualTo(user.path("id").asText());
  assertThat(owner.send("PUT","/auth/perfil",Map.of("name","Ana","lastName","Pérez","academic",Map.of("university","NONE","faculty","INVALID","career","NONE")),true).statusCode()).isEqualTo(400);
  var question=Map.of("statement","Pregunta","explanation","Explicación","options",List.of(Map.of("text","A","correct",true),Map.of("text","B","correct",false)));
  JsonNode bank=owner.ok("POST","/bancos/carga",Map.of("name","Privado","questions",List.of(question)),201);String id=bank.path("id").asText();
  assertThat(bank.path("status").asText()).isEqualTo("BORRADOR");
  assertThat(owner.send("POST","/bancos/"+id+"/intentos",Map.of("questionCount",1),true).statusCode()).isEqualTo(409);
  assertThat(other.send("GET","/bancos/"+id,null,false).statusCode()).isEqualTo(404);
  assertThat(other.send("PUT","/bancos/"+id+"/estado",Map.of("status","PUBLICO"),true).statusCode()).isEqualTo(404);
  owner.ok("PUT","/bancos/"+id+"/estado",Map.of("status","PRIVADO"),200);
  assertThat(other.send("POST","/bancos/"+id+"/intentos",Map.of("questionCount",1),true).statusCode()).isEqualTo(404);
  owner.ok("PUT","/bancos/"+id+"/estado",Map.of("status","PUBLICO"),200);
  assertThat(other.ok("GET","/bancos?scope=practice",null,200).path("items").size()).isEqualTo(1);
  assertThat(other.ok("GET","/bancos",null,200).path("total").asInt()).isZero();
  assertThat(other.send("GET","/bancos/"+id+"/preguntas",null,false).statusCode()).isEqualTo(404);
  JsonNode attempt=other.ok("POST","/bancos/"+id+"/intentos",Map.of("questionCount",1,"participantName","Suplantación"),201);String aid=attempt.path("id").asText();
  assertThat(attempt.path("participantName").asText()).isEqualTo("Ana Pérez");assertThat(attempt.path("questions").get(0).path("correctOption").isNull()).isTrue();
  assertThat(owner.send("GET","/intentos/"+aid,null,false).statusCode()).isEqualTo(404);
  assertThat(owner.send("POST","/intentos/"+aid+"/finalizar",null,true).statusCode()).isEqualTo(404);
  String qid=attempt.path("questions").get(0).path("id").asText();
  assertThat(owner.send("PUT","/intentos/"+aid+"/respuestas/"+qid,Map.of("optionIndex",0),true).statusCode()).isEqualTo(404);
  other.ok("PUT","/intentos/"+aid+"/respuestas/"+qid,Map.of("optionIndex",0),200);
  assertThat(other.ok("POST","/intentos/"+aid+"/finalizar",null,200).path("score").asInt()).isEqualTo(1);
  owner.ok("POST","/auth/salir",null,200);assertThat(owner.send("GET","/auth/me",null,false).statusCode()).isEqualTo(401);
  assertThat(owner.send("POST","/auth/ingresar",Map.of("email",email,"password","wrong-password"),true).statusCode()).isEqualTo(401);
  owner.ok("POST","/auth/ingresar",Map.of("email",email,"password","Correct-horse-battery1"),200);
  assertThat(owner.ok("GET","/bancos",null,200).path("total").asInt()).isEqualTo(1);
 }
 @Test void registroExigeApellidoYLasCuatroReglasDeContrasena() throws Exception {
  Client client=new Client();String email="rules-"+UUID.randomUUID()+"@example.com";
  var academic=Map.of("university","NONE","faculty","NONE","career","NONE");
  assertThat(client.send("POST","/auth/registro",Map.of("name","Ana","email",email,"password","Abcdefg1","academic",academic),true).statusCode()).isEqualTo(400);
  for(String password:List.of("abcdefg1","ABCDEFG1","Abcdefgh","Abc1")){
   assertThat(client.send("POST","/auth/registro",Map.of("name","Ana","lastName","Pérez","email",email,"password",password,"academic",academic),true).statusCode()).isEqualTo(400);
  }
  JsonNode created=client.ok("POST","/auth/registro",Map.of("name","Ana","lastName","Pérez","email",email,"password","Abcdefg1","academic",academic),201);
  assertThat(created.path("lastName").asText()).isEqualTo("Pérez");
  JsonNode updated=client.ok("PUT","/auth/perfil",Map.of("name","Ana","lastName","Gómez","academic",academic),200);
  assertThat(updated.path("lastName").asText()).isEqualTo("Gómez");
 }
 @Test void googleNoVinculaCuentasSoloPorEmail() {
  var auth=context.getBean(AuthService.class);
  String email="local-"+UUID.randomUUID()+"@example.com";
  UserAccount local=auth.register(new AuthModels.RegisterInput("Ana","Pérez",email,"Correct-horse-battery1",new AuthModels.AcademicInput("NONE","NONE","NONE",null)));
  assertThatThrownBy(()->auth.google("subject-"+UUID.randomUUID(),email,"Ana","Pérez",true,null)).isInstanceOf(BackendException.class);
  assertThatThrownBy(()->auth.google("subject-"+UUID.randomUUID(),"other-"+email,"Ana","Pérez",false,null)).isInstanceOf(BackendException.class);
  UserAccount google=auth.google("subject-"+UUID.randomUUID(),"google-"+email,"Ana","Pérez",true,null);
  assertThat(auth.view(google).profileComplete()).isFalse();assertThat(google.passwordHash).isNull();
  assertThat(auth.google("linked-"+local.id,email,"Ana","Pérez",true,local.id).googleSubject).isEqualTo("linked-"+local.id);
 }
}

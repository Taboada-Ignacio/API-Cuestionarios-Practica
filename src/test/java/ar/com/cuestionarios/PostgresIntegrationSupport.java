package ar.com.cuestionarios;

import org.junit.jupiter.api.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** JUnit 5: arranque explícito de Spring Boot; DB aislada en Compose o Testcontainers local. */
public abstract class PostgresIntegrationSupport {
    protected static ConfigurableApplicationContext context;
    private static PostgreSQLContainer postgres;
    @BeforeAll static void startApplication() {
        String url=System.getenv("INTEGRATION_DB_URL");
        String username=System.getenv("INTEGRATION_DB_USERNAME");
        String password=System.getenv("INTEGRATION_DB_PASSWORD");
        if(url==null || url.isBlank()) {
            postgres=new PostgreSQLContainer("postgres:17");
            postgres.start();
            url=postgres.getJdbcUrl();username=postgres.getUsername();password=postgres.getPassword();
        }
        try {
            context=new SpringApplicationBuilder(ApiCuestionariosPracticaApplication.class)
                .run("--server.port=0","--spring.datasource.url="+url,
                    "--spring.datasource.username="+username,
                    "--spring.datasource.password="+password);
        } catch(RuntimeException ex) {
            if(postgres!=null) { postgres.stop(); postgres=null; }
            throw ex;
        }
    }
    @AfterAll static void stopApplication() {
        if(context!=null) { context.close(); context=null; }
        if(postgres!=null) { postgres.stop(); postgres=null; }
    }
}

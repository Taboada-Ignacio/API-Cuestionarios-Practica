package ar.com.cuestionarios;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ApiCuestionariosPracticaApplicationTests extends PostgresIntegrationSupport {
    @Test void arrancaConPostgresqlYFlyway() throws Exception {
        try(var connection=context.getBean(DataSource.class).getConnection();
            var statement=connection.createStatement();
            var result=statement.executeQuery("select version()")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getString(1)).startsWith("PostgreSQL 17.");
        }
        assertThat(context.getBean(Flyway.class).validateWithResult().validationSuccessful).isTrue();
    }
}

package ar.com.cuestionarios;

import org.springframework.boot.SpringApplication;

public class TestApiCuestionariosPracticaApplication {

	public static void main(String[] args) {
		SpringApplication.from(ApiCuestionariosPracticaApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

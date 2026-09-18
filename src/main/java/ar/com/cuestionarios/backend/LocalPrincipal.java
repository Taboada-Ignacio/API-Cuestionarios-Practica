package ar.com.cuestionarios.backend;
import java.io.Serializable;
import java.util.UUID;
public record LocalPrincipal(UUID id) implements Serializable {}

package ar.com.cuestionarios.backend;

import org.springframework.http.HttpStatus;

class BackendException extends RuntimeException {
    final HttpStatus status;
    BackendException(HttpStatus status,String message) { super(message); this.status=status; }
}

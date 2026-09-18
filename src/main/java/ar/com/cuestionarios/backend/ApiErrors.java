package ar.com.cuestionarios.backend;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.*;
import java.util.*;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(BackendException.class)
    ResponseEntity<ProblemDetail> domain(BackendException ex) { return problem(ex.status,ex.getMessage()); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException ex) {
        ProblemDetail body=ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"Datos inválidos");
        body.setProperty("errors",ex.getBindingResult().getFieldErrors().stream()
            .map(e->Map.of("field",e.getField(),"message",Objects.toString(e.getDefaultMessage(),"Inválido"))).toList());
        return ResponseEntity.badRequest().body(body);
    }
    @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class})
    ResponseEntity<ProblemDetail> invalid(Exception ex) { return problem(HttpStatus.BAD_REQUEST,"Solicitud o identificador inválido"); }
    @ExceptionHandler({DataIntegrityViolationException.class,PessimisticLockingFailureException.class})
    ResponseEntity<ProblemDetail> conflict(Exception ex) { return problem(HttpStatus.CONFLICT,"Conflicto al guardar; reintentá la operación"); }
    private ResponseEntity<ProblemDetail> problem(HttpStatus status,String detail) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status,detail));
    }
}

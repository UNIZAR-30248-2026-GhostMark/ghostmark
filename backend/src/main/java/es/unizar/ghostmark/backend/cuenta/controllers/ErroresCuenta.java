package es.unizar.ghostmark.backend.cuenta.controllers;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(
    assignableTypes = {
        ControladorCuenta.class,
        ControladorHorario.class
    })
public class ErroresCuenta {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> manejar(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode())
            .body(error.getBody());
    }
}

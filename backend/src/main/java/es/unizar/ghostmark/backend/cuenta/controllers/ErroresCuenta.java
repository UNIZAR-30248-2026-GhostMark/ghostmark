package es.unizar.ghostmark.backend.cuenta.controllers;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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
        ProblemDetail problema = error.getBody();
        if (error.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()) {
            problema.setProperty("codigo", "NO_AUTENTICADO");
        }
        return ResponseEntity.status(error.getStatusCode())
            .body(problema);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail cuerpoNoValido(HttpMessageNotReadableException error) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Algún campo no cumple sus reglas");
        problema.setTitle("Bad Request");
        problema.setProperty("codigo", "DATOS_NO_VALIDOS");
        problema.setProperty("errores",
                List.of(Map.of("campo", "cuerpo", "mensaje", "El cuerpo no es un JSON válido")));
        return problema;
    }
}

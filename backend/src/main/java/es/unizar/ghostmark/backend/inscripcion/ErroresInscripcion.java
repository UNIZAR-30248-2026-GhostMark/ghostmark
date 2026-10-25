package es.unizar.ghostmark.backend.inscripcion;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ErroresInscripcion {

    @ExceptionHandler(ErrorInscripcion.class)
    ProblemDetail inscripcion(ErrorInscripcion error) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                error.estado(), error.getMessage());
        problema.setProperty("codigo", error.codigo());
        return problema;
    }
}

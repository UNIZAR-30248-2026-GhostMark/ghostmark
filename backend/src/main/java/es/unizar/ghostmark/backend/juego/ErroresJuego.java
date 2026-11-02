package es.unizar.ghostmark.backend.juego;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ErroresJuego {

    @ExceptionHandler(ErrorJuego.class)
    ProblemDetail juego(ErrorJuego error) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                error.estado(), error.getMessage());
        problema.setProperty("codigo", error.codigo());
        error.propiedades().forEach(problema::setProperty);
        return problema;
    }
}

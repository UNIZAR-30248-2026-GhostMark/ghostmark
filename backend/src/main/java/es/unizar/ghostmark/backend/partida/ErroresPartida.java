package es.unizar.ghostmark.backend.partida;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ErroresPartida {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail cuerpoNoValido(HttpMessageNotReadableException error) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Algún campo no cumple sus reglas");
        problema.setTitle("Bad Request");
        problema.setProperty("codigo", "DATOS_NO_VALIDOS");
        problema.setProperty("errores",
                List.of(Map.of("campo", "cuerpo", "mensaje", "El cuerpo no es un JSON válido")));
        return problema;
    }
}

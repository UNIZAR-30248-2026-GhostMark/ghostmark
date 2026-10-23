package es.unizar.ghostmark.backend.sesion;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ErroresSesion {

    @ExceptionHandler(DatosNoValidosException.class)
    ProblemDetail datosNoValidos(DatosNoValidosException error) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Algún campo no cumple sus reglas");
        problema.setTitle("Bad Request");
        problema.setProperty("codigo", "DATOS_NO_VALIDOS");
        problema.setProperty(
                "errores",
                error.errores().entrySet().stream()
                        .map(e -> Map.of("campo", e.getKey(), "mensaje", e.getValue()))
                        .toList());
        return problema;
    }

    @ExceptionHandler(CuentaExisteException.class)
    ProblemDetail cuentaExiste(CuentaExisteException error) {
        ProblemDetail problema =
                ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, error.getMessage());
        problema.setTitle("Conflict");
        problema.setProperty("codigo", error.codigo());
        return problema;
    }

    @ExceptionHandler(CredencialesException.class)
    ProblemDetail credenciales(CredencialesException error) {
        ProblemDetail problema =
                ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, error.getMessage());
        problema.setTitle("Unauthorized");
        problema.setProperty("codigo", "CREDENCIALES_INCORRECTAS");
        return problema;
    }

    @ExceptionHandler(TokenException.class)
    ProblemDetail token(TokenException error) {
        ProblemDetail problema =
                ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, error.getMessage());
        problema.setTitle("Unauthorized");
        problema.setProperty("codigo", "TOKEN_NO_VALIDO");
        return problema;
    }
}

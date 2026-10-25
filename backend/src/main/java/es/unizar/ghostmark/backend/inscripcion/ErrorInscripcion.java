package es.unizar.ghostmark.backend.inscripcion;

import org.springframework.http.HttpStatus;

public class ErrorInscripcion extends RuntimeException {

    private final HttpStatus estado;
    private final String codigo;

    public ErrorInscripcion(HttpStatus estado, String codigo, String mensaje) {
        super(mensaje);
        this.estado = estado;
        this.codigo = codigo;
    }

    public HttpStatus estado() {
        return estado;
    }

    public String codigo() {
        return codigo;
    }
}

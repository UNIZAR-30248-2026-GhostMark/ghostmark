package es.unizar.ghostmark.backend.juego;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class ErrorJuego extends RuntimeException {

    private final HttpStatus estado;
    private final String codigo;
    private final Map<String, Object> propiedades;

    public ErrorJuego(HttpStatus estado, String codigo, String mensaje) {
        this(estado, codigo, mensaje, Map.of());
    }

    public ErrorJuego(HttpStatus estado, String codigo, String mensaje,
            Map<String, Object> propiedades) {
        super(mensaje);
        this.estado = estado;
        this.codigo = codigo;
        this.propiedades = propiedades;
    }

    public HttpStatus estado() {
        return estado;
    }

    public String codigo() {
        return codigo;
    }

    public Map<String, Object> propiedades() {
        return propiedades;
    }
}

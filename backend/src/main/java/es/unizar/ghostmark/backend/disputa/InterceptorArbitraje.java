package es.unizar.ghostmark.backend.disputa;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Comprueba los relevos pendientes antes de atender peticiones de una partida. */
@Component
public class InterceptorArbitraje implements HandlerInterceptor {

    private static final String PREFIJO = "/api/v1/partidas/";

    private final ServicioArbitraje arbitraje;
    private final Clock reloj;

    public InterceptorArbitraje(ServicioArbitraje arbitraje, Clock reloj) {
        this.arbitraje = arbitraje;
        this.reloj = reloj;
    }

    @Override
    public boolean preHandle(HttpServletRequest peticion, HttpServletResponse respuesta,
            Object controlador) {
        String ruta = peticion.getRequestURI();
        if (!ruta.startsWith(PREFIJO)) {
            return true;
        }
        String resto = ruta.substring(PREFIJO.length());
        String id = resto.split("/", 2)[0];
        UUID partidaId;
        try {
            partidaId = UUID.fromString(id);
        } catch (IllegalArgumentException invalido) {
            return true;
        }
        arbitraje.revisarPartida(partidaId, Instant.now(reloj));
        return true;
    }
}

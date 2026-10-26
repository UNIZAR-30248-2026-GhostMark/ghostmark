package es.unizar.ghostmark.backend.juego;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class InterceptorArranquePendiente implements HandlerInterceptor {

    private final TareaArranque tarea;
    private final Clock reloj;

    public InterceptorArranquePendiente(TareaArranque tarea, Clock reloj) {
        this.tarea = tarea;
        this.reloj = reloj;
    }

    @Override
    public boolean preHandle(HttpServletRequest peticion, HttpServletResponse respuesta,
            Object manejador) {
        tarea.revisar(Instant.now(reloj));
        return true;
    }
}

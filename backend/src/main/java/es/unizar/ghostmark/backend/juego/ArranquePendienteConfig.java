package es.unizar.ghostmark.backend.juego;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ArranquePendienteConfig implements WebMvcConfigurer {

    private final InterceptorArranquePendiente arranque;
    private final InterceptorCierrePendiente cierre;

    public ArranquePendienteConfig(InterceptorArranquePendiente arranque,
            InterceptorCierrePendiente cierre) {
        this.arranque = arranque;
        this.cierre = cierre;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registro) {
        registro.addInterceptor(arranque).addPathPatterns("/api/v1/partidas/**",
                "/api/v1/inscripciones", "/api/v1/cuentas/yo/partidas");
        registro.addInterceptor(cierre).addPathPatterns("/api/v1/partidas/**",
                "/api/v1/inscripciones", "/api/v1/cuentas/yo/partidas");
    }
}

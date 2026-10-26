package es.unizar.ghostmark.backend.juego;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ArranquePendienteConfig implements WebMvcConfigurer {

    private final InterceptorArranquePendiente interceptor;

    public ArranquePendienteConfig(InterceptorArranquePendiente interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registro) {
        registro.addInterceptor(interceptor).addPathPatterns("/api/v1/partidas/**",
                "/api/v1/inscripciones", "/api/v1/cuentas/yo/partidas");
    }
}

package es.unizar.ghostmark.backend.disputa;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ConfigArbitraje implements WebMvcConfigurer {

    private final InterceptorArbitraje arbitraje;

    public ConfigArbitraje(InterceptorArbitraje arbitraje) {
        this.arbitraje = arbitraje;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registro) {
        registro.addInterceptor(arbitraje).addPathPatterns("/api/v1/partidas/**");
    }
}

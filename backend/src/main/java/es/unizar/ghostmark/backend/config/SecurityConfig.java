package es.unizar.ghostmark.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableScheduling
public class SecurityConfig {

    private static final String NO_AUTENTICADO = "{\"type\":\"about:blank\","
            + "\"title\":\"Unauthorized\",\"status\":401,"
            + "\"detail\":\"Falta el token de acceso o no vale\","
            + "\"codigo\":\"NO_AUTENTICADO\"}";

    private final FiltroAcceso filtroAcceso;

    public SecurityConfig(FiltroAcceso filtroAcceso) {
        this.filtroAcceso = filtroAcceso;
    }

    @Bean
    SecurityFilterChain filter(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
                .authorizeHttpRequests(a -> a
                        // /error abierto: si no, un fallo del servidor llega a la app como 401.
                        .requestMatchers("/api/v1/health", "/v3/api-docs/**",
                                "/swagger-ui/**", "/swagger-ui.html", "/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/cuentas", "/api/v1/sesiones",
                                "/api/v1/sesiones/refresco").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(this::noAutenticado))
                .addFilterBefore(filtroAcceso, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void noAutenticado(HttpServletRequest peticion, HttpServletResponse respuesta,
            AuthenticationException error) throws IOException {
        respuesta.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        respuesta.setContentType("application/problem+json");
        respuesta.setCharacterEncoding("UTF-8");
        respuesta.getWriter().write(NO_AUTENTICADO);
    }
}
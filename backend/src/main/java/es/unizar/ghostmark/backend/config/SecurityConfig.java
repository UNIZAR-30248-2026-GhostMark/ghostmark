package es.unizar.ghostmark.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableScheduling
public class SecurityConfig {

    private final FiltroAcceso filtroAcceso;

    public SecurityConfig(FiltroAcceso filtroAcceso) {
        this.filtroAcceso = filtroAcceso;
    }

    @Bean
    SecurityFilterChain filter(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/v1/health", "/v3/api-docs/**",
                                "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/cuentas", "/api/v1/sesiones",
                                "/api/v1/sesiones/refresco").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(filtroAcceso, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
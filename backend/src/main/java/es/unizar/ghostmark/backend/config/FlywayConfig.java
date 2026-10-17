package es.unizar.ghostmark.backend.config;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway manual: Spring Boot 4 ya no trae su autoconfiguracion.
 * Las migraciones versionadas de classpath:db/migration siguen mandando
 * (convenciones-modelo-datos.md) y se aplican antes de que JPA valide.
 */
@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    Flyway flyway(DataSource dataSource) {
        return Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load();
    }

    @Bean
    static BeanFactoryPostProcessor flywayDependsOnPostProcessor() {
        return beanFactory -> {
            if (beanFactory.containsBeanDefinition("entityManagerFactory")) {
                beanFactory.getBeanDefinition("entityManagerFactory").setDependsOn("flyway");
            }
        };
    }
}

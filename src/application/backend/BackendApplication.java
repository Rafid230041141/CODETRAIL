package application.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Entry point for the HTTP backend and the JavaFX dependency-injection context. */
@SpringBootApplication(scanBasePackages = "application")
@EntityScan(basePackages = "application.backend.domain")
@EnableJpaRepositories(basePackages = "application.backend.repository")
public class BackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}

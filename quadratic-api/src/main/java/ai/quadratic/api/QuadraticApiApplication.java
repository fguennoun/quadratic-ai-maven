package ai.quadratic.api;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication
@EnableScheduling
public class QuadraticApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(QuadraticApiApplication.class, args);
    }
}
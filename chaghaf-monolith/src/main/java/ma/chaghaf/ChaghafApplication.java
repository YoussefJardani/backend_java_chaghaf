package ma.chaghaf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChaghafApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChaghafApplication.class, args);
    }
}

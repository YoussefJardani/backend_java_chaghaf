package ma.chaghaf.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.chaghaf.auth.entity.User;
import ma.chaghaf.auth.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // ── Default ADMIN ─────────────────────────────────────────
        if (!userRepo.existsByEmail("admin@chaghaf.ma")) {
            User admin = User.builder()
                .fullName("Administrateur Chaghaf")
                .email("admin@chaghaf.ma")
                .password(passwordEncoder.encode("admin123"))
                .phone("+212600000001")
                .role(User.Role.ADMIN)
                .active(true)
                .build();
            userRepo.save(admin);
            log.info("✅ Default ADMIN created: admin@chaghaf.ma / admin123");
        } else {
            log.info("ℹ️  Default admin already exists");
        }

        // ── Default USER ─────────────────────────────────────────
        if (!userRepo.existsByEmail("user@chaghaf.ma")) {
            User user = User.builder()
                .fullName("Utilisateur Test")
                .email("user@chaghaf.ma")
                .password(passwordEncoder.encode("user123"))
                .phone("+212600000002")
                .role(User.Role.USER)
                .active(true)
                .build();
            userRepo.save(user);
            log.info("✅ Default USER created: user@chaghaf.ma / user123");
        } else {
            log.info("ℹ️  Default user already exists");
        }
    }
}
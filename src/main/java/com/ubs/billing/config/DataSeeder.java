package com.ubs.billing.config;

import com.ubs.billing.entity.Role;
import com.ubs.billing.entity.User;
import com.ubs.billing.entity.UserRole;
import com.ubs.billing.repository.RoleRepository;
import com.ubs.billing.repository.UserRepository;
import com.ubs.billing.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Map<String, String> ROLE_DESCRIPTIONS = Map.of(
            Constants.ROLE_ADMIN, "System administrator with full access",
            Constants.ROLE_OPERATOR, "Utility operations staff",
            Constants.ROLE_FINANCE, "Finance and billing staff",
            Constants.ROLE_CUSTOMER, "End customer with self-service access"
    );

    @Override
    @Transactional
    public void run(String... args) {
        seedRoles();
        seedDefaultAdmin();
    }

    private void seedRoles() {
        for (String roleName : Constants.DEFAULT_ROLES) {
            if (!roleRepository.existsByName(roleName)) {
                Role role = Role.builder()
                        .name(roleName)
                        .description(ROLE_DESCRIPTIONS.get(roleName))
                        .build();
                roleRepository.save(role);
                log.info("Seeded role: {}", roleName);
            }
        }
    }

    private void seedDefaultAdmin() {
        String adminEmail = "brillanteigabemurangwa@gmail.com";
        String adminUsername = "brillanteigabemurangwa";
        String adminPassword = "Password123!";

        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        userRepository.findByEmail("admin@ubs.example.com")
                .or(() -> userRepository.findByUsername("admin"))
                .ifPresentOrElse(
                        existing -> migrateLegacyAdmin(existing, adminEmail, adminUsername, adminPassword),
                        () -> createDefaultAdmin(adminEmail, adminUsername, adminPassword));
    }

    private void migrateLegacyAdmin(User existing, String adminEmail, String adminUsername, String adminPassword) {
        existing.setEmail(adminEmail);
        existing.setUsername(adminUsername);
        existing.setPassword(passwordEncoder.encode(adminPassword));
        existing.setFullName("Brillante Igabe Murangwa");
        existing.setFirstName("Brillante");
        existing.setLastName("Murangwa");
        existing.setEmailVerified(true);
        existing.setEnabled(true);
        existing.setFirstLogin(false);
        userRepository.save(existing);
        log.info("Migrated legacy admin user to: {}", adminEmail);
    }

    private void createDefaultAdmin(String adminEmail, String adminUsername, String adminPassword) {
        Role adminRole = roleRepository.findByName(Constants.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN must exist before seeding admin user"));

        User admin = User.builder()
                .username(adminUsername)
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .fullName("Brillante Igabe Murangwa")
                .phoneNumber("+250780000000")
                .firstName("Brillante")
                .lastName("Murangwa")
                .emailVerified(true)
                .enabled(true)
                .firstLogin(false)
                .build();

        UserRole userRole = UserRole.builder()
                .user(admin)
                .role(adminRole)
                .build();
        admin.addUserRole(userRole);

        userRepository.save(admin);
        log.info("Seeded default admin user: {}", adminEmail);
    }
}

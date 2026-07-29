package com.infobeans.ibnextstep.config;

import com.infobeans.ibnextstep.user.Role;
import com.infobeans.ibnextstep.user.User;
import com.infobeans.ibnextstep.user.UserRepository;
import com.infobeans.ibnextstep.user.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Bootstraps the very first ADMIN account on a fresh database.
 *
 * There is no public "/api/auth/register" endpoint by design — accounts are
 * created BY an admin, not self-registered (matches the activity diagram).
 * That means on an empty database nobody can ever log in, so nobody can
 * ever create the first user. This runner breaks that chicken-and-egg
 * problem: if the "users" collection is empty when the app starts, it
 * creates one default ADMIN account and prints the credentials to the
 * console so you can log in, get a JWT, and take it from there.
 *
 * Safe to leave in for dev/testing: it only ever runs when the collection
 * is completely empty, so it will never touch an existing database.
 */

@Component
@RequiredArgsConstructor
@Slf4j

public class DataSeeder implements CommandLineRunner {

    private static final String DEFAULT_ADMIN_EMAIL = "indalkarrohan466@gmail.com";
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin@12345";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        User admin = User.builder()
                .firstName("System")
                .lastName("Admin")
                .email(DEFAULT_ADMIN_EMAIL)
                .password(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD))
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .mustChangePassword(true)
                .createdAt(Instant.now())
                .build();

        userRepository.save(admin);

        log.warn("=================================================================");
        log.warn(" No users found — a default ADMIN account has been created.");
        log.warn(" Email:    {}", DEFAULT_ADMIN_EMAIL);
        log.warn(" Password: {}", DEFAULT_ADMIN_PASSWORD);
        log.warn(" Log in with this via POST /api/auth/login, verify the OTP via");
        log.warn(" POST /api/auth/verify-otp (check console for the OTP — see the");
        log.warn(" mail warning below if spring.mail is not configured yet), then");
        log.warn(" use the returned JWT in Swagger's Authorize button. Change this");
        log.warn(" password immediately after first login.");
        log.warn("=================================================================");
    }
    
}
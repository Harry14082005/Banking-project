package com.hethongtrongbanking.nienluancosonganh.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.hethongtrongbanking.nienluancosonganh.entity.User;
import com.hethongtrongbanking.nienluancosonganh.enums.Role;
import com.hethongtrongbanking.nienluancosonganh.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * DataSeeder - Tao san user demo khi khoi dong lan dau
 * Chi tao neu chua ton tai (idempotent)
 * 
 * Tai khoan test (tat ca mat khau: 123456):
 * | Username       | Role    | Muc dich                              |
 * |----------------|---------|---------------------------------------|
 * | demo_customer  | VIEWER  | KH demo, CardInfo se gan vao user nay |
 * | admin          | ADMIN   | Quan ly he thong                      |
 * | analyst01      | ANALYST | Nhan vien duyet FraudCase             |
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUser("demo_customer", "123456", "demo@banking.local", "0900000001", Role.VIEWER);
        seedUser("admin", "123456", "admin@banking.local", "0900000002", Role.ADMIN);
        seedUser("analyst01", "123456", "analyst01@banking.local", "0900000003", Role.ANALYST);

        log.info("=== Data seeding hoan tat ===");
    }

    private void seedUser(String username, String rawPassword, String email, String phone, Role role) {
        if (userRepository.findByUsername(username).isPresent()) {
            log.debug("User '{}' da ton tai, bo qua.", username);
            return;
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .email(email)
                .phone(phone)
                .role(role)
                .isActive(true)
                .build();

        userRepository.save(user);
        log.info("Tao user: {} (role={})", username, role);
    }
}

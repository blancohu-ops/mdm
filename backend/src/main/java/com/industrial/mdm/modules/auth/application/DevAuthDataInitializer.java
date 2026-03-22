package com.industrial.mdm.modules.auth.application;

import com.industrial.mdm.common.security.UserRole;
import com.industrial.mdm.modules.auth.domain.AccountStatus;
import com.industrial.mdm.modules.auth.repository.UserEntity;
import com.industrial.mdm.modules.auth.repository.UserRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("dev")
public class DevAuthDataInitializer {

    @Bean
    ApplicationRunner seedAdminUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            seedUser(
                    userRepository,
                    passwordEncoder,
                    "admin@example.com",
                    "13800000001",
                    "admin@example.com",
                    "平台运营管理员",
                    "平台运营中心",
                    UserRole.OPERATIONS_ADMIN);
            seedUser(
                    userRepository,
                    passwordEncoder,
                    "reviewer@example.com",
                    "13800000002",
                    "reviewer@example.com",
                    "平台审核员",
                    "平台审核中心",
                    UserRole.REVIEWER);
        };
    }

    private void seedUser(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            String account,
            String phone,
            String email,
            String displayName,
            String organization,
            UserRole role) {
        if (userRepository.existsByAccountIgnoreCase(account)) {
            return;
        }
        UserEntity user = new UserEntity();
        user.setAccount(account);
        user.setPhone(phone);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Admin1234"));
        user.setRole(role);
        user.setStatus(AccountStatus.ACTIVE);
        user.setDisplayName(displayName);
        user.setOrganization(organization);
        userRepository.save(user);
    }
}

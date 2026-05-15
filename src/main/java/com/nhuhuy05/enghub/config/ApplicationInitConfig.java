package com.nhuhuy05.enghub.config;

import com.nhuhuy05.enghub.user.entity.Role;
import com.nhuhuy05.enghub.user.entity.User;
import com.nhuhuy05.enghub.common.enums.SystemRole;
import com.nhuhuy05.enghub.user.repository.RoleRepository;
import com.nhuhuy05.enghub.user.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ApplicationInitConfig {

    PasswordEncoder passwordEncoder;
    RoleRepository roleRepository;

    @Bean
    ApplicationRunner applicationRunner(UserRepository userRepository){
        return args -> {
            if (userRepository.findByEmail("admin@gmail.com").isEmpty()){
                Role adminRole = resolveOrCreateRole(SystemRole.ADMIN);

                User user = User.builder()
                        .email("admin@gmail.com")
                        .fullName("Administrator")
                        .password(passwordEncoder.encode("admin"))
                        .roles(Set.of(adminRole))
                        .build();

                userRepository.save(user);
                log.warn("admin user has been created with default password: admin, please change it");
            }
        };
    }

    private Role resolveOrCreateRole(SystemRole roleName) {
        return roleRepository.findByName(roleName.name())
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .name(roleName.name())
                                .description(roleName.name())
                                .build()
                ));
    }
}




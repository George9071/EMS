package com._6.ems.configuration;

import com._6.ems.entity.Account;
import com._6.ems.enums.Role;
import com._6.ems.repository.AccountRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ApplicationInitConfig {

    PasswordEncoder passwordEncoder;

    @Bean
    @ConditionalOnProperty(
            prefix = "spring",
            value = "datasource.driverClassName",
            havingValue = "com.mysql.cj.jdbc.Driver"
    )
    ApplicationRunner applicationRunner(AccountRepository accountRepository){
        return args -> {
            if (accountRepository.findByUsername("admin").isEmpty()){
                Account account = Account.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin"))
                        .role(Role.ADMIN)
                        .build();

                accountRepository.save(account);
                log.warn("admin account has been created with default password: admin, please change it");
            }
        };
    }
}

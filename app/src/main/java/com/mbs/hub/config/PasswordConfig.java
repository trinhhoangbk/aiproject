package com.mbs.hub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * BCrypt-12 encoder (05 Security §4.1). The full SecurityFilterChain lives in
 * {@code com.mbs.hub.security} and is wired in M8; this encoder is already
 * injectable so M1 roster creation can hash passwords.
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}

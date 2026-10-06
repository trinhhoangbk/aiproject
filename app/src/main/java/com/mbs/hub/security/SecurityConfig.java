package com.mbs.hub.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * PLAN-028 Spring Security filter chain + PLAN-029 method-level RBAC.
 *
 * <p>Rules (05 Security §4/§5):</p>
 * <ul>
 *   <li>{@code /webhooks/jira} — permitAll, HMAC-only (CSRF disabled on this path).</li>
 *   <li>{@code /actuator/health, /actuator/info} — permitAll.</li>
 *   <li>{@code /actuator/prometheus, /actuator/metrics} — ADMIN only.</li>
 *   <li>{@code /api/auth/login, /api/auth/me, /api/auth/logout} — accessible to the login flow.</li>
 *   <li>Everything else — authenticated; per-method {@code @PreAuthorize} refines.</li>
 * </ul>
 *
 * <p>MFA, login-lockout (SV-06) and CAPTCHA are explicit v1.0 deferrals (05 Security
 * §12 SEC-R-03). Credential stuffing on a dev-host tunnel is tolerable because
 * TD-COND-01 forbids exposing anything but {@code /webhooks/jira} publicly.</p>
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            // CSRF — enabled everywhere EXCEPT the webhook endpoint (external, HMAC-protected)
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(new AntPathRequestMatcher("/webhooks/jira"))
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            )
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/webhooks/jira").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/actuator/prometheus", "/actuator/metrics/**").hasRole("ADMIN")
                .requestMatchers("/api/auth/login", "/api/auth/me", "/api/auth/logout").permitAll()
                // SPA static assets
                .requestMatchers("/", "/index.html", "/static/**", "/assets/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(login -> login
                .loginProcessingUrl("/api/auth/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler((req, res, a) -> res.setStatus(200))
                .failureHandler((req, res, e) -> res.setStatus(401))
            )
            .logout(out -> out
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((req, res, a) -> res.setStatus(204))
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            )
            // Spring Security 6.x defaults: HttpOnly cookie; set Secure dynamically at the server
            // via server.servlet.session.cookie.secure=true when behind TLS (set in prod profile).
            .build();
    }
}

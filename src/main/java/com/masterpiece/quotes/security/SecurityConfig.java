package com.masterpiece.quotes.security;

import com.masterpiece.quotes.config.JsonErrors;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

// One owner, one password (APP_PASSWORD), sent with every request as HTTP Basic over HTTPS.
// No sessions or cookies, so there is nothing for CSRF to abuse.
@Configuration
public class SecurityConfig {

    static final int MIN_PASSWORD_LENGTH = 12;

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService users(@Value("${app.auth.username}") String username,
                             @Value("${app.auth.password}") String password,
                             PasswordEncoder encoder) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            // Fail closed: without a proper password the API must not start, rather than start open.
            throw new IllegalStateException("APP_PASSWORD must be set to at least " + MIN_PASSWORD_LENGTH
                    + " characters. Set it in the Render environment variables.");
        }
        return new InMemoryUserDetailsManager(
                User.withUsername(username).password(encoder.encode(password)).roles("OWNER").build());
    }

    @Bean
    AttemptLimiter loginLimiter(@Value("${app.auth.max-failures}") int maxFailures,
                                @Value("${app.auth.lockout-minutes}") int lockoutMinutes) {
        return new AttemptLimiter(maxFailures, Duration.ofMinutes(lockoutMinutes), Clock.systemUTC());
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, AttemptLimiter loginLimiter) throws Exception {
        AttemptLimiter clientErrorLimiter = new AttemptLimiter(30, Duration.ofMinutes(1), Clock.systemUTC());

        // Rejects with 401 but WITHOUT "WWW-Authenticate: Basic", so browsers never show their own
        // login popup - the app shows its own login screen. Failed passwords count toward lockout.
        AuthenticationEntryPoint entryPoint = (request, response, ex) -> {
            if (request.getHeader("Authorization") != null) {
                loginLimiter.record(request.getRemoteAddr());
            }
            JsonErrors.write(response, HttpServletResponse.SC_UNAUTHORIZED, "Password required or incorrect");
        };

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(b -> b.authenticationEntryPoint(entryPoint))
                .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/health", "/api/health/ready").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new LockoutFilter(loginLimiter, clientErrorLimiter), BasicAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") String origins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).filter(o -> !o.isEmpty()).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Location"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    // Blocks an IP after too many wrong passwords (brute force), and caps client error reports.
    static final class LockoutFilter extends OncePerRequestFilter {
        private final AttemptLimiter loginLimiter;
        private final AttemptLimiter clientErrorLimiter;

        LockoutFilter(AttemptLimiter loginLimiter, AttemptLimiter clientErrorLimiter) {
            this.loginLimiter = loginLimiter;
            this.clientErrorLimiter = clientErrorLimiter;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            String ip = request.getRemoteAddr();
            if (request.getHeader("Authorization") != null && loginLimiter.isBlocked(ip)) {
                JsonErrors.write(response, 429, "Too many wrong passwords. Try again in a few minutes.");
                return;
            }
            if ("/api/client-errors".equals(request.getRequestURI())) {
                if (clientErrorLimiter.isBlocked(ip)) {
                    response.setStatus(429);
                    return;
                }
                clientErrorLimiter.record(ip);
            }
            chain.doFilter(request, response);
        }
    }
}

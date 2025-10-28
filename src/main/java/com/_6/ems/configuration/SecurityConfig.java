package com._6.ems.configuration;

import com._6.ems.converter.CustomJwtGrantedAuthoritiesConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.http.HttpMethod;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import lombok.RequiredArgsConstructor;


/**
 * Main Spring Security configuration class.

 * This configuration secures the application using JWT-based authentication
 * for API endpoints, integrates a custom JWT decoder, and defines public
 * (unauthenticated) routes.
 * It also enables method-level security annotations like @PreAuthorize and @Secured.</p>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final String[] PUBLIC_ENDPOINTS = {
            "/auth/**",
    };

    private final CustomJwtDecoder customJwtDecoder;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .cors(Customizer.withDefaults()) // Enable CORS using default configuration
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF since this is a stateless REST API

                // Define authorization rules for incoming HTTP requests
                .authorizeHttpRequests(request ->
                    request
                            // Allow OPTIONS requests for preflight CORS checks
                            .requestMatchers(HttpMethod.OPTIONS, PUBLIC_ENDPOINTS).permitAll()

                            // Allow POST requests for authentication-related endpoints
                            .requestMatchers(HttpMethod.POST, PUBLIC_ENDPOINTS).permitAll()

                            // Allow unauthenticated access to certain public routes
                            .requestMatchers("/api/auth/**").permitAll()
                            .requestMatchers("/swagger-ui/**").permitAll()
                            .requestMatchers("/v3/api-docs/**").permitAll()
                            .requestMatchers("/api-docs/**").permitAll()
                            .requestMatchers("/swagger-ui.html").permitAll()
                            .requestMatchers("/actuator/health").permitAll()

                            // All other endpoints require authentication
                            .anyRequest().authenticated()
                )

                // Configure JWT-based OAuth2 Resource Server authentication
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(customJwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter()))

                        // Define custom entry point for unauthorized access handling
                        .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
        );
        return httpSecurity.build();
    }

    /**
     * Configures the {@link JwtAuthenticationConverter} to transform JWT claims
     * into {@link org.springframework.security.core.GrantedAuthority} objects.
     *
     * @return a configured {@link JwtAuthenticationConverter} bean
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        // Use a custom converter to extract roles/authorities from JWT claims
        converter.setJwtGrantedAuthoritiesConverter(new CustomJwtGrantedAuthoritiesConverter());

        return converter;
    }

    @Bean
    PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder(10);
    }
}

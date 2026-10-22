package com.uvindu.crane_management_system.config;

import com.uvindu.crane_management_system.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.DispatcherType;
import java.rmi.server.ExportException;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtauthenticationFilter, UserDetailsService userDetailsService) {
        this.jwtAuthenticationFilter = jwtauthenticationFilter;
        this.userDetailsService = userDetailsService;
    }


    // Configure Spring Security to allow unauthenticated access to the register endpoint
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws ExportException {

        http
                // Disable CSRF because this is REST API
                .csrf(AbstractHttpConfigurer::disable)

                .exceptionHandling(exception -> exception

                        // User is not authenticated -> 401
                        .authenticationEntryPoint((request, response, authException) ->
                                response.setStatus(
                                        HttpStatus.UNAUTHORIZED.value()
                                ))

                        // User is authenticated but lacks permission -> 403
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.setStatus(
                                        HttpStatus.FORBIDDEN.value()
                                ))
                )

                // JWT authentication is stateless
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth

                        // Allow internal servlet error dispatches to preserve
                        // the original HTTP error status
                        .dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()

                        // Login endpoint
                        .requestMatchers("/api/auth/login")
                        .permitAll()

                        // Admin APIs
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")


                        // Everything else requires JWT
                        .anyRequest()
                        .authenticated()
                )

                // Tell Spring how to authenticate users
                .authenticationProvider(authenticationProvider())

                // Run JWT filter before username/password filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Create Password Encoder Bean
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }



    // Create Authentication Provider
    @Bean
    public AuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }


    // Create Authentication Manager Bean
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {

        return config.getAuthenticationManager();

    }

}

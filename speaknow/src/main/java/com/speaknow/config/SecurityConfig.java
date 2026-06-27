package com.speaknow.config;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.speaknow.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring()
            .requestMatchers("/api/auth/**")
            .requestMatchers("/assets/**")
            .requestMatchers("/favicon.ico")
            .requestMatchers("/*.html")
            .requestMatchers("/*.css")
            .requestMatchers("/*.js")
            .requestMatchers("/images/**");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()  // Public endpoints
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/users/discover").permitAll()  // User discovery endpoint
                .requestMatchers("/api/users/online").permitAll()  // Online users endpoint
                .requestMatchers("/api/users/{id}").permitAll()  // User profile endpoint
                .requestMatchers("/api/chat").permitAll()  // Public AI chat endpoint
                .requestMatchers("/api/grammar-check").permitAll()
                .requestMatchers("/api/alternative").permitAll()
                .requestMatchers("/api/translate").permitAll()
                .requestMatchers("/api/session/save").permitAll()
                .requestMatchers("/api/hint").permitAll()
                .requestMatchers("/api/interview/**").permitAll()
                .requestMatchers("/api/dictionary/**").permitAll()
                .requestMatchers("/api/user/**").permitAll()
                .requestMatchers("/assets/**").permitAll()
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/login.html",
                    "/register.html",
                    "/dashboard.html",
                    "/chat-user.html",
                    "/chat-list.html",
                    "/chat.html",
                    "/session.html",
                    "/dictionary.html",
                    "/interview.html",
                    "/voice-chat.html"
                ).permitAll()
                .requestMatchers("/ws/**").permitAll()  // WebSocket untuk chat
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

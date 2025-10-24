// backend/config/SecurityConfig.java
package com.homeride.backend.config;

import com.homeride.backend.filter.JwtAuthenticationFilter;
import com.homeride.backend.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final EmployeeService employeeService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter, EmployeeService employeeService, PasswordEncoder passwordEncoder) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.employeeService = employeeService;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(withDefaults())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/contact/**").permitAll()   // 👈 add this line

                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/api/places/**").permitAll()
                        .requestMatchers("/api/maps/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/api/rides/travel-info").permitAll()
                        .requestMatchers("/api/rides/calculate-price").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/employees/{id}").permitAll()

                        // Authenticated endpoints
                        .requestMatchers(HttpMethod.GET, "/api/rides").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/rides/my-rides").authenticated()

                        // FIX: Explicitly allow authenticated users to offer a ride
                        .requestMatchers(HttpMethod.POST, "/api/rides/offer").authenticated()

                        .requestMatchers(HttpMethod.DELETE, "/api/rides/{rideId}").authenticated()
                        .requestMatchers("/api/rides/**").authenticated()
                        .requestMatchers("/api/ratings/**").authenticated()
                        //.requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/employees/me/profile-picture").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/employees/me/profile-picture").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/employees/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/chat/history/**").authenticated()
                        .requestMatchers("/api/chatbot/**").authenticated()

                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(employeeService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
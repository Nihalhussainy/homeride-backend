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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
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
                        .requestMatchers("/api/auth/public/stats").permitAll()
                        .requestMatchers("/api/contact/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/employees/{id}").permitAll()
                        .requestMatchers("/api/locations/cities").permitAll()
                        .requestMatchers("/api/places/autocomplete").permitAll()
                        .requestMatchers("/api/places/details").permitAll()
                        .requestMatchers("/api/proxy/directions").permitAll()
                        .requestMatchers("/uploads/**").permitAll() // Allow access to uploaded files
                        .requestMatchers("/api/test/public").permitAll()
                        // *** ADDED THIS LINE TO FIX 403 ERROR ***
                        .requestMatchers(HttpMethod.GET, "/api/rides/travel-info").permitAll()

                        // Authenticated endpoints
                        .requestMatchers(HttpMethod.GET, "/api/rides").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/rides").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/rides/my-rides").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/rides/{id}").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/rides/{id}/book").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/rides/{rideId}/status").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/employees/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/employees/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/employees/me/profile-picture").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/employees/me/profile-picture").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/employees/profile/{userId}").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/ratings").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/ratings/my-ratings").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/ratings/given").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/notifications").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/notifications/mark-read").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/chatbot").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/rides/cancel").authenticated()
                        .requestMatchers("/api/chat/**").authenticated()
                        .requestMatchers("/app/**").authenticated() // WebSocket endpoints
                        .requestMatchers("/topic/**").authenticated() // WebSocket endpoints
                        .requestMatchers("/user/**").authenticated() // WebSocket endpoints
                        .requestMatchers("/ws/**").permitAll()

                        // Admin endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/test/admin").hasRole("ADMIN")

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
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173","https://homeride-frontend.vercel.app"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
package com.homeride.backend.controller;

import com.homeride.backend.dto.LoginRequestDTO;
import com.homeride.backend.dto.LoginResponseDTO;
import com.homeride.backend.dto.RegisterRequestDTO;
import com.homeride.backend.model.Employee;
import com.homeride.backend.service.AnalyticsService; // <-- ADDED THIS IMPORT
import com.homeride.backend.service.EmployeeService;
import com.homeride.backend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map; // <-- ADDED THIS IMPORT

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final EmployeeService employeeService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final AnalyticsService analyticsService; // <-- ADDED THIS FIELD

    @Autowired
    // MODIFIED CONSTRUCTOR
    public AuthController(EmployeeService employeeService, JwtUtil jwtUtil, AuthenticationManager authenticationManager, AnalyticsService analyticsService) {
        this.employeeService = employeeService;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.analyticsService = analyticsService; // <-- ADDED THIS LINE
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerEmployee(@RequestBody RegisterRequestDTO registerRequest) {
        try {
            Employee registeredEmployee = employeeService.register(registerRequest);
            return ResponseEntity.ok("Employee registered successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: Email is already in use!");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginEmployee(@RequestBody LoginRequestDTO loginRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );
            final UserDetails userDetails = employeeService.loadUserByUsername(loginRequest.getEmail());
            final String token = jwtUtil.generateToken(userDetails);
            return ResponseEntity.ok(new LoginResponseDTO(token));
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Error: Invalid credentials");
        }
    }

    // --- NEW PUBLIC ENDPOINT ---
    @GetMapping("/public/stats")
    public ResponseEntity<Map<String, Long>> getPublicStats() {
        // This method is from AnalyticsService
        return ResponseEntity.ok(analyticsService.getBasicStats());
    }
    // --- END NEW ENDPOINT ---
}
package com.homeride.backend.controller;

import com.homeride.backend.dto.LoginRequestDTO;
import com.homeride.backend.dto.LoginResponseDTO;
import com.homeride.backend.dto.RegisterRequestDTO;
import com.homeride.backend.model.Employee;
import com.homeride.backend.service.EmployeeService;
import com.homeride.backend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final EmployeeService employeeService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public AuthController(EmployeeService employeeService, JwtUtil jwtUtil, AuthenticationManager authenticationManager) {
        this.employeeService = employeeService;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerEmployee(@RequestBody RegisterRequestDTO registerRequest) {
        try {
            Employee registeredEmployee = employeeService.registerEmployee(registerRequest);
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
}
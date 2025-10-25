package com.homeride.backend.service;

import com.homeride.backend.dto.AdminUserUpdateDTO;
import com.homeride.backend.dto.LoginRequestDTO;
import com.homeride.backend.dto.RegisterRequestDTO;
import com.homeride.backend.dto.UserProfileUpdateDTO; // NEW IMPORT
import com.homeride.backend.model.Employee;
import com.homeride.backend.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Collections;
import java.util.List;

@Service
public class EmployeeService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    @Autowired
    public EmployeeService(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder, FileStorageService fileStorageService) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileStorageService = fileStorageService;
    }

    public Employee findEmployeeByEmail(String email) {
        return employeeRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public Employee register(RegisterRequestDTO request) {
        // --- 1. Convert email to lowercase ---
        String lowercaseEmail = request.getEmail().toLowerCase();

        // --- 2. Check if the lowercase email already exists ---
        if (employeeRepository.existsByEmail(lowercaseEmail)) {
            throw new IllegalArgumentException("Email address is already registered.");
        }

        // --- 3. Validate password complexity (Example - adapt as needed) ---
        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long.");
        }
        // Add more password rules if desired (e.g., regex for uppercase, numbers, symbols)

        // --- 4. Proceed with creating the new Employee ---
        Employee employee = new Employee();
        employee.setName(request.getName());
        employee.setEmail(lowercaseEmail); // <-- Use the lowercase email
        employee.setPassword(passwordEncoder.encode(request.getPassword()));
        employee.setGender(request.getGender());
        employee.setRole("EMPLOYEE"); // Set your default role

        // Initialize other fields if necessary (e.g., travelCredit)
        employee.setTravelCredit(0.0); // Example initialization

        return employeeRepository.save(employee);
    }

    // NEW: Method to update user's own profile (name and phone)
    public Employee updateUserProfile(String email, UserProfileUpdateDTO updateDTO) {
        Employee employee = findEmployeeByEmail(email);

        if (updateDTO.getName() != null && !updateDTO.getName().isEmpty()) {
            employee.setName(updateDTO.getName());
        }
        if (updateDTO.getPhoneNumber() != null) {
            employee.setPhoneNumber(updateDTO.getPhoneNumber());
        }
        return employeeRepository.save(employee);
    }

    public Employee updateProfilePicture(String email, MultipartFile file) {
        Employee employee = findEmployeeByEmail(email);
        // FileStorageService already returns the complete Cloudinary URL
        String profilePictureUrl = fileStorageService.store(file);
        employee.setProfilePictureUrl(profilePictureUrl);
        return employeeRepository.save(employee);
    }

    public Employee removeProfilePicture(String email) {
        Employee employee = findEmployeeByEmail(email);
        employee.setProfilePictureUrl(null);
        return employeeRepository.save(employee);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        return new User(employee.getEmail(), employee.getPassword(), Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + employee.getRole())));
    }

    public Employee updateUserAsAdmin(Long userId, AdminUserUpdateDTO updateRequest) {
        Employee employee = employeeRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + userId));
        if (updateRequest.getRole() != null) {
            employee.setRole(updateRequest.getRole());
        }
        if (updateRequest.getTravelCredit() != null) {
            employee.setTravelCredit(updateRequest.getTravelCredit());
        }
        return employeeRepository.save(employee);
    }
    public Employee findEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
    }
}
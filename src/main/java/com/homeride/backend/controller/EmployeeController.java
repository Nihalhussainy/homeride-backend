package com.homeride.backend.controller;

import com.homeride.backend.dto.PublicProfileDTO; // NEW IMPORT
import com.homeride.backend.dto.UserProfileUpdateDTO;
import com.homeride.backend.model.Employee;
import com.homeride.backend.service.EmployeeService;
import com.homeride.backend.service.PublicProfileService; // NEW IMPORT
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final PublicProfileService publicProfileService; // NEW FIELD

    @Autowired
    public EmployeeController(EmployeeService employeeService, PublicProfileService publicProfileService) { // NEW CONSTRUCTOR PARAMETER
        this.employeeService = employeeService;
        this.publicProfileService = publicProfileService;
    }

    @GetMapping("/me")
    public ResponseEntity<Employee> getCurrentUser(Principal principal) {
        Employee employee = employeeService.findEmployeeByEmail(principal.getName());
        return ResponseEntity.ok(employee);
    }

    // NEW: Endpoint to handle profile data updates (name, phone)
    @PutMapping("/me")
    public ResponseEntity<Employee> updateUserProfile(@RequestBody UserProfileUpdateDTO updateDTO, Principal principal) {
        Employee updatedEmployee = employeeService.updateUserProfile(principal.getName(), updateDTO);
        return ResponseEntity.ok(updatedEmployee);
    }

    @PostMapping("/me/profile-picture")
    public ResponseEntity<?> uploadProfilePicture(@RequestParam("file") MultipartFile file, Principal principal) {
        try {
            Employee updatedEmployee = employeeService.updateProfilePicture(principal.getName(), file);
            return ResponseEntity.ok(updatedEmployee);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Could not upload the file: " + e.getMessage()));
        }
    }

    @DeleteMapping("/me/profile-picture")
    public ResponseEntity<Employee> removeProfilePicture(Principal principal) {
        Employee updatedEmployee = employeeService.removeProfilePicture(principal.getName());
        return ResponseEntity.ok(updatedEmployee);
    }

    // UPDATED: Public endpoint to get a user's public profile data
    @GetMapping("/{id}")
    public ResponseEntity<PublicProfileDTO> getPublicProfileById(@PathVariable Long id) {
        PublicProfileDTO profile = publicProfileService.getPublicProfile(id);
        return ResponseEntity.ok(profile);
    }
}
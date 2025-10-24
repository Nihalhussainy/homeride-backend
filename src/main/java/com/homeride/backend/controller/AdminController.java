package com.homeride.backend.controller;

import com.homeride.backend.dto.AdminEmployeeViewDTO;
import com.homeride.backend.dto.AdminUserUpdateDTO;
import com.homeride.backend.model.Employee;
import com.homeride.backend.service.AnalyticsService;
import com.homeride.backend.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final EmployeeService employeeService;
    private final AnalyticsService analyticsService;

    @Autowired
    public AdminController(EmployeeService employeeService, AnalyticsService analyticsService) {
        this.employeeService = employeeService;
        this.analyticsService = analyticsService;
    }

    @GetMapping("/employees")
    public ResponseEntity<List<AdminEmployeeViewDTO>> getAllEmployees() {
        List<AdminEmployeeViewDTO> employeesWithCounts = analyticsService.getAllEmployeesWithRideCounts();
        return ResponseEntity.ok(employeesWithCounts);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(analyticsService.getBasicStats());
    }

    @PutMapping("/employees/{userId}")
    public ResponseEntity<Employee> updateUser(@PathVariable Long userId, @RequestBody AdminUserUpdateDTO updateRequest) {
        Employee updatedEmployee = employeeService.updateUserAsAdmin(userId, updateRequest);
        return ResponseEntity.ok(updatedEmployee);
    }
}
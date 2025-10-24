package com.homeride.backend.service;

import com.homeride.backend.dto.AdminEmployeeViewDTO;
import com.homeride.backend.model.Employee;
import com.homeride.backend.repository.EmployeeRepository;
import com.homeride.backend.repository.RideParticipantRepository;
import com.homeride.backend.repository.RideRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final EmployeeRepository employeeRepository;
    private final RideRequestRepository rideRequestRepository;
    private final RideParticipantRepository rideParticipantRepository;

    @Autowired
    public AnalyticsService(EmployeeRepository employeeRepository, RideRequestRepository rideRequestRepository, RideParticipantRepository rideParticipantRepository) {
        this.employeeRepository = employeeRepository;
        this.rideRequestRepository = rideRequestRepository;
        this.rideParticipantRepository = rideParticipantRepository;
    }

    public Map<String, Long> getBasicStats() {
        long totalUsers = employeeRepository.count();
        long totalRides = rideRequestRepository.count();
        return Map.of("totalUsers", totalUsers, "totalRides", totalRides);
    }
    public List<AdminEmployeeViewDTO> getAllEmployeesWithRideCounts() {
        List<Employee> employees = employeeRepository.findAll();
        return employees.stream().map(employee -> {
            long ridesAsRequester = rideRequestRepository.countByRequester(employee);
            long ridesAsDriver = rideRequestRepository.countByDriver(employee);
            long ridesAsParticipant = rideParticipantRepository.countByParticipant(employee);
            return AdminEmployeeViewDTO.from(employee, ridesAsRequester + ridesAsDriver + ridesAsParticipant);
        }).collect(Collectors.toList());
    }
}
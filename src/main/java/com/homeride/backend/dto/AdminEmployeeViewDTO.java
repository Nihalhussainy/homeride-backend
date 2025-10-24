package com.homeride.backend.dto;

import com.homeride.backend.model.Employee;
import lombok.Data;

@Data
public class AdminEmployeeViewDTO {
    private Long id;
    private String name;
    private String email;
    private String role;
    private double travelCredit;
    private long ridesTraveled;

    public static AdminEmployeeViewDTO from(Employee employee, long ridesTraveled) {
        AdminEmployeeViewDTO dto = new AdminEmployeeViewDTO();
        dto.setId(employee.getId());
        dto.setName(employee.getName());
        dto.setEmail(employee.getEmail());
        dto.setRole(employee.getRole());
        dto.setTravelCredit(employee.getTravelCredit());
        dto.setRidesTraveled(ridesTraveled);
        return dto;
    }
}
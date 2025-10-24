package com.homeride.backend.dto;

import lombok.Data;

@Data
public class AdminUserUpdateDTO {
    private String role;
    private Double travelCredit;
}
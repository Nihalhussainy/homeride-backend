package com.homeride.backend.dto;

import lombok.Data;

@Data
public class RegisterRequestDTO {
    private String name;
    private String email;
    private String password;
    // NEW: Added gender field
    private String gender;
}

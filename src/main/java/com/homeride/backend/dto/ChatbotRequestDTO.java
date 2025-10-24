package com.homeride.backend.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotRequestDTO {
    @NotBlank(message = "Message cannot be empty")
    private String message;

    private String userEmail; // Add this to identify the user
}
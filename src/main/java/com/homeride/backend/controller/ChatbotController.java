package com.homeride.backend.controller;

import com.homeride.backend.dto.ChatbotRequestDTO;
import com.homeride.backend.dto.ChatbotResponseDTO;
import com.homeride.backend.service.ChatbotService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatbot")
@CrossOrigin(origins = "*")
public class ChatbotController {

    private final ChatbotService chatbotService;

    @Autowired
    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/message")
    public ResponseEntity<ChatbotResponseDTO> handleMessage(@Valid @RequestBody ChatbotRequestDTO request) {
        String userEmail = request.getUserEmail();

        // If email not provided in request, extract from JWT token (Spring Security)
        if (userEmail == null || userEmail.trim().isEmpty()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                userEmail = auth.getName(); // This gets the username/email from the JWT
                request.setUserEmail(userEmail);
            }
        }

        ChatbotResponseDTO response = chatbotService.generateResponse(request);
        return ResponseEntity.ok(response);
    }
}
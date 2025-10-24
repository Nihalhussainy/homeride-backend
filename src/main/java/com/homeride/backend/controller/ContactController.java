package com.homeride.backend.controller;

import com.homeride.backend.dto.ContactRequest;
import com.homeride.backend.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/contact")
@CrossOrigin(origins = "http://localhost:5173")
public class ContactController {

    @Autowired
    private EmailService emailService;

    @PostMapping("/send")
    public ResponseEntity<?> sendContactMessage(@Valid @RequestBody ContactRequest contactRequest) {
        try {
            emailService.sendContactEmail(contactRequest);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Thank you for your message! We'll get back to you soon.");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to send message. Please try again later.");
            return ResponseEntity.status(500).body(error);
        }
    }
}
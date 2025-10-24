package com.homeride.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/protected")
    public String protectedEndpoint() {
        return "✅ You have accessed a protected endpoint with a valid JWT!";
    }
}
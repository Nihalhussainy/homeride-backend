package com.homeride.backend;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordEncoderTest implements CommandLineRunner {

    private final PasswordEncoder passwordEncoder;

    public PasswordEncoderTest(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        String rawPassword = "mypassword"; // pretend this is user's input
        String encodedPassword = passwordEncoder.encode(rawPassword);

        System.out.println("Raw: " + rawPassword);
        System.out.println("Encoded: " + encodedPassword);

        // Check match
        boolean matches = passwordEncoder.matches("mypassword", encodedPassword);
        System.out.println("Matches? " + matches);
    }
}
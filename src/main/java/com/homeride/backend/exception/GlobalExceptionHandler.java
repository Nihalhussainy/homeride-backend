package com.homeride.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalStateException(IllegalStateException ex) {
        // This will catch our "already joined" or "cannot join own ride" errors
        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.CONFLICT); // Sends a 409 Conflict status
    }
}
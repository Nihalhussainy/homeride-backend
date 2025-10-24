package com.homeride.backend.model;

import com.homeride.backend.model.Employee; // <-- FIX: This line was missing
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Employee user;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private boolean isRead = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private String link; // Link to the relevant page (e.g., /ride/{rideId})

    // NEW FIELDS
    private String type; // e.g., "CHAT_MESSAGE", "RIDE_JOINED"
    private Long rideId; // To group chat messages by ride
}
package com.homeride.backend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ride_participants")
public class RideParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ride_request_id", nullable = false)
    @JsonBackReference
    private RideRequest rideRequest;

    @ManyToOne
    @JoinColumn(name = "participant_id", nullable = false)
    private Employee participant;

    @Column(nullable = false)
    private String pickupPoint;

    @Column(nullable = false)
    private String dropoffPoint;

    @Column
    private Double price;

    // NEW FIELD: Number of seats booked by this participant
    @Column
    private Integer numberOfSeats;

    @Column(updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
        // Default to 1 seat if not specified
        if (numberOfSeats == null) {
            numberOfSeats = 1;
        }
    }
}
package com.homeride.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ratings")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The user who GAVE the rating
    @ManyToOne
    @JoinColumn(name = "rater_id", nullable = false)
    private Employee rater;

    // The user who RECEIVED the rating
    @ManyToOne
    @JoinColumn(name = "ratee_id", nullable = false)
    private Employee ratee;

    // The ride this rating is associated with
    @ManyToOne
    @JoinColumn(name = "ride_request_id", nullable = false)
    private RideRequest rideRequest;

    @Column(nullable = false)
    private int score; // e.g., 1 to 5 stars

    @Column(columnDefinition = "TEXT")
    private String comment;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
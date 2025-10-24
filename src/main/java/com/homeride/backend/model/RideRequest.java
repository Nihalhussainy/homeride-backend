package com.homeride.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@Entity
@Table(name = "ride_requests")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RideRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "origin_city", nullable = false)
    private String originCity;

    @Column(nullable = false)
    private String origin;

    @Column(name = "destination_city", nullable = false)
    private String destinationCity;

    @Column(nullable = false)
    private String destination;

    // REMOVED: Old @ElementCollection for stops
    // @ElementCollection(fetch = FetchType.EAGER)
    // @CollectionTable(name = "ride_stops", joinColumns = @JoinColumn(name = "ride_request_id"))
    // @Column(name = "stop")
    // private List<String> stops = new ArrayList<>();

    // NEW: Relationship to the Stopover entity
    @OneToMany(mappedBy = "rideRequest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<Stopover> stopovers = new ArrayList<>();

    @Column(nullable = false)
    private String rideType;

    @Column(nullable = false)
    private LocalDateTime travelDateTime;

    private String status;
    private String vehicleModel;
    private Integer vehicleCapacity;
    private String genderPreference;
    private Double price;
    private Integer duration; // Duration in minutes
    private Double distance; // Distance in kilometers

    @Column(length = 1000)
    private String driverNote;

    @Column(columnDefinition = "TEXT")
    private String routePolyline;

    @Column
    private Double pricePerKm;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ride_stopover_prices", joinColumns = @JoinColumn(name = "ride_request_id"))
    @Column(name = "price")
    private List<Double> stopoverPrices = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private Employee requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Employee driver;

    @OneToMany(mappedBy = "rideRequest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<RideParticipant> participants = new HashSet<>();
}
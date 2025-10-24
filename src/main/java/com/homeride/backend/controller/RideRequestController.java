package com.homeride.backend.controller;

import com.homeride.backend.dto.RideRequestDTO;
import com.homeride.backend.dto.TravelInfo;
import com.homeride.backend.model.RideParticipant;
import com.homeride.backend.model.RideRequest;
import com.homeride.backend.service.GoogleMapsService;
import com.homeride.backend.service.RideRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rides")
public class RideRequestController {

    private final RideRequestService rideRequestService;
    private final GoogleMapsService googleMapsService;

    @Autowired
    public RideRequestController(
            RideRequestService rideRequestService,
            GoogleMapsService googleMapsService) {
        this.rideRequestService = rideRequestService;
        this.googleMapsService = googleMapsService;
    }

    @GetMapping("/travel-info")
    public ResponseEntity<TravelInfo> getTravelInfo(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam(required = false) String[] stops) {
        TravelInfo travelInfo = googleMapsService.getTravelInfo(origin, destination, stops);
        return ResponseEntity.ok(travelInfo);
    }

    @PostMapping("/offer") // This endpoint must exist
    public ResponseEntity<RideRequest> createRideOffer(@RequestBody RideRequestDTO rideRequestDTO, Principal principal) {
        String requesterEmail = principal.getName();
        RideRequest newRideRequest = rideRequestService.createRideOffer(rideRequestDTO, requesterEmail);
        return ResponseEntity.ok(newRideRequest);
    }

    @GetMapping
    public ResponseEntity<List<RideRequest>> getAllRides(
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String travelDateTime,
            @RequestParam(required = false) Integer passengerCount
    ) {
        List<RideRequest> rides = rideRequestService.getAllRideRequests(
                origin,
                destination,
                travelDateTime,
                passengerCount
        );
        return ResponseEntity.ok(rides);
    }

    @GetMapping("/{rideId}")
    public ResponseEntity<RideRequest> getRideById(@PathVariable Long rideId) {
        RideRequest ride = rideRequestService.getRideById(rideId);
        return ResponseEntity.ok(ride);
    }

    @PostMapping("/{rideId}/join")
    public ResponseEntity<RideParticipant> joinRide(
            @PathVariable Long rideId,
            @RequestBody Map<String, Object> segmentDetails,
            Principal principal) {
        String participantEmail = principal.getName();
        RideParticipant rideParticipant = rideRequestService.joinRideRequest(rideId, participantEmail, segmentDetails);
        return ResponseEntity.ok(rideParticipant);
    }

    @GetMapping("/my-rides")
    public ResponseEntity<List<RideRequest>> getMyRides(Principal principal) {
        List<RideRequest> myRides = rideRequestService.getRidesForUser(principal.getName());
        return ResponseEntity.ok(myRides);
    }

    @DeleteMapping("/{rideId}")
    public ResponseEntity<?> deleteRide(@PathVariable Long rideId, Principal principal) {
        rideRequestService.deleteRide(rideId, principal.getName());
        return ResponseEntity.ok().build();
    }
}
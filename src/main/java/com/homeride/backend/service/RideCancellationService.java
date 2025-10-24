// backend/src/main/java/com/homeride/backend/service/RideCancellationService.java
package com.homeride.backend.service;

import com.homeride.backend.model.Employee;
import com.homeride.backend.model.RideRequest;
import com.homeride.backend.model.RideParticipant;
import com.homeride.backend.repository.RideRequestRepository;
import com.homeride.backend.repository.RideParticipantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RideCancellationService {

    private final RideRequestRepository rideRequestRepository;
    private final RideParticipantRepository rideParticipantRepository;
    private final NotificationService notificationService;
    private final RatingService ratingService;

    @Autowired
    public RideCancellationService(
            RideRequestRepository rideRequestRepository,
            RideParticipantRepository rideParticipantRepository,
            NotificationService notificationService,
            RatingService ratingService) {
        this.rideRequestRepository = rideRequestRepository;
        this.rideParticipantRepository = rideParticipantRepository;
        this.notificationService = notificationService;
        this.ratingService = ratingService;
    }

    @Transactional
    public void cancelRideAsPassenger(Long rideId, String userEmail, String reason) {
        RideRequest ride = rideRequestRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found with id: " + rideId));

        // Find the participant to remove using a case-insensitive email comparison
        RideParticipant participantToRemove = ride.getParticipants().stream()
                .filter(p -> p.getParticipant().getEmail().equalsIgnoreCase(userEmail))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User is not a participant in this ride"));

        Employee participantUser = participantToRemove.getParticipant();

        // Remove participant from the ride's collection and delete the record
        ride.getParticipants().remove(participantToRemove);
        rideParticipantRepository.delete(participantToRemove);

        // Delete only ratings associated with this specific user for this ride
        ratingService.deleteRatingsForParticipantOnRide(ride, participantUser);

        // UPDATED: Notify the driver with passenger's name
        String driverMessage = participantUser.getName() + " has cancelled their booking for your ride from " +
                ride.getOriginCity() + " to " + ride.getDestinationCity();

        notificationService.createNotification(
                ride.getRequester(),
                driverMessage,
                "/ride/" + ride.getId(),
                "PASSENGER_CANCELLED",
                ride.getId()
        );
    }

    @Transactional
    public void cancelRideAsDriver(Long rideId, String driverEmail) {
        RideRequest ride = rideRequestRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found with id: " + rideId));

        // Verify the user is the driver using a case-insensitive email comparison
        if (!ride.getRequester().getEmail().equalsIgnoreCase(driverEmail)) {
            throw new IllegalStateException("You are not authorized to cancel this ride.");
        }

        // Delete all ratings associated with this ride
        ratingService.deleteAllRatingsForRide(ride);

        // Notify all participants before deleting
        for (RideParticipant participant : ride.getParticipants()) {
            String message = "Your ride from " + ride.getOriginCity() + " to " +
                    ride.getDestinationCity() + " has been cancelled by the driver.";

            notificationService.createNotification(
                    participant.getParticipant(),
                    message,
                    "/dashboard",
                    "RIDE_CANCELLED",
                    ride.getId()
            );
        }

        // Delete the ride. Cascade settings will automatically delete associated participants.
        rideRequestRepository.delete(ride);
    }
}
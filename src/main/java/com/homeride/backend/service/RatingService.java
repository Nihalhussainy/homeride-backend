// backend/src/main/java/com/homeride/backend/service/RatingService.java
package com.homeride.backend.service;

import com.homeride.backend.dto.RatingDTO;
import com.homeride.backend.model.Employee;
import com.homeride.backend.model.Rating;
import com.homeride.backend.model.RideRequest;
import com.homeride.backend.repository.EmployeeRepository;
import com.homeride.backend.repository.RatingRepository;
import com.homeride.backend.repository.RideRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RatingService {

    private final RatingRepository ratingRepository;
    private final EmployeeRepository employeeRepository;
    private final RideRequestRepository rideRequestRepository;
    private final NotificationService notificationService;

    @Autowired
    public RatingService(RatingRepository ratingRepository,
                         EmployeeRepository employeeRepository,
                         RideRequestRepository rideRequestRepository,
                         NotificationService notificationService) {
        this.ratingRepository = ratingRepository;
        this.employeeRepository = employeeRepository;
        this.rideRequestRepository = rideRequestRepository;
        this.notificationService = notificationService;
    }

    public Rating submitRating(RatingDTO ratingDTO, String raterEmail) {
        Employee rater = employeeRepository.findByEmail(raterEmail)
                .orElseThrow(() -> new RuntimeException("Rater not found"));

        Employee ratee = employeeRepository.findById(ratingDTO.getRateeId())
                .orElseThrow(() -> new RuntimeException("User being rated not found"));

        RideRequest rideRequest = rideRequestRepository.findById(ratingDTO.getRideRequestId())
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        if (ratingRepository.existsByRideRequestAndRaterAndRatee(rideRequest, rater, ratee)) {
            throw new IllegalStateException("You have already submitted a rating for this user on this ride.");
        }

        Rating newRating = new Rating();
        newRating.setRater(rater);
        newRating.setRatee(ratee);
        newRating.setRideRequest(rideRequest);
        newRating.setScore(ratingDTO.getScore());
        newRating.setComment(ratingDTO.getComment());

        Rating savedRating = ratingRepository.save(newRating);

        // CREATE NOTIFICATION FOR THE PERSON WHO WAS RATED
        String message = rater.getName() + " rated you for the ride from " +
                rideRequest.getOriginCity() + " to " + rideRequest.getDestinationCity();
        notificationService.createNotification(
                ratee,
                message,
                "/ride/" + rideRequest.getId(),
                "RATING_RECEIVED",
                rideRequest.getId()
        );

        return savedRating;
    }

    public void deleteAllRatingsForRide(RideRequest rideRequest) {
        ratingRepository.deleteAllByRideRequest(rideRequest);
    }

    // Method to clean up ratings when a passenger leaves a ride.
    public void deleteRatingsForParticipantOnRide(RideRequest ride, Employee participant) {
        ratingRepository.deleteAllByRideRequestAndRater(ride, participant);
        ratingRepository.deleteAllByRideRequestAndRatee(ride, participant);
    }

    public List<Rating> getRatingsForUser(String userEmail) {
        Employee employee = employeeRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ratingRepository.findByRateeId(employee.getId());
    }

    public List<Rating> getRatingsGivenByUser(String raterEmail) {
        Employee rater = employeeRepository.findByEmail(raterEmail)
                .orElseThrow(() -> new RuntimeException("Rater not found"));
        return ratingRepository.findByRater(rater);
    }

    public Double calculateAverageRating(Long employeeId) {
        List<Rating> ratings = ratingRepository.findByRateeId(employeeId);

        if (ratings == null || ratings.isEmpty()) {
            return null;
        }

        double sum = ratings.stream()
                .mapToDouble(Rating::getScore)
                .sum();

        return sum / ratings.size();
    }
}
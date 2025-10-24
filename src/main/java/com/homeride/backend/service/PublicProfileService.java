package com.homeride.backend.service;

import com.homeride.backend.dto.PublicProfileDTO;
import com.homeride.backend.model.Employee;
import com.homeride.backend.model.Rating;
import com.homeride.backend.repository.EmployeeRepository;
import com.homeride.backend.repository.RatingRepository;
import com.homeride.backend.repository.RideParticipantRepository;
import com.homeride.backend.repository.RideRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.OptionalDouble;

@Service
public class PublicProfileService {

    private final EmployeeRepository employeeRepository;
    private final RatingRepository ratingRepository;
    private final RideRequestRepository rideRequestRepository;
    private final RideParticipantRepository rideParticipantRepository;

    @Autowired
    public PublicProfileService(EmployeeRepository employeeRepository, RatingRepository ratingRepository, RideRequestRepository rideRequestRepository, RideParticipantRepository rideParticipantRepository) {
        this.employeeRepository = employeeRepository;
        this.ratingRepository = ratingRepository;
        this.rideRequestRepository = rideRequestRepository;
        this.rideParticipantRepository = rideParticipantRepository;
    }

    public PublicProfileDTO getPublicProfile(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + employeeId));

        PublicProfileDTO dto = new PublicProfileDTO();
        dto.setId(employee.getId());
        dto.setName(employee.getName());
        dto.setEmail(employee.getEmail());
        dto.setPhoneNumber(employee.getPhoneNumber()); // Add this line
        dto.setProfilePictureUrl(employee.getProfilePictureUrl());
        dto.setGender(employee.getGender());

        // Get ratings for the employee
        List<Rating> receivedRatings = ratingRepository.findByRateeId(employee.getId());
        dto.setReceivedRatings(receivedRatings);

        // Calculate average rating
        OptionalDouble average = receivedRatings.stream()
                .mapToInt(Rating::getScore)
                .average();
        dto.setAverageRating(average.isPresent() ? average.getAsDouble() : null);

        // Calculate total rides
        long ridesAsRequester = rideRequestRepository.countByRequester(employee);
        long ridesAsDriver = rideRequestRepository.countByDriver(employee);
        long ridesAsParticipant = rideParticipantRepository.countByParticipant(employee);
        dto.setTotalRides(ridesAsRequester + ridesAsDriver + ridesAsParticipant);

        return dto;
    }
}
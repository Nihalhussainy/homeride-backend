// backend/src/main/java/com/homeride/backend/repository/RatingRepository.java
package com.homeride.backend.repository;

import com.homeride.backend.model.Employee;
import com.homeride.backend.model.Rating;
import com.homeride.backend.model.RideRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    List<Rating> findByRateeId(Long rateeId);
    boolean existsByRideRequestAndRaterAndRatee(RideRequest rideRequest, Employee rater, Employee ratee);
    List<Rating> findByRater(Employee rater);

    void deleteAllByRideRequest(RideRequest rideRequest);

    // NEW: Methods to delete ratings associated with a specific user on a specific ride.
    void deleteAllByRideRequestAndRater(RideRequest rideRequest, Employee rater);
    void deleteAllByRideRequestAndRatee(RideRequest rideRequest, Employee ratee);
}
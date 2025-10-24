package com.homeride.backend.repository;

import com.homeride.backend.model.Employee;
import com.homeride.backend.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUser_IdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    // NEW METHOD
    Optional<Notification> findFirstByUserAndRideIdAndTypeAndIsReadFalse(Employee user, Long rideId, String type);
}
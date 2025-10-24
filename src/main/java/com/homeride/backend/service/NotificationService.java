package com.homeride.backend.service;

import com.homeride.backend.model.Employee;
import com.homeride.backend.model.Notification;
import com.homeride.backend.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;


@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    public void createNotification(Employee user, String message, String link, String type, Long rideId) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(message);
        notification.setLink(link);
        notification.setType(type);
        notification.setRideId(rideId);
        notificationRepository.save(notification);
    }

    public void createOrUpdateChatNotification(Employee user, String message, String link, Long rideId) {
        Optional<Notification> existingNotification = notificationRepository.findFirstByUserAndRideIdAndTypeAndIsReadFalse(user, rideId, "CHAT_MESSAGE");

        if (existingNotification.isPresent()) {
            // Notification already exists, just update the timestamp to bump it up
            Notification notification = existingNotification.get();
            notification.setCreatedAt(LocalDateTime.now()); // Update timestamp
            notificationRepository.save(notification);
        } else {
            // No unread chat notification for this ride, create a new one
            createNotification(user, message, link, "CHAT_MESSAGE", rideId);
        }
    }
}
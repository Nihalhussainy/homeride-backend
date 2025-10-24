package com.homeride.backend.controller;

import com.homeride.backend.model.Employee;
import com.homeride.backend.model.Notification;
import com.homeride.backend.repository.NotificationRepository;
import com.homeride.backend.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EmployeeService employeeService;

    @GetMapping
    public ResponseEntity<List<Notification>> getUserNotifications(Principal principal) {
        Long userId = employeeService.findEmployeeByEmail(principal.getName()).getId();
        return ResponseEntity.ok(notificationRepository.findByUser_IdAndIsReadFalseOrderByCreatedAtDesc(userId));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationRepository.findById(id).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Principal principal) {
        Long userId = employeeService.findEmployeeByEmail(principal.getName()).getId();
        List<Notification> unreadNotifications = notificationRepository.findByUser_IdAndIsReadFalseOrderByCreatedAtDesc(userId);
        unreadNotifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unreadNotifications);
        return ResponseEntity.ok().build();
    }
}
package com.homeride.backend.controller;

import com.homeride.backend.dto.ChatMessageDTO;
import com.homeride.backend.model.ChatMessage;
import com.homeride.backend.model.Employee;
import com.homeride.backend.model.RideParticipant;
import com.homeride.backend.model.RideRequest;
import com.homeride.backend.repository.ChatMessageRepository;
import com.homeride.backend.repository.EmployeeRepository;
import com.homeride.backend.repository.RideRequestRepository;
import com.homeride.backend.service.NotificationService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.time.LocalDateTime;

@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;
    private final EmployeeRepository employeeRepository;
    private final RideRequestRepository rideRequestRepository;
    private final NotificationService notificationService;


    public ChatController(SimpMessagingTemplate messagingTemplate, ChatMessageRepository chatMessageRepository, EmployeeRepository employeeRepository, RideRequestRepository rideRequestRepository, NotificationService notificationService) {
        this.messagingTemplate = messagingTemplate;
        this.chatMessageRepository = chatMessageRepository;
        this.employeeRepository = employeeRepository;
        this.rideRequestRepository = rideRequestRepository;
        this.notificationService = notificationService;
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageDTO chatMessageDTO) {
        // Look up the sender's details from the database
        Employee sender = employeeRepository.findByEmail(chatMessageDTO.getSenderEmail())
                .orElseThrow(() -> new RuntimeException("Sender not found for chat message"));

        // Convert the DTO to a JPA entity
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSenderName(sender.getName()); // Use retrieved name
        chatMessage.setSenderEmail(sender.getEmail());
        chatMessage.setSenderProfilePictureUrl(sender.getProfilePictureUrl()); // Set the profile picture URL
        chatMessage.setContent(chatMessageDTO.getContent());
        chatMessage.setRideId(chatMessageDTO.getRideId());
        chatMessage.setType(chatMessageDTO.getType());
        chatMessage.setTimestamp(LocalDateTime.now());

        // Save message to database
        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

        // Broadcast the saved message to the ride's group topic
        messagingTemplate.convertAndSend("/topic/ride." + savedMessage.getRideId(), savedMessage);

        // Create notifications for all ride participants except the sender
        RideRequest ride = rideRequestRepository.findById(chatMessageDTO.getRideId()).orElse(null);
        if (ride != null) {
            String message = "You have a new message in the chat for your ride from " + ride.getOriginCity() + " to " + ride.getDestinationCity();
            String link = "/ride/" + ride.getId();

            // Notify the driver
            if (!ride.getRequester().getEmail().equals(chatMessageDTO.getSenderEmail())) {
                notificationService.createOrUpdateChatNotification(ride.getRequester(), message, link, ride.getId());
            }

            // Notify participants
            for (RideParticipant participant : ride.getParticipants()) {
                if (!participant.getParticipant().getEmail().equals(chatMessageDTO.getSenderEmail())) {
                    notificationService.createOrUpdateChatNotification(participant.getParticipant(), message, link, ride.getId());
                }
            }
        }
    }
}
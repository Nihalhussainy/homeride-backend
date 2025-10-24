package com.homeride.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Data
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String senderName;
    private String senderEmail;
    private String content;
    private Long rideId;
    private String recipientEmail; // Null for group messages
    private String type; // GROUP or PRIVATE
    private String senderProfilePictureUrl;
    private LocalDateTime timestamp;
}
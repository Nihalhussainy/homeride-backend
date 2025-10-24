
package com.homeride.backend.dto;

import lombok.Data;

@Data
public class ChatMessageDTO {
    private String senderName;
    private String senderEmail;
    private String senderProfilePictureUrl;
    private String content;
    private Long rideId;
    private String type;
}
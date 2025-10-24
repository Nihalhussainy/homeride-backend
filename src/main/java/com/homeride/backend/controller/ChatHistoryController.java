package com.homeride.backend.controller;

import com.homeride.backend.model.ChatMessage;
import com.homeride.backend.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatHistoryController {

    private final ChatMessageRepository chatMessageRepository;

    @Autowired
    public ChatHistoryController(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    @GetMapping("/history/{rideId}")
    public ResponseEntity<List<ChatMessage>> getChatHistory(@PathVariable Long rideId) {
        List<ChatMessage> history = chatMessageRepository.findByRideIdOrderByTimestampAsc(rideId);
        return ResponseEntity.ok(history);
    }
}
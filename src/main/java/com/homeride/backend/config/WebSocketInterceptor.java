package com.homeride.backend.config;

import com.homeride.backend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
public class WebSocketInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Autowired
    public WebSocketInterceptor(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7); // Remove "Bearer " prefix
                    String email = jwtUtil.extractEmail(token);

                    if (email != null && jwtUtil.isTokenValid(token, email)) {
                        // Load user details
                        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                        // Create authentication token
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());

                        // Set the principal for this WebSocket session
                        accessor.setUser(authentication);
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        System.out.println("✅ WebSocket authenticated for user: " + email);
                    } else {
                        System.out.println("❌ Invalid or expired token");
                        throw new IllegalArgumentException("Invalid token");
                    }
                } catch (Exception e) {
                    System.out.println("❌ WebSocket authentication error: " + e.getMessage());
                    throw new IllegalArgumentException("Authentication failed", e);
                }
            } else {
                System.out.println("❌ No authorization header found");
                throw new IllegalArgumentException("Authorization header missing");
            }
        }

        return message;
    }
}
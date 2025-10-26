package com.homeride.backend.service;

import com.homeride.backend.dto.ContactRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${contact.email.recipient}")
    private String recipientEmail;

    @Value("${spring.mail.username}")
    private String senderEmail;

    public void sendContactEmail(ContactRequest contactRequest) {
        try {
            logger.info("Preparing to send contact email from: {} to: {}", contactRequest.getEmail(), recipientEmail);

            SimpleMailMessage message = new SimpleMailMessage();

            // Set recipient (your business email)
            message.setTo(recipientEmail);

            // CRITICAL: Use the authenticated Gmail address as the sender
            // Gmail SMTP requires this to match the authenticated account
            message.setFrom(senderEmail);

            // Set reply-to (so you can reply directly to the user)
            message.setReplyTo(contactRequest.getEmail());

            // Set subject
            message.setSubject("New Contact Form Submission from " + contactRequest.getName());

            // Set body
            String emailBody = String.format(
                    "You have received a new message from the HomeRide contact form.\n\n" +
                            "Name: %s\n" +
                            "Email: %s\n\n" +
                            "Message:\n%s\n\n" +
                            "---\n" +
                            "This message was sent via the HomeRide contact form.",
                    contactRequest.getName(),
                    contactRequest.getEmail(),
                    contactRequest.getMessage()
            );

            message.setText(emailBody);

            // Send the email
            mailSender.send(message);
            logger.info("Contact email sent successfully to: {}", recipientEmail);

        } catch (Exception e) {
            logger.error("Failed to send contact email", e);
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }
}
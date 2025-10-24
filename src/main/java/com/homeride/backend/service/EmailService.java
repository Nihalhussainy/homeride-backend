package com.homeride.backend.service;

import com.homeride.backend.dto.ContactRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${contact.email.recipient}")
    private String recipientEmail;

    public void sendContactEmail(ContactRequest contactRequest) {
        SimpleMailMessage message = new SimpleMailMessage();

        // Set recipient (your business email)
        message.setTo(recipientEmail);

        // Set from (the user's email)
        message.setFrom(contactRequest.getEmail());

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
    }
}
package com.homeride.backend.service;

import com.homeride.backend.dto.ContactRequest;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class EmailService {

    // Inject the API key and verified "from" email from application.properties
    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    @Value("${sendgrid.from.email}")
    private String fromEmail;

    @Value("${contact.email.recipient}")
    private String recipientEmail;

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    public void sendContactEmail(ContactRequest contactRequest) {

        // --- This is the new SendGrid Logic ---

        // 1. Set the "from" email (must be your verified SendGrid sender)
        Email from = new Email(fromEmail);

        // 2. Set the "to" email (your business email)
        Email to = new Email(recipientEmail);

        // 3. Set the subject
        String subject = "New Contact Form Submission from " + contactRequest.getName();

        // 4. Build the email body
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
        Content content = new Content("text/plain", emailBody);

        // 5. Create the SendGrid Mail object
        Mail mail = new Mail(from, subject, to, content);

        // 6. IMPORTANT: Set the "reply-to" to the user's email
        // This makes it so hitting "Reply" in your inbox replies to the user, not your SendGrid account.
        mail.setReplyTo(new Email(contactRequest.getEmail()));

        // 7. Initialize the SendGrid client
        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();

        // 8. Send the email
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);

            logger.info("SendGrid Response Status Code: {}", response.getStatusCode());
            logger.info("SendGrid Response Body: {}", response.getBody());

            if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
                throw new IOException("Error sending email: " + response.getBody());
            }

        } catch (IOException ex) {
            logger.error("Error sending email via SendGrid", ex);
            // Re-throw as a runtime exception so the controller can handle it
            throw new RuntimeException("Failed to send contact email: " + ex.getMessage());
        }
    }
}
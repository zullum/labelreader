package com.labelreader.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@labelreader.com}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:4200}")
    private String baseUrl;

    @Async
    public void sendNotificationEmail(String toEmail, String title, String message, String linkUrl) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(toEmail);
            mailMessage.setSubject("LabelReader - " + title);

            String body = message + "\n\n";
            if (linkUrl != null && !linkUrl.isEmpty()) {
                body += "View details: " + baseUrl + linkUrl + "\n\n";
            }
            body += "---\nThis is an automated notification from LabelReader.";

            mailMessage.setText(body);
            mailSender.send(mailMessage);
            log.info("Email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String userName) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(toEmail);
            mailMessage.setSubject("Welcome to LabelReader!");

            String body = "Hello " + userName + ",\n\n" +
                    "Welcome to LabelReader! We're excited to have you on board.\n\n" +
                    "Start exploring: " + baseUrl + "\n\n" +
                    "Best regards,\n" +
                    "The LabelReader Team";

            mailMessage.setText(body);
            mailSender.send(mailMessage);
            log.info("Welcome email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
        }
    }
}

package com.labelreader.security;

import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;

@Component
public class InputSanitizer {

    private final PolicyFactory policy = Sanitizers.FORMATTING.and(Sanitizers.LINKS);

    public String sanitizeHtml(String input) {
        if (input == null) {
            return null;
        }
        return policy.sanitize(input);
    }

    public String sanitizeFilePath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new SecurityException("File path cannot be null or empty");
        }

        // Normalize the path
        String normalized = Paths.get(filePath).normalize().toString();

        // Prevent path traversal
        if (normalized.contains("..") || normalized.startsWith("/") || normalized.contains("\\..")) {
            throw new SecurityException("Path traversal attempt detected");
        }

        // Prevent absolute paths
        if (Paths.get(normalized).isAbsolute()) {
            throw new SecurityException("Absolute paths are not allowed");
        }

        return normalized;
    }

    public String sanitizeEmail(String email) {
        if (email == null) {
            return null;
        }

        // Basic email pattern validation
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        if (!email.matches(emailRegex)) {
            throw new IllegalArgumentException("Invalid email format");
        }

        return email.trim().toLowerCase();
    }

    public String sanitizeUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        // Remove any HTML tags and special characters
        String sanitized = username.replaceAll("[<>\"'&]", "");

        if (sanitized.length() < 3 || sanitized.length() > 50) {
            throw new IllegalArgumentException("Username must be between 3 and 50 characters");
        }

        return sanitized.trim();
    }
}

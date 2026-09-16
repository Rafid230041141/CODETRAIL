package application.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    public void sendPasswordResetEmail(String recipientEmail, String recipientName, String resetCode) {
        log.info("==========================================================");
        log.info("[EMAIL DISPATCH] Password reset verification code for: {}", recipientEmail);
        log.info("Recipient: {}", recipientName != null && !recipientName.isBlank() ? recipientName : recipientEmail);
        log.info("Subject: CodeTrail - Reset Your Password");
        log.info("Verification Code: [{}]", resetCode);
        log.info("Valid for: 15 minutes");
        log.info("==========================================================");
    }
}

package com.waste.management.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String mailHost;

    public EmailService(
            @Autowired(required = false) JavaMailSender mailSender,
            @Value("${app.password-reset.from-email:no-reply@plasticwaste.org}") String fromEmail,
            @Value("${spring.mail.host:}") String mailHost) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.mailHost = mailHost;
    }

    /**
     * Dispatches a password reset email to the recipient.
     * Note: In accordance with security requirements, raw reset tokens, reset URLs,
     * and credentials are NEVER output to ordinary application logs.
     */
    public boolean sendPasswordResetEmail(String recipientEmail, String resetUrl) {
        if (!isSmtpConfigured()) {
            logger.info("[EMAIL_SERVICE] SMTP host not configured. Reset email queued/dispatched in mock/offline mode for user.");
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

            helper.setFrom(fromEmail);
            helper.setTo(recipientEmail);
            helper.setSubject("Password Reset Request - EcoClean Plastic Waste Network");

            String htmlContent = buildResetEmailHtml(resetUrl);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("[EMAIL_SERVICE] Password reset email successfully delivered via SMTP.");
            return true;
        } catch (MessagingException | MailException ex) {
            // Fail safely in production: Log error type without exposing tokens, URLs, or credentials
            logger.error("[EMAIL_SERVICE] Failed to deliver password reset email via SMTP: {}", ex.getClass().getSimpleName());
            return false;
        }
    }

    public boolean isSmtpConfigured() {
        return mailSender != null && StringUtils.hasText(mailHost);
    }

    private String buildResetEmailHtml(String resetUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0f172a; color: #f8fafc; padding: 24px; }
                .card { max-width: 560px; margin: 0 auto; background: #1e293b; border-radius: 8px; border: 1px solid #334155; padding: 32px; }
                .btn { display: inline-block; background: #10b981; color: #022c22; font-weight: 700; padding: 12px 24px; border-radius: 6px; text-decoration: none; margin: 20px 0; }
                .footer { font-size: 12px; color: #94a3b8; margin-top: 24px; border-top: 1px solid #334155; padding-top: 16px; }
              </style>
            </head>
            <body>
              <div class="card">
                <h2 style="color: #38bdf8; margin-top: 0;">Password Reset Request</h2>
                <p>Hello,</p>
                <p>We received a request to reset the password for your account on the <strong>EcoClean Plastic Waste Network</strong>.</p>
                <p>Click the button below to choose a new password. This link is single-use and will expire in 30 minutes.</p>
                <p><a href="%s" class="btn">Reset My Password</a></p>
                <p>If you did not request this password reset, please ignore this email. Your existing password will remain secure and unchanged.</p>
                <div class="footer">
                  EcoClean Network &bull; Secure Authentication Service &bull; Do not reply to this email
                </div>
              </div>
            </body>
            </html>
            """.formatted(resetUrl);
    }
}

package com.qprint.auth.service;

import jakarta.mail.internet.MimeMessage;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${SMTP_FROM:noreply@qprint.app}")
    private String from;

    public void sendHtml(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(Objects.requireNonNull(from, "from"));
            helper.setTo(Objects.requireNonNull(to, "to"));
            helper.setSubject(Objects.requireNonNull(subject, "subject"));
            helper.setText(Objects.requireNonNull(html, "html"), true);
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Email send failed to={} subject={}: {}", to, subject, e.getMessage());
        }
    }
}

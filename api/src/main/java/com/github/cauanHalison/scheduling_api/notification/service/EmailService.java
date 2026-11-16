package com.github.cauanHalison.scheduling_api.notification.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@scheduling.local}")
    private String fromAddress;

    public void sendEmail(
            String to,
            String subject,
            String htmlBody,
            byte[] icsBytes
    ) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "utf-8");

            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            if (icsBytes != null) {
                String icsText = new String(icsBytes, StandardCharsets.UTF_8);
                String method = icsText.contains("METHOD:CANCEL") ? "CANCEL" : "REQUEST";

                helper.addAttachment(
                        "invite.ics",
                        new ByteArrayResource(icsBytes),
                        "text/calendar; charset=utf-8; method=" + method
                );
            }
            mailSender.send(mimeMessage);

        } catch (Exception e) {
            log.error("Sending email to {} failed: {}", to, e.getMessage(), e);
        }
    }
}

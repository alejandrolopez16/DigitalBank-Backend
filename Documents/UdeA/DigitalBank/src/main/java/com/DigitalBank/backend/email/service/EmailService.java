package com.DigitalBank.backend.email.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class EmailService {
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom("DigitalBank <digitalbanknotificaciones@gmail.com>");

            // 👇 AQUÍ DECIMOS QUE ES HTML
            helper.setText(content, true);

            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Error enviando correo", e);
        }
    }

}

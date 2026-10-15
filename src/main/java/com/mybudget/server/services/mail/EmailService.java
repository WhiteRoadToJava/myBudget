package com.mybudget.server.services.mail;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSernder;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.mail.enabled:true}")
    private boolean enabled;

    public EmailService(JavaMailSender mailSernder){
        this.mailSernder = mailSernder;
    }

    @Async
    public void send(String to, String subject, String html){
        if(!enabled){
            log.info("[MAIL disabked] to={} subject={}\n {}", to, subject, html);
            return;
        }
        try {
            MimeMessage message = mailSernder.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSernder.send(message);
        } catch (Exception e) {
            log.error("Failed to send email o {}", to , e);
        }
    }
}

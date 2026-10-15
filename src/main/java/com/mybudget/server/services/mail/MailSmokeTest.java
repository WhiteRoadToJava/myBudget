package com.mybudget.server.services.mail;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;

@Component
public class MailSmokeTest implements ApplicationRunner {
    private final EmailService emailService;
    private static final Logger log = LoggerFactory.getLogger(MailSmokeTest.class);

    @Value("${MAIL_TEST_TO:}")
    private String to;

    public MailSmokeTest(EmailService emailService){
        this.emailService = emailService;
    }

    @Override
    public void run(ApplicationArguments args) {
        /*
        if (to == null || to.isBlank()) {
            log.warn("MailSmokeTest: MAIL_TEST_TO is empty, skipping");
            return;
        }

        log.info("MailSmokeTest: sending test mail to {}", to);
        emailService.send(to, "MyBudget SMTP test", "<p>SMTP works ✅</p>");


         */
    }
}

package com.mybudget.server.services;

import com.mybudget.server.exeptions.InvalidTokenException;
import com.mybudget.server.exeptions.InvalidTokenException.Reason;
import com.mybudget.server.modules.User;
import com.mybudget.server.modules.VerificationToken;
import com.mybudget.server.modules.enums.TokenType;
import com.mybudget.server.repositories.UserRepository;
import com.mybudget.server.services.mail.EmailService;
import com.mybudget.server.services.mail.EmailTemplates;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AccountVerificationService {
    private static final Duration ACTIVATION_TTL = Duration.ofHours(24);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);

    private final TokenService tokenService;
    private final EmailService emailService;
    private final EmailTemplates emailTemplates;
    private final UserRepository userRepository;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public void sendActivationEmail(User user, String language) {
        String token = tokenService.createToken(user.getUsername(), TokenType.ACTIVATION, ACTIVATION_TTL);
        String link = baseUrl() + "/auth/activate?token=" + token;
        EmailTemplates.Mail mail = emailTemplates.activation(language, user.getFirstName(), link);
        emailService.send(user.getUsername(), mail.subject(), mail.html());
    }

    public void activate(String rawToken) {
        VerificationToken token = tokenService.consume(rawToken, TokenType.ACTIVATION);
        User user = userRepository.findByUsername(token.getUsername())
                .orElseThrow(() -> new InvalidTokenException(Reason.TOKEN_INVALID));
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            user.setEnabled(true);
            userRepository.save(user);
        }
    }

    public void resendActivation(String username, String language) {
        userRepository.findByUsername(username)
                .filter(u -> Boolean.FALSE.equals(u.getEnabled()))
                .filter(u -> !tokenService.issuedWithin(u.getUsername(), TokenType.ACTIVATION, RESEND_COOLDOWN))
                .ifPresent(u -> sendActivationEmail(u, language));
    }

    private String baseUrl() {
        return frontendUrl.replaceAll("/+$", "");
    }
}

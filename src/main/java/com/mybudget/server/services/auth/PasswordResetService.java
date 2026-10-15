package com.mybudget.server.services.auth;

import com.mybudget.server.dto.auth.password.ResetPasswordRequest;
import com.mybudget.server.exeptions.InvalidTokenException;
import com.mybudget.server.exeptions.InvalidTokenException.Reason;
import com.mybudget.server.modules.User;
import com.mybudget.server.modules.VerificationToken;
import com.mybudget.server.modules.enums.TokenType;
import com.mybudget.server.repositories.UserRepository;
import com.mybudget.server.services.TokenService;
import com.mybudget.server.services.mail.EmailService;
import com.mybudget.server.services.mail.EmailTemplates;
import lombok.RequiredArgsConstructor;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;


@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private static final Duration REST_TTL = Duration.ofHours(1);
    private static final Duration rESEND_COOLDOWN = Duration.ofSeconds(60);

    private final TokenService tokenService;
    private final EmailService emailService;
    private final EmailTemplates emailTemplates;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend.url}")
    private String frontendUrl;


    public void requestReset(String username, String language) {
        userRepository.findByUsername(username)
                .filter(u -> !tokenService.issuedWithin(u.getUsername(), TokenType.PASSWORD_RESET, rESEND_COOLDOWN))
                .ifPresent(u -> {
                    String token = tokenService.createToken(u.getUsername(), TokenType.PASSWORD_RESET, REST_TTL);
                    String link = frontendUrl.replaceAll("/+$", "") + "/auth/reset-password?token=" + token;
                    EmailTemplates.Mail mail = emailTemplates.passwordReset(language, u.getFirstName(), link);
                    emailService.send(u.getUsername(), mail.subject(), mail.html());
                });
    }


    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Password do not match");
        }

        VerificationToken token = tokenService.consume(request.getToken(), TokenType.PASSWORD_RESET);
        User user = userRepository.findByUsername(token.getUsername())
                .orElseThrow(() -> new InvalidTokenException(Reason.TOKEN_INVALID));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        if (Boolean.FALSE.equals(user.getEnabled())) {
            user.setEnabled(true);
        }
        userRepository.save(user);
    }
}



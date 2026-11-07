package com.mybudget.server.services.admin;

import com.mybudget.server.dto.admin.AdminAccount;
import com.mybudget.server.dto.admin.InviteAdminRequest;
import com.mybudget.server.exeptions.ConflictException;
import com.mybudget.server.exeptions.ResourceNotFoundException;
import com.mybudget.server.modules.User;
import com.mybudget.server.modules.enums.Role;
import com.mybudget.server.modules.enums.TokenType;
import com.mybudget.server.repositories.UserRepository;
import com.mybudget.server.services.TokenService;
import com.mybudget.server.services.mail.EmailService;
import com.mybudget.server.services.mail.EmailTemplates;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * An admin adds another admin by email. The new account starts disabled with a
 * random password nobody knows; the email contains a one-time link to the
 * reset-password page, where the new admin chooses a password. Choosing it
 * enables the account (PasswordResetService), and the admin can sign in.
 */
@Service
@RequiredArgsConstructor
public class AdminInvitationService {

    private static final Duration INVITATION_TTL = Duration.ofHours(72);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final EmailTemplates emailTemplates;
    private final MongoTemplate mongoTemplate;

    @Value("${app.frontend.path}")
    private String frontendUrl;

    public AdminAccount invite(InviteAdminRequest request, User invitedBy) {
        String email = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("Enter a valid email address");
        }
        if (request.getFirstName() == null || request.getFirstName().isBlank()) {
            throw new IllegalArgumentException("Enter the first name");
        }
        if (userRepository.findByUsername(email).isPresent()) {
            throw new ConflictException("A user with this email already exists");
        }

        User admin = new User();
        admin.setUsername(email);
        admin.setFirstName(request.getFirstName().trim());
        admin.setLastName(clean(request.getLastName()));
        admin.setPhone(clean(request.getPhone()));
        admin.setRoles(Set.of(Role.ADMIN));
        // nobody knows this password; the invitation link replaces it
        admin.setPassword(passwordEncoder.encode(randomSecret()));
        admin.setEnabled(false);
        User saved = userRepository.save(admin);

        sendInvitation(saved, request.getLanguage(), invitedBy);
        return toAccount(saved);
    }

    // a new link for an invitation that was lost or has expired
    public AdminAccount resend(String adminId, String language, User invitedBy) {
        User admin = userRepository.findById(adminId)
                .filter(u -> u.getRoles() != null && u.getRoles().contains(Role.ADMIN))
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        if (Boolean.TRUE.equals(admin.getEnabled())) {
            throw new ConflictException("This admin has already chosen a password");
        }
        if (tokenService.issuedWithin(admin.getUsername(), TokenType.PASSWORD_RESET, RESEND_COOLDOWN)) {
            throw new ConflictException("An invitation was sent less than a minute ago");
        }
        sendInvitation(admin, language, invitedBy);
        return toAccount(admin);
    }

    public List<AdminAccount> list() {
        Query query = new Query(Criteria.where("roles").is(Role.ADMIN));
        return mongoTemplate.find(query, User.class).stream().map(this::toAccount).toList();
    }

    private void sendInvitation(User admin, String language, User invitedBy) {
        // the same token type as "forgot password", so the existing page and endpoint handle it
        String token = tokenService.createToken(admin.getUsername(), TokenType.PASSWORD_RESET, INVITATION_TTL);
        String link = frontendUrl.replaceAll("/+$", "") + "/auth/reset-password?token=" + token + "&invite=1";
        String inviter = invitedBy == null ? "" : nameOf(invitedBy);
        EmailTemplates.Mail mail = emailTemplates.adminInvitation(language, admin.getFirstName(), inviter, link);
        emailService.send(admin.getUsername(), mail.subject(), mail.html());
    }

    private AdminAccount toAccount(User user) {
        return new AdminAccount(user.getId(), user.getUsername(), user.getFirstName(), user.getLastName(),
                !Boolean.TRUE.equals(user.getEnabled()), user.getCreatedAt());
    }

    private String nameOf(User user) {
        String name = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return name.isEmpty() ? user.getUsername() : name;
    }

    private static String randomSecret() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String clean(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}

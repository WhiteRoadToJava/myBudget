package com.mybudget.server.services.admin;

import com.mybudget.server.dto.admin.AdminAccount;
import com.mybudget.server.dto.admin.InviteAdminRequest;
import com.mybudget.server.exeptions.ConflictException;
import com.mybudget.server.modules.User;
import com.mybudget.server.modules.enums.Role;
import com.mybudget.server.modules.enums.TokenType;
import com.mybudget.server.repositories.UserRepository;
import com.mybudget.server.services.TokenService;
import com.mybudget.server.services.mail.EmailService;
import com.mybudget.server.services.mail.EmailTemplates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminInvitationServiceTest {

    private UserRepository users;
    private TokenService tokens;
    private EmailService email;
    private AdminInvitationService service;
    private final User inviter = new User();

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        tokens = mock(TokenService.class);
        email = mock(EmailService.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(anyString())).thenReturn("encoded");
        when(users.save(any())).thenAnswer(i -> i.getArgument(0));
        when(tokens.createToken(anyString(), eq(TokenType.PASSWORD_RESET), any())).thenReturn("TOKEN");
        service = new AdminInvitationService(users, encoder, tokens, email, new EmailTemplates(), null);
        ReflectionTestUtils.setField(service, "frontendUrl", "https://mybudget.win/");
        inviter.setFirstName("Sara");
    }

    private InviteAdminRequest request(String mail) {
        InviteAdminRequest r = new InviteAdminRequest();
        r.setEmail(mail);
        r.setFirstName("Omar");
        r.setLanguage("ar");
        return r;
    }

    @Test
    void anInvitedAdminStartsDisabledAndGetsALink() {
        AdminAccount account = service.invite(request("  Omar@Example.COM "), inviter);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertEquals("omar@example.com", saved.getValue().getUsername());
        assertEquals(Set.of(Role.ADMIN), saved.getValue().getRoles());
        assertFalse(saved.getValue().getEnabled());
        assertTrue(account.pending());

        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(email).send(eq("omar@example.com"), anyString(), html.capture());
        assertTrue(html.getValue().contains("https://mybudget.win/auth/reset-password?token=TOKEN&amp;invite=1"));
        assertTrue(html.getValue().contains("Sara"));
    }

    @Test
    void anExistingEmailIsRefused() {
        when(users.findByUsername("omar@example.com")).thenReturn(Optional.of(new User()));
        assertThrows(ConflictException.class, () -> service.invite(request("omar@example.com"), inviter));
        verify(email, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void theEmailAndNameAreChecked() {
        assertThrows(IllegalArgumentException.class, () -> service.invite(request("not-an-email"), inviter));
        InviteAdminRequest noName = request("a@b.se");
        noName.setFirstName(" ");
        assertThrows(IllegalArgumentException.class, () -> service.invite(noName, inviter));
    }

    @Test
    void anAcceptedInvitationCanNotBeSentAgain() {
        User active = new User();
        active.setRoles(Set.of(Role.ADMIN));
        active.setEnabled(true);
        when(users.findById("a1")).thenReturn(Optional.of(active));
        assertThrows(ConflictException.class, () -> service.resend("a1", "ar", inviter));
    }
}

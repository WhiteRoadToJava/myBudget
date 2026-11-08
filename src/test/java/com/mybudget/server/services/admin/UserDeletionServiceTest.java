package com.mybudget.server.services.admin;

import com.mongodb.client.result.DeleteResult;
import com.mybudget.server.dto.admin.UserDeletionReport;
import com.mybudget.server.exeptions.ConflictException;
import com.mybudget.server.modules.SubscriptionPayment;
import com.mybudget.server.modules.User;
import com.mybudget.server.modules.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserDeletionServiceTest {

    @TempDir
    Path uploads;

    private MongoTemplate mongo;
    private UserDeletionService service;
    private User admin;
    private User user;

    @BeforeEach
    void setUp() {
        mongo = mock(MongoTemplate.class);
        service = new UserDeletionService(mongo);
        ReflectionTestUtils.setField(service, "uploadDir", uploads.toString());
        when(mongo.remove(any(Query.class), any(Class.class))).thenReturn(DeleteResult.acknowledged(2));
        when(mongo.find(any(Query.class), any(Class.class))).thenReturn(List.of());

        admin = new User();
        admin.setId("admin");
        admin.setUsername("admin@x.com");
        admin.setRoles(Set.of(Role.ADMIN));

        user = new User();
        user.setId("u1");
        user.setUsername("ali@x.com");
        user.setRoles(Set.of(Role.USER));
        when(mongo.findById("u1", User.class)).thenReturn(user);
        when(mongo.findById("admin", User.class)).thenReturn(admin);
    }

    @Test
    void deletesTheUserAndEverythingTheyOwn() {
        UserDeletionReport report = service.delete("u1", " ALI@x.com ", admin);
        assertEquals("ali@x.com", report.username());
        assertEquals(2L, report.deleted().get("accounts"));
        assertEquals(1L, report.deleted().get("users"));
        assertEquals(11, report.deleted().size());
        verify(mongo).remove(user);
    }

    @Test
    void thePaymentHistoryIsKept() {
        when(mongo.count(any(Query.class), eq(SubscriptionPayment.class))).thenReturn(3L);
        UserDeletionReport report = service.delete("u1", "ali@x.com", admin);
        assertFalse(report.deleted().containsKey("subscriptionPayments"));
        assertEquals(3L, report.subscriptionPaymentsKept());
        verify(mongo, never()).remove(any(Query.class), eq(SubscriptionPayment.class));
    }

    @Test
    void theEmailMustBeTypedToConfirm() {
        assertThrows(IllegalArgumentException.class, () -> service.delete("u1", "someone@x.com", admin));
        assertThrows(IllegalArgumentException.class, () -> service.delete("u1", null, admin));
        verify(mongo, never()).remove(user);
    }

    @Test
    void anAdminCanNotDeleteThemselves() {
        assertThrows(ConflictException.class, () -> service.delete("admin", "admin@x.com", admin));
    }

    @Test
    void theLastAdminStays() {
        User other = new User();
        other.setId("a2");
        other.setUsername("sara@x.com");
        other.setRoles(Set.of(Role.ADMIN));
        when(mongo.findById("a2", User.class)).thenReturn(other);
        when(mongo.count(any(Query.class), eq(User.class))).thenReturn(1L);
        assertThrows(ConflictException.class, () -> service.delete("a2", "sara@x.com", admin));
    }
}

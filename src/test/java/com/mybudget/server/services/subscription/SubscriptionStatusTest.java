package com.mybudget.server.services.subscription;

import com.mybudget.server.dto.subscription.SubscriptionInfo;
import com.mybudget.server.modules.User;
import com.mybudget.server.modules.enums.Role;
import com.mybudget.server.modules.enums.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// The status rules only; no database needed
class SubscriptionStatusTest {

    private SubscriptionService service;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        service = new SubscriptionService(null, null, null);
        ReflectionTestUtils.setField(service, "trialDays", 30);
        ReflectionTestUtils.setField(service, "graceDays", 3);
        ReflectionTestUtils.setField(service, "zone", "Asia/Damascus");
        today = service.today();
    }

    private User user(LocalDate endsAt, Boolean onTrial) {
        User user = new User();
        user.setRoles(Set.of(Role.USER));
        user.setSubscriptionEndsAt(endsAt);
        user.setOnTrial(onTrial);
        return user;
    }

    @Test
    void aNewUserGetsThirtyDaysOfTrial() {
        User user = user(null, null);
        service.startTrial(user);
        assertEquals(today.plusDays(30), user.getSubscriptionEndsAt());
        assertEquals(SubscriptionStatus.TRIAL, service.infoOf(user).status());
    }

    @Test
    void theLastDayIsStillActive() {
        SubscriptionInfo info = service.infoOf(user(today, false));
        assertEquals(SubscriptionStatus.ACTIVE, info.status());
        assertEquals(0L, info.daysLeft());
        assertTrue(info.canWrite());
    }

    @Test
    void graceDaysKeepFullAccess() {
        assertEquals(SubscriptionStatus.GRACE, service.infoOf(user(today.minusDays(1), false)).status());
        assertEquals(SubscriptionStatus.GRACE, service.infoOf(user(today.minusDays(3), false)).status());
        assertTrue(service.infoOf(user(today.minusDays(3), false)).canWrite());
    }

    @Test
    void afterTheGraceDaysItIsReadOnly() {
        SubscriptionInfo info = service.infoOf(user(today.minusDays(4), false));
        assertEquals(SubscriptionStatus.EXPIRED, info.status());
        assertFalse(info.canWrite());
    }

    @Test
    void usersWithoutADateKeepFullAccess() {
        SubscriptionInfo info = service.infoOf(user(null, null));
        assertEquals(SubscriptionStatus.NONE, info.status());
        assertTrue(info.canWrite());
    }

    @Test
    void theOldTextDateIsUsedWhenTheNewOneIsEmpty() {
        User user = user(null, null);
        user.setExpireDate(today.minusDays(10).toString());
        assertEquals(SubscriptionStatus.EXPIRED, service.infoOf(user).status());
    }

    @Test
    void adminsAreNeverBlocked() {
        User admin = user(today.minusDays(100), false);
        admin.setRoles(Set.of(Role.ADMIN));
        assertTrue(service.infoOf(admin).canWrite());
    }
}

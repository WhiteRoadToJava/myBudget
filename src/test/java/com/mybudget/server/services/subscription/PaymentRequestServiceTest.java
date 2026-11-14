package com.mybudget.server.services.subscription;

import com.mybudget.server.dto.subscription.AdminUserDetails;
import com.mybudget.server.dto.subscription.CreatePaymentRequest;
import com.mybudget.server.dto.subscription.ExtendSubscriptionRequest;
import com.mybudget.server.dto.subscription.HandlePaymentRequest;
import com.mybudget.server.exeptions.ConflictException;
import com.mybudget.server.modules.PaymentRequest;
import com.mybudget.server.modules.User;
import com.mybudget.server.modules.enums.PaymentMethod;
import com.mybudget.server.modules.enums.PaymentRequestStatus;
import com.mybudget.server.repositories.PaymentRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentRequestServiceTest {

    private PaymentRequestRepository repository;
    private SubscriptionService subscriptionService;
    private PaymentRequestService service;
    private User user;

    @BeforeEach
    void setUp() {
        repository = mock(PaymentRequestRepository.class);
        subscriptionService = mock(SubscriptionService.class);
        service = new PaymentRequestService(repository, subscriptionService);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        user = new User();
        user.setId("u1");
        user.setUsername("ali@x.com");
    }

    private CreatePaymentRequest valid() {
        CreatePaymentRequest r = new CreatePaymentRequest();
        r.setMonths(3);
        r.setMethod(PaymentMethod.SHAM_CASH);
        r.setReference(" TX-1 ");
        r.setAmount(new BigDecimal("150000"));
        r.setCurrency("syp");
        return r;
    }

    private PaymentRequest pending() {
        PaymentRequest p = new PaymentRequest();
        p.setId("r1");
        p.setUserId("u1");
        p.setMonths(3);
        p.setMethod(PaymentMethod.SHAM_CASH);
        p.setReference("TX-1");
        p.setStatus(PaymentRequestStatus.PENDING);
        return p;
    }

    @Test
    void aUserSendsARequest() {
        PaymentRequest saved = service.create(user, valid());
        assertEquals(PaymentRequestStatus.PENDING, saved.getStatus());
        assertEquals("TX-1", saved.getReference());
        assertEquals("SYP", saved.getCurrency());
    }

    @Test
    void onlyOnePendingRequestAtATime() {
        when(repository.existsByUserIdAndStatus("u1", PaymentRequestStatus.PENDING)).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.create(user, valid()));
    }

    @Test
    void theRequestNeedsMonthsAMethodAndAReference() {
        CreatePaymentRequest noReference = valid();
        noReference.setReference("  ");
        assertThrows(IllegalArgumentException.class, () -> service.create(user, noReference));

        CreatePaymentRequest free = valid();
        free.setMethod(PaymentMethod.FREE);
        assertThrows(IllegalArgumentException.class, () -> service.create(user, free));

        CreatePaymentRequest tooLong = valid();
        tooLong.setMonths(24);
        assertThrows(IllegalArgumentException.class, () -> service.create(user, tooLong));
    }

    @Test
    void approvingExtendsByTheRequestedMonths() {
        when(repository.findById("r1")).thenReturn(Optional.of(pending()));
        when(subscriptionService.extend(eq("u1"), any(), eq("admin"))).thenReturn(new AdminUserDetails(null, List.of()));

        service.approve("r1", null, "admin");

        ArgumentCaptor<ExtendSubscriptionRequest> extend = ArgumentCaptor.forClass(ExtendSubscriptionRequest.class);
        verify(subscriptionService).extend(eq("u1"), extend.capture(), eq("admin"));
        assertEquals(3, extend.getValue().getMonths());
        assertEquals(PaymentMethod.SHAM_CASH, extend.getValue().getMethod());
        assertEquals("TX-1", extend.getValue().getReference());
    }

    @Test
    void aHandledRequestCanNotBeHandledAgain() {
        PaymentRequest done = pending();
        done.setStatus(PaymentRequestStatus.APPROVED);
        when(repository.findById("r1")).thenReturn(Optional.of(done));
        assertThrows(ConflictException.class, () -> service.approve("r1", null, "admin"));
        verify(subscriptionService, never()).extend(any(), any(), any());
    }

    @Test
    void rejectingNeedsAReason() {
        when(repository.findById("r1")).thenReturn(Optional.of(pending()));
        assertThrows(IllegalArgumentException.class, () -> service.reject("r1", new HandlePaymentRequest(), "admin"));

        HandlePaymentRequest reason = new HandlePaymentRequest();
        reason.setAdminNote("No transfer with this number");
        assertEquals(PaymentRequestStatus.REJECTED, service.reject("r1", reason, "admin").getStatus());
    }
}

package com.mybudget.server.services.subscription;

import com.mybudget.server.dto.subscription.AdminUserDetails;
import com.mybudget.server.dto.subscription.AdminUserSummary;
import com.mybudget.server.dto.subscription.ExtendSubscriptionRequest;
import com.mybudget.server.dto.subscription.PageResult;
import com.mybudget.server.dto.subscription.SubscriptionInfo;
import com.mybudget.server.dto.subscription.SubscriptionStats;
import com.mybudget.server.exeptions.ResourceNotFoundException;
import com.mybudget.server.modules.SubscriptionPayment;
import com.mybudget.server.modules.User;
import com.mybudget.server.modules.enums.PaymentMethod;
import com.mybudget.server.modules.enums.Role;
import com.mybudget.server.modules.enums.SubscriptionStatus;
import com.mybudget.server.repositories.SubscriptionPaymentRepository;
import com.mybudget.server.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Subscriptions that admins manage by hand until online payment exists.
 *
 * Dates are calendar days in the app's time zone (Asia/Damascus by default):
 * "ends on 30 November" means the end of that day in Syria, wherever the server runs.
 */
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final UserRepository userRepository;
    private final SubscriptionPaymentRepository paymentRepository;
    private final MongoTemplate mongoTemplate;

    @Value("${app.subscription.trial-days:30}")
    private int trialDays;

    @Value("${app.subscription.grace-days:3}")
    private int graceDays;

    @Value("${app.subscription.zone:Asia/Damascus}")
    private String zone;

    public LocalDate today() {
        return LocalDate.now(ZoneId.of(zone));
    }

    // ---------- status ----------

    // the last day with full access; the old free-text expireDate is read when the new field is empty
    public LocalDate endOf(User user) {
        if (user.getSubscriptionEndsAt() != null) {
            return user.getSubscriptionEndsAt();
        }
        String legacy = user.getExpireDate();
        if (legacy == null || legacy.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(legacy.trim().substring(0, Math.min(10, legacy.trim().length())));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public SubscriptionInfo infoOf(User user) {
        if (isAdmin(user)) {
            return new SubscriptionInfo(SubscriptionStatus.ACTIVE, null, null, null, true);
        }
        LocalDate end = endOf(user);
        if (end == null) {
            return new SubscriptionInfo(SubscriptionStatus.NONE, null, null, null, true);
        }
        LocalDate today = today();
        LocalDate graceEnd = end.plusDays(graceDays);
        SubscriptionStatus status;
        if (!today.isAfter(end)) {
            status = Boolean.TRUE.equals(user.getOnTrial()) ? SubscriptionStatus.TRIAL : SubscriptionStatus.ACTIVE;
        } else if (!today.isAfter(graceEnd)) {
            status = SubscriptionStatus.GRACE;
        } else {
            status = SubscriptionStatus.EXPIRED;
        }
        long daysLeft = ChronoUnit.DAYS.between(today, end);
        return new SubscriptionInfo(status, end, graceEnd, daysLeft, status != SubscriptionStatus.EXPIRED);
    }

    public boolean canWrite(User user) {
        return user != null && infoOf(user).canWrite();
    }

    // called once when a user registers
    public void startTrial(User user) {
        user.setSubscriptionEndsAt(today().plusDays(trialDays));
        user.setOnTrial(true);
    }

    // ---------- admin: extend ----------

    public AdminUserDetails extend(String userId, ExtendSubscriptionRequest request, String adminUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        validate(request);

        LocalDate previous = endOf(user);
        LocalDate newEnd;
        if (request.getUntil() != null) {
            newEnd = request.getUntil();
        } else {
            // renewing early keeps the remaining days; after the end it starts today
            LocalDate base = previous != null && previous.isAfter(today()) ? previous : today();
            newEnd = base.plusMonths(request.getMonths());
        }

        user.setSubscriptionEndsAt(newEnd);
        user.setOnTrial(false);
        userRepository.save(user);

        SubscriptionPayment payment = new SubscriptionPayment();
        payment.setUserId(user.getId());
        payment.setUsername(user.getUsername());
        payment.setPreviousEndsAt(previous);
        payment.setNewEndsAt(newEnd);
        payment.setMonths(request.getUntil() != null ? null : request.getMonths());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency() == null ? null : request.getCurrency().trim().toUpperCase());
        payment.setMethod(request.getMethod());
        payment.setReference(clean(request.getReference()));
        payment.setNote(clean(request.getNote()));
        payment.setRecordedBy(adminUsername);
        payment.setCreatedAt(Instant.now());
        paymentRepository.save(payment);

        return details(userId);
    }

    private void validate(ExtendSubscriptionRequest request) {
        boolean byMonths = request.getMonths() != null;
        boolean byDate = request.getUntil() != null;
        if (byMonths == byDate) {
            throw new IllegalArgumentException("Give either a number of months or a date");
        }
        if (byMonths && (request.getMonths() < 1 || request.getMonths() > 36)) {
            throw new IllegalArgumentException("Months must be between 1 and 36");
        }
        if (byDate && request.getUntil().isBefore(today())) {
            throw new IllegalArgumentException("The date can not be in the past");
        }
        if (request.getMethod() == null) {
            throw new IllegalArgumentException("Choose how it was paid");
        }
        if (request.getAmount() != null && request.getAmount().signum() < 0) {
            throw new IllegalArgumentException("The amount can not be negative");
        }
        if (request.getMethod() != PaymentMethod.FREE && request.getAmount() != null
                && (request.getCurrency() == null || request.getCurrency().isBlank())) {
            throw new IllegalArgumentException("Give the currency of the amount");
        }
    }

    // ---------- admin: users ----------

    public AdminUserDetails details(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new AdminUserDetails(summaryOf(user), paymentRepository.findAllByUserIdOrderByCreatedAtDesc(userId));
    }

    public AdminUserDetails setEnabled(String userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setEnabled(enabled);
        userRepository.save(user);
        return details(userId);
    }

    /**
     * Search by part of the email, name or phone (any case), with an optional filter:
     * TRIAL, EXPIRING (ends within 7 days), EXPIRED, NOT_SET. Newest users first.
     */
    public PageResult<AdminUserSummary> search(String text, String filter, int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 100));
        int safePage = Math.max(0, page);
        Query query = new Query(criteriaFor(text, filter));
        long total = mongoTemplate.count(query, User.class);

        query.with(Sort.by(Sort.Direction.DESC, "_id")).skip((long) safePage * safeSize).limit(safeSize);
        List<AdminUserSummary> items = mongoTemplate.find(query, User.class).stream().map(this::summaryOf).toList();
        return new PageResult<>(items, total, safePage, safeSize);
    }

    public SubscriptionStats stats() {
        return new SubscriptionStats(
                mongoTemplate.count(new Query(), User.class),
                mongoTemplate.count(new Query(criteriaFor(null, "TRIAL")), User.class),
                mongoTemplate.count(new Query(criteriaFor(null, "ACTIVE")), User.class),
                mongoTemplate.count(new Query(criteriaFor(null, "EXPIRING")), User.class),
                mongoTemplate.count(new Query(criteriaFor(null, "EXPIRED")), User.class),
                mongoTemplate.count(new Query(criteriaFor(null, "NOT_SET")), User.class)
        );
    }

    private Criteria criteriaFor(String text, String filter) {
        List<Criteria> parts = new ArrayList<>();
        if (text != null && !text.isBlank()) {
            String pattern = escapeRegex(text.trim());
            parts.add(new Criteria().orOperator(
                    Criteria.where("username").regex(pattern, "i"),
                    Criteria.where("firstName").regex(pattern, "i"),
                    Criteria.where("lastName").regex(pattern, "i"),
                    Criteria.where("phone").regex(pattern, "i")));
        }
        LocalDate today = today();
        if (filter != null) {
            switch (filter.toUpperCase()) {
                case "TRIAL" -> parts.add(Criteria.where("onTrial").is(true).and("subscriptionEndsAt").gte(today));
                case "ACTIVE" -> parts.add(Criteria.where("onTrial").ne(true).and("subscriptionEndsAt").gte(today));
                case "EXPIRING" -> parts.add(Criteria.where("subscriptionEndsAt").gte(today).lte(today.plusDays(7)));
                case "EXPIRED" -> parts.add(Criteria.where("subscriptionEndsAt").lt(today.minusDays(graceDays)));
                case "NOT_SET" -> parts.add(Criteria.where("subscriptionEndsAt").is(null));
                default -> { }
            }
        }
        return parts.isEmpty() ? new Criteria() : new Criteria().andOperator(parts.toArray(new Criteria[0]));
    }

    private AdminUserSummary summaryOf(User user) {
        return new AdminUserSummary(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                Boolean.TRUE.equals(user.getEnabled()),
                isAdmin(user),
                user.getCreatedAt(),
                infoOf(user));
    }

    private boolean isAdmin(User user) {
        return user.getRoles() != null && user.getRoles().contains(Role.ADMIN);
    }

    // the search text is matched literally: "+46" or "a.b" are not regex syntax
    private String escapeRegex(String text) {
        return text.replaceAll("[\\\\^$.|?*+()\\[\\]{}]", "\\\\$0");
    }

    private String clean(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}

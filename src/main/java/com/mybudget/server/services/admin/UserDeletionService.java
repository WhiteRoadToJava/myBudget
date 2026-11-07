package com.mybudget.server.services.admin;

import com.mybudget.server.dto.admin.UserDeletionReport;
import com.mybudget.server.exeptions.ConflictException;
import com.mybudget.server.exeptions.ResourceNotFoundException;
import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.AccountGroup;
import com.mybudget.server.modules.Debt;
import com.mybudget.server.modules.DebtMovement;
import com.mybudget.server.modules.Expense;
import com.mybudget.server.modules.Incomse;
import com.mybudget.server.modules.PaymentRequest;
import com.mybudget.server.modules.Schedule;
import com.mybudget.server.modules.SubscriptionPayment;
import com.mybudget.server.modules.TotalBalance;
import com.mybudget.server.modules.Transfer;
import com.mybudget.server.modules.User;
import com.mybudget.server.modules.VerificationToken;
import com.mybudget.server.modules.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Deletes a user and everything that belongs to them: accounts, transactions,
 * transfers, schedules, debts and their movements, balance snapshots,
 * payment requests, email tokens and uploaded receipt images.
 * The subscription payments are kept: they are the business's record of what was paid.
 *
 * Children are removed before the things they point to (movements before debts,
 * transfers before accounts), so a failure half-way never leaves records that
 * point to a missing account. The local database is the source: the hourly
 * Atlas sync removes the same documents from the copy.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeletionService {

    private final MongoTemplate mongoTemplate;

    @Value("${upload.dir}")
    private String uploadDir;

    public UserDeletionReport delete(String userId, String confirmEmail, User admin) {
        User user = mongoTemplate.findById(userId, User.class);
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }
        checkAllowed(user, confirmEmail, admin);

        // read the image names before the documents that hold them are gone
        List<String> images = new ArrayList<>();
        images.addAll(imageNames(Incomse.class, user, Incomse::getImage));
        images.addAll(imageNames(Expense.class, user, Expense::getImage));
        images.addAll(imageNames(Transfer.class, user, Transfer::getImage));

        Query byUser = new Query(Criteria.where("user").is(user));
        Map<String, Long> deleted = new LinkedHashMap<>();
        deleted.put("debtMovements", remove(byUser, DebtMovement.class));
        deleted.put("debts", remove(byUser, Debt.class));
        deleted.put("schedules", remove(byUser, Schedule.class));
        deleted.put("transfers", remove(byUser, Transfer.class));
        deleted.put("incomes", remove(byUser, Incomse.class));
        deleted.put("expenses", remove(byUser, Expense.class));
        deleted.put("accounts", remove(byUser, Account.class));
        deleted.put("accountGroups", remove(byUser, AccountGroup.class));
        deleted.put("totalBalance", remove(new Query(Criteria.where("user").is(user.getId())), TotalBalance.class));
        deleted.put("paymentRequests", remove(new Query(Criteria.where("userId").is(user.getId())), PaymentRequest.class));
        deleted.put("verificationTokens", remove(new Query(Criteria.where("username").is(user.getUsername())), VerificationToken.class));
        mongoTemplate.remove(user);
        deleted.put("users", 1L);

        // the payment history is the business's own record (income, accounting): it stays,
        // with the email it was paid under, so it can still be found after the account is gone
        long paymentsKept = mongoTemplate.count(new Query(Criteria.where("userId").is(user.getId())), SubscriptionPayment.class);

        int filesDeleted = deleteFiles(images);
        // who deleted whom, without keeping the user's data
        log.info("User {} deleted by admin {}: {} , images={}", user.getId(), admin.getUsername(), deleted, filesDeleted);
        return new UserDeletionReport(user.getUsername(), deleted, filesDeleted, paymentsKept);
    }

    private void checkAllowed(User user, String confirmEmail, User admin) {
        if (confirmEmail == null || !confirmEmail.trim().toLowerCase(Locale.ROOT)
                .equals(user.getUsername().toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Type the user's email to confirm");
        }
        if (Objects.equals(user.getId(), admin.getId())) {
            throw new ConflictException("You can not delete your own account");
        }
        boolean isAdmin = user.getRoles() != null && user.getRoles().contains(Role.ADMIN);
        if (isAdmin) {
            long admins = mongoTemplate.count(new Query(Criteria.where("roles").is(Role.ADMIN)), User.class);
            if (admins <= 1) {
                throw new ConflictException("The last admin can not be deleted");
            }
        }
    }

    private long remove(Query query, Class<?> type) {
        return mongoTemplate.remove(query, type).getDeletedCount();
    }

    private <T> List<String> imageNames(Class<T> type, User user, java.util.function.Function<T, Map<String, String>> image) {
        return mongoTemplate.find(new Query(Criteria.where("user").is(user)), type).stream()
                .map(image)
                .filter(Objects::nonNull)
                .map(m -> m.get("filename"))
                .filter(Objects::nonNull)
                .toList();
    }

    // only plain file names inside the upload folder: a stored "../x" can not reach other files
    private int deleteFiles(List<String> names) {
        Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
        int count = 0;
        for (String name : names) {
            Path file = base.resolve(name).normalize();
            if (!file.getParent().equals(base)) {
                log.warn("Skipped image outside the upload folder: {}", name);
                continue;
            }
            try {
                if (Files.deleteIfExists(file)) count++;
            } catch (IOException e) {
                log.warn("Could not delete image {}: {}", name, e.getMessage());
            }
        }
        return count;
    }
}

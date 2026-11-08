package com.mybudget.server.services.schedule;

import com.mybudget.server.services.account.AccountBalances;

import com.mybudget.server.dto.expense.ExpenseRequest;
import com.mybudget.server.dto.incomse.IncomseRequset;
import com.mybudget.server.dto.schedule.ScheduleRequest;
import com.mybudget.server.dto.schedule.ScheduleResponse;
import com.mybudget.server.dto.schedule.ScheduleUpdateRequest;
import com.mybudget.server.dto.transfer.TransferRequest;
import com.mybudget.server.exeptions.ResourceNotFoundException;
import com.mybudget.server.exeptions.UnauthorizedException;
import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.Schedule;
import com.mybudget.server.modules.User;
import com.mybudget.server.modules.enums.ScheduleInterval;
import com.mybudget.server.modules.enums.TransactionType;
import com.mybudget.server.repositories.AccountRepository;
import com.mybudget.server.repositories.ScheduleRepository;
import com.mybudget.server.services.ExpenseService;
import com.mybudget.server.services.IncomseService;
import com.mybudget.server.services.TransferService;
import com.mybudget.server.services.debt.DebtService;
import com.mybudget.server.services.subscription.SubscriptionService;
import com.mybudget.server.util.UserUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final IncomseService incomseService;
    private final ExpenseService expenseService;
    private final TransferService transferService;
    private final AccountRepository accountRepository;
    private final UserUtils userUtils;
    private final ScheduleMapper scheduleMapper;
    private final DebtService debtService;
    private final SubscriptionService subscriptionService;


    public ScheduleResponse executeSchedule(ScheduleRequest request) {
        User currentUser = userUtils.getCurrentAuthenticatedUser();

        // reject an invalid schedule now, instead of letting it fail silently at 08:00
        validateNewSchedule(request);
        boolean isTransfer = request.getTransactionTypes().contains(TransactionType.TRANSFER);
        boolean isDebtPayment = request.getTransactionTypes().contains(TransactionType.DEBT_PAYMENT);
        Account sourceAccount = findOwnedAccount(request.getSourceAccountId(), currentUser);
        Account destinationAccount = sourceAccount;
        // a debt payment is in the debt's currency; anything else in the chosen currency of the account
        String sourceCurrency = isDebtPayment
                ? debtService.checkScheduledPayment(request.getDebtId(), sourceAccount.getId(), currentUser)
                : AccountBalances.resolve(sourceAccount, request.getSourceCurrency());
        String destinationCurrency = sourceCurrency;
        if (isTransfer) {
            destinationAccount = findOwnedAccount(request.getDestinationAccountId(), currentUser);
            destinationCurrency = AccountBalances.resolve(destinationAccount, request.getDestinationCurrency());
            // the same account is fine in two currencies (an exchange), not twice the same pocket
            if (destinationAccount.getId().equals(sourceAccount.getId()) && destinationCurrency.equals(sourceCurrency)) {
                throw new IllegalArgumentException("A transfer needs two different accounts or two different currencies");
            }
        }

        Schedule schedule = Schedule.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(isDebtPayment ? "Debt" : request.getCategory())
                .debtId(isDebtPayment ? request.getDebtId() : null)
                .sourceAccount(sourceAccount)
                .destinationAccount(destinationAccount)
                .sourceCurrency(sourceCurrency)
                .destinationCurrency(destinationCurrency)
                .transactionTypes(request.getTransactionTypes())
                .scheduleIntervals(request.getScheduleIntervals())
                .amountSend(request.getAmountSend())
                .nextExecutionDate(request.getNextExecutionDate())
                .isActive(true)
                .user(currentUser)
                .build();

        if (isTransfer) {
            applyTransferAmounts(schedule, request.getExChangeRate());
        }

        return mapToResponse(scheduleRepository.save(schedule));
    }

    public ScheduleResponse updateSchedule(String scheduleId, ScheduleUpdateRequest request) {
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        Schedule schedule = findOwnedSchedule(scheduleId, currentUser);

        // validate first, then apply: a bad request changes nothing
        if (request.getScheduleIntervalSet() != null) {
            requireOneInterval(request.getScheduleIntervalSet());
        }
        if (request.getAmountSend() != null) {
            requirePositive(request.getAmountSend(), "Amount");
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            schedule.setName(request.getName());
        }
        if (request.getDescription() != null) {
            schedule.setDescription(request.getDescription());
        }
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            schedule.setCategory(request.getCategory());
        }
        if (request.getScheduleIntervalSet() != null) {
            schedule.setScheduleIntervals(request.getScheduleIntervalSet());
        }
        if (request.getAmountSend() != null) {
            schedule.setAmountSend(request.getAmountSend());
        }
        if (isTransfer(schedule)) {
            // the edit form sends 0 when the rate is unknown: keep the stored one then
            BigDecimal rate = isPositive(request.getExChangeRate())
                    ? request.getExChangeRate()
                    : schedule.getExChangeRate();
            applyTransferAmounts(schedule, rate);
        }
        if (request.getNextExecutionDate() != null) {
            schedule.setNextExecutionDate(request.getNextExecutionDate());
        }
        if (request.getActive() != null) {
            schedule.setActive(request.getActive());
        }

        return mapToResponse(scheduleRepository.save(schedule));
    }


    @Scheduled(cron = "${app.schedule.cron:0 0 8 * * *}")
    private void runDailySchedules() {
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);

        // everything due today or earlier: a date stored at exactly 00:00 is included,
        // and a day the server missed is picked up on the next run
        List<Schedule> dueSchedules = scheduleRepository.findByNextExecutionDateLessThanEqualAndIsActiveTrue(endOfDay);
        for (Schedule schedule : dueSchedules) {
            try {
                // read-only users: nothing runs; the occurrence is skipped, not caught up later
                if (!subscriptionService.canWrite(schedule.getUser())) {
                    updateNextExecutionDate(schedule);
                    log.info("Skipped schedule id={}: the user's subscription has ended", schedule.getId());
                    continue;
                }
                processSchedule(schedule);
            } catch (Exception e) {
                // one failing schedule must not stop the others
                log.error("Failed to process schedule id={}: {}", schedule.getId(), e.getMessage());
            }
        }
    }


    private void processSchedule(Schedule schedule) {
        TransactionType type = schedule.getTransactionTypes().iterator().next();
        User user = schedule.getUser();
        switch (type) {
            case INCOMSE -> {
                IncomseRequset request = mapToIncomeRequest(schedule);
                incomseService.excuteIncomse(request, user);
                updateNextExecutionDate(schedule);
            }
            case EXPENSE -> {
                ExpenseRequest request = mapToExpenseRequest(schedule);
                expenseService.addExpense(request, user);
                updateNextExecutionDate(schedule);
            }
            case TRANSFER -> {
                TransferRequest request = mapToTransferRequest(schedule);
                transferService.executeTransfer(request, user);
                updateNextExecutionDate(schedule);
            }
            case DEBT_PAYMENT -> processDebtPayment(schedule, user);
        }
    }

    private void processDebtPayment(Schedule schedule, User user) {
        if (schedule.getSourceAccount() == null) {
            stopSchedule(schedule);
            log.warn("Debt payment schedule id={} stopped: its account was deleted", schedule.getId());
            return;
        }
        DebtService.ScheduledPayment result;
        try {
            result = debtService.payScheduled(
                    schedule.getDebtId(), schedule.getSourceAccount().getId(), schedule.getAmountSend(), user
            );
        } catch (ResourceNotFoundException | IllegalArgumentException e) {
            stopSchedule(schedule);
            log.warn("Debt payment schedule id={} stopped : {}", schedule.getId(), e.getMessage());
            return;
        }
        if (result == DebtService.ScheduledPayment.PAID) {
            updateNextExecutionDate(schedule);
        } else {
            stopSchedule(schedule);
        }
    }

    private void stopSchedule(Schedule schedule) {
        schedule.setActive(false);
        scheduleRepository.save(schedule);
    }

    private void updateNextExecutionDate(Schedule schedule) {
        ScheduleInterval interval = schedule.getScheduleIntervals().iterator().next();

        LocalDateTime next = switch (interval) {
            case DAILY -> schedule.getNextExecutionDate().plusDays(1);
            case WEEKLY -> schedule.getNextExecutionDate().plusWeeks(1);
            case MONTHLY -> schedule.getNextExecutionDate().plusMonths(1);
            case YEARLY -> schedule.getNextExecutionDate().plusYears(1);
        };
        schedule.setNextExecutionDate(next);
        scheduleRepository.save(schedule);
    }


    public List<ScheduleResponse> getAllSchedules() {
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        if (currentUser == null) throw new UnauthorizedException("User not found");
        String userId = currentUser.getId();
        List<Schedule> schedules = scheduleRepository.findAllByUser(userId);
        return schedules.stream().map(this::mapToResponse).toList();
    }


    public void deleteSchedule(String scheduleId) {
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        Schedule schedule = findOwnedSchedule(scheduleId, currentUser);
        scheduleRepository.delete(schedule);
    }


    // ---------- validation ----------

    private void validateNewSchedule(ScheduleRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        Set<TransactionType> types = request.getTransactionTypes();
        if (types == null || types.size() != 1 || types.contains(null)) {
            throw new IllegalArgumentException("Exactly one transaction type is required");
        }
        requireOneInterval(request.getScheduleIntervals());
        requirePositive(request.getAmountSend(), "Amount");
        if (request.getNextExecutionDate() == null) {
            throw new IllegalArgumentException("Next execution date is required");
        }
        if (types.contains(TransactionType.TRANSFER)
                && (request.getDestinationAccountId() == null || request.getDestinationAccountId().isBlank())) {
            throw new IllegalArgumentException("A transfer needs a destination account");
        }
        if (types.contains(TransactionType.DEBT_PAYMENT)
                && (request.getDebtId() == null || request.getDebtId().isBlank())) {
            throw new IllegalArgumentException("A debt payment needs a debt");
        }
    }


    private void requireOneInterval(Set<ScheduleInterval> intervals) {
        if (intervals == null || intervals.size() != 1 || intervals.contains(null)) {
            throw new IllegalArgumentException("Exactly one schedule interval is required");
        }
    }

    private void requirePositive(BigDecimal value, String field) {
        if (!isPositive(value)) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean isTransfer(Schedule schedule) {
        return schedule.getTransactionTypes() != null
                && schedule.getTransactionTypes().contains(TransactionType.TRANSFER);
    }

    // Same currency: rate 1. Different currencies: the client must send a rate.
    // The received amount is always derived from the rate, like TransferService does.
    private void applyTransferAmounts(Schedule schedule, BigDecimal requestedRate) {
        BigDecimal rate = requestedRate;
        if (!isPositive(rate)) {
            String from = schedule.getSourceAccount() != null ? schedule.getSourceAccount().getCurrency() : null;
            String to = schedule.getDestinationAccount() != null ? schedule.getDestinationAccount().getCurrency() : null;
            if (from != null && Objects.equals(from, to)) {
                rate = BigDecimal.ONE;
            } else {
                throw new IllegalArgumentException("Exchange rate is required for accounts with different currencies");
            }
        }
        schedule.setExChangeRate(rate);
        schedule.setAmountReceived(schedule.getAmountSend().multiply(rate).setScale(2, RoundingMode.HALF_UP));
    }


    // ---------- mapping and lookups ----------

    private ScheduleResponse mapToResponse(Schedule schedule) {
        return scheduleMapper.mapToResponse(schedule);
    }

    private IncomseRequset mapToIncomeRequest(Schedule schedule) {
        return scheduleMapper.mapToIncomstRequest(schedule);
    }

    private ExpenseRequest mapToExpenseRequest(Schedule schedule) {
        return scheduleMapper.mapToExpenseRequest(schedule);
    }

    private TransferRequest mapToTransferRequest(Schedule schedule) {
        return scheduleMapper.mapToTransferRequest(schedule);
    }

    private Account findOwnedAccount(String accountId, User user) {
        if (accountId == null || accountId.isBlank()) {
            throw new ResourceNotFoundException("Account not found");
        }
        Account account = accountRepository.findByIdAndUser(accountId, user);
        if (account == null) {
            throw new ResourceNotFoundException("Account not found");
        }
        return account;
    }

    private Schedule findOwnedSchedule(String scheduleId, User user) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        if (schedule.getUser() == null || !schedule.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Schedule not found");
        }
        return schedule;
    }
}
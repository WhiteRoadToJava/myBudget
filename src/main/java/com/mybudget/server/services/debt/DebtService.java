package com.mybudget.server.services.debt;


import com.mybudget.server.dto.debt.*;
import com.mybudget.server.exeptions.ResourceNotFoundException;
import com.mybudget.server.modules.*;
import com.mybudget.server.modules.enums.AccountStatus;
import com.mybudget.server.modules.enums.CashFlow;
import com.mybudget.server.modules.enums.InterestCompounding;
import com.mybudget.server.repositories.AccountRepository;
import com.mybudget.server.repositories.DebtRepository;
import com.mybudget.server.repositories.ExpenseRepository;
import com.mybudget.server.repositories.IncomseRepository;
import com.mybudget.server.repositories.DebtMovementRepository;

import com.mybudget.server.services.account.AccountService;
import com.mybudget.server.util.UserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DebtService {
    public static final String DEBT_CATEGORY = "Debt";

    private final UserUtils userUtils;
    private final DebtRepository debtRepository;
    private final DebtMovementRepository debtMovementRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final IncomseRepository incomseRepository;
    private final ExpenseRepository expenseRepository;


    public DebtResponse createDebt(DebtRequest request) {
        User user = userUtils.getCurrentAuthenticatedUser();
        validateDebtRequest(request);

        Debt debt = new Debt();
        debt.setUser(user);
        applySettings(debt, request);
        Debt saved = debtRepository.save(debt);

        return toDebtResponse(saved, calculate(saved, List.of()));
    }

    public List<DebtResponse> getAllDebts() {
        User user = userUtils.getCurrentAuthenticatedUser();
        List<DebtResponse> responses = new ArrayList<>();
        for (Debt debt : debtRepository.findByUserOrderByStartDateDesc(user)) {
            List<DebtMovement> movements = debtMovementRepository.findByDebtOrderByDateAscCreatedAtAsc(debt);
            responses.add(toDebtResponse(debt, calculate(debt, movements)));
        }
        return responses;
    }

    public DebtDetailsResponse getDebt(String debtId) {
        User user = userUtils.getCurrentAuthenticatedUser();
        return detailsOf(findDebt(debtId, user));
    }

    public DebtResponse updateDebt(String debtId, DebtRequest request) {
        User user = userUtils.getCurrentAuthenticatedUser();
        Debt debt = findDebt(debtId, user);
        validateDebtRequest(request);

        List<DebtMovement> movements = debtMovementRepository.findByDebtOrderByDateAscCreatedAtAsc(debt);
        if (!movements.isEmpty()) {
            if (request.getDirection() != debt.getDirection()) {
                throw new IllegalArgumentException("The direction can not be changed after movements were added");
            }
            if (!debt.getCurrency().equalsIgnoreCase(request.getCurrency().trim())) {
                throw new IllegalArgumentException("The currency can not be changed after movements were added");
            }
        }
        applySettings(debt, request);
        DebtCalculator.Result result = calculate(debt, movements);

        return toDebtResponse(debtRepository.save(debt), result);
    }

    public String deleteDebt(String debtId) {
        User user = userUtils.getCurrentAuthenticatedUser();
        Debt debt = findDebt(debtId, user);

        if (!debtMovementRepository.findByDebtOrderByDateAscCreatedAtAsc(debt).isEmpty()) {
            throw new IllegalArgumentException(
                    "Delete the movements of the debt first, so the money in your accounts is put back");
        }
        debtRepository.delete(debt);
        return "Debt deleted successfully";
    }


    //----------------------------------------------


    public DebtDetailsResponse addMovement(String debtId, DebtMovementRequest request) {
        User user = userUtils.getCurrentAuthenticatedUser();
        Debt debt = findDebt(debtId, user);
        validateMovementRequest(request);


        Account account = accountRepository.findByIdAndUser(request.getAccountId(), user);
        if (account == null) {
            throw new ResourceNotFoundException("Account not found");
        }
        checkAccountCanBeUsed(debt, account);

        DebtMovement movement = new DebtMovement();
        movement.setDebt(debt);
        movement.setUser(user);
        movement.setAccount(account);
        movement.setMovementType(request.getMovementType());
        movement.setAmount(toMoney(request.getAmount()));
        movement.setDate(request.getDate());
        movement.setDescription(request.getDescription());

        List<DebtMovement> history = new ArrayList<>(debtMovementRepository.findByDebtOrderByDateAscCreatedAtAsc(debt));
        history.add(movement);
        calculate(debt, history);

        CashFlow cashFlow = debt.getDirection().cashFlowOf(movement.getMovementType());
        accountService.applyCashFlow(account, cashFlow, movement.getAmount());
        try {
            createIncomeOrExpenseIfRequested(debt, movement, cashFlow);
            debtMovementRepository.save(movement);
        } catch (RuntimeException e) {
            deleteIncomeOrExpense(movement);
            accountService.revertCashFlow(account, cashFlow, movement.getAmount());
            throw e;
        }
        return detailsOf(debt);
    }

    public DebtDetailsResponse deleteMovement(String debtId, String movementId) {
        User user = userUtils.getCurrentAuthenticatedUser();
        Debt debt = findDebt(debtId, user);
        DebtMovement movement = debtMovementRepository.findByIdAndDebt(movementId, debt)
                .orElseThrow(() -> new ResourceNotFoundException("Movement is not founded"));

        List<DebtMovement> remaining = new ArrayList<>();
        for (DebtMovement other : debtMovementRepository.findByDebtOrderByDateAscCreatedAtAsc(debt)) {
            if (!other.getId().equals(movement.getId())) {
                remaining.add(other);
            }
        }
        calculate(debt, remaining);

        Account account = movement.getAccount();
        if (account != null) {
            CashFlow cashFlow = debt.getDirection().cashFlowOf(movement.getMovementType());
            accountService.revertCashFlow(account, cashFlow, movement.getAmount());
        }
        deleteIncomeOrExpense(movement);
        debtMovementRepository.delete(movement);

        return detailsOf(debt);
    }


    //-------------------------------------------------------
    private Debt findDebt(String debtId, User user) {
        return debtRepository.findByIdAndUser(debtId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Debt not found"));
    }
// check this function
    private void applySettings(Debt debt, DebtRequest request) {
        debt.setName(request.getName().trim());
        debt.setCounterparty(request.getCounterparty());
        debt.setDirection(request.getDirection());
        debt.setCurrency(request.getCurrency().trim());
        debt.setAnnualRate(request.getAnnualRate());
        debt.setStartDate(request.getStartDate());
        debt.setCompounding(request.getCompounding() == null ? InterestCompounding.NONE : request.getCompounding());
        debt.setCountInflowsAsIncome(request.isCountInflowsAsIncome());
        debt.setCountOutflowsAsExpense(request.isCountOutflowsAsExpense());
        debt.setNote(request.getNote());
    }

    private void checkAccountCanBeUsed(Debt debt, Account account) {
        if (account.getStatus() != null && account.getStatus().contains(AccountStatus.ARCHIVED)) {
            throw new IllegalArgumentException("The account is archived");
        }
        if (account.getCurrency().trim() == null || !account.getCurrency().trim().equalsIgnoreCase(debt.getCurrency())) {
            throw new IllegalArgumentException("The currency of the account (" + account.getCurrency()
                    + ") must be the same as the currency of the debt (" + debt.getCurrency() + ")");
        }
    }

    private void validateDebtRequest(DebtRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("The name of the debt is required");
        }
        if (request.getDirection() == null) {
            throw new IllegalArgumentException("The direction is required (OWED_BY_ME or OWED_TO_ME)");
        }
        if (request.getCurrency() == null || request.getCurrency().isBlank()) {
            throw new IllegalArgumentException("The currency is required");
        }
        if (Double.isNaN(request.getAnnualRate()) || request.getAnnualRate() < 0 || request.getAnnualRate() > 100) {
            throw new IllegalArgumentException("The interest rate must be between 0 and 100");
        }
        if (request.getStartDate() == null) {
            throw new IllegalArgumentException("The start date is required");
        }
        if (request.getStartDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("The start date can not be in the future");
        }
    }

    private void validateMovementRequest(DebtMovementRequest request) {
        if (request.getMovementType() == null) {
            throw new IllegalArgumentException("The movement type is required (DISBURSEMENT or REPAYMENT)");
        }
        if (Double.isNaN(request.getAmount()) || Double.isInfinite(request.getAmount()) || toMoney(request.getAmount()) <= 0) {
            throw new IllegalArgumentException("The amount must be greater than zero");
        }
        if (request.getDate() == null) {
            throw new IllegalArgumentException("The date is required");
        }
        if (request.getDate().isAfter(LocalDate.now())) {
            // money that has not moved yet must not change the accounts now (scheduled payments come later)
            throw new IllegalArgumentException("The date can not be in the future");
        }
        if (request.getAccountId() == null || request.getAccountId().isBlank()) {
            throw new IllegalArgumentException("The account is required");
        }
    }

    private DebtCalculator.Result calculate(Debt debt, List<DebtMovement> movements) {
        List<DebtCalculator.Movement> input = new ArrayList<>();
        for (DebtMovement movement : movements) {
            input.add(new DebtCalculator.Movement(
                    movement.getId(),
                    movement.getMovementType(),
                    movement.getDate(),
                    BigDecimal.valueOf(movement.getAmount())));
        }
        return DebtCalculator.calculate(
                debt.getStartDate(),
                BigDecimal.valueOf(debt.getAnnualRate()),
                debt.getCompounding(),
                input,
                LocalDate.now()
        );
    }

    private double toMoney(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }


    private DebtResponse toDebtResponse(Debt debt, DebtCalculator.Result result) {
        DebtSummaryResponse summary = new DebtSummaryResponse(
                result.principal().doubleValue(),
                result.accruedInterest().doubleValue(),
                result.totalOwed().doubleValue(),
                result.totalDisbursed().doubleValue(),
                result.totalRepaid().doubleValue(),
                result.interestPaid().doubleValue(),
                result.principalPaid().doubleValue(),
                LocalDate.now());
        return new DebtResponse(
                debt.getId(),
                debt.getName(),
                debt.getCounterparty(),
                debt.getDirection(),
                debt.getCurrency(),
                debt.getAnnualRate(),
                debt.getCompounding(),
                debt.getStartDate(),
                debt.isCountInflowsAsIncome(),
                debt.isCountOutflowsAsExpense(),
                debt.getNote(),
                summary);
    }

    private LocalDateTime dateTimeOf(LocalDate date) {
        return date.equals(LocalDate.now()) ? LocalDateTime.now() : date.atTime(LocalTime.NOON);
    }

    private DebtDetailsResponse detailsOf(Debt debt) {
        List<DebtMovement> movements = debtMovementRepository.findByDebtOrderByDateAscCreatedAtAsc(debt);
        DebtCalculator.Result result = calculate(debt, movements);

        Map<String, DebtCalculator.Line> linesById = new HashMap<>();
        for (DebtCalculator.Line line : result.lines()) {
            linesById.put(line.movement().ref(), line);
        }

        List<DebtMovementResponse> responses = new ArrayList<>();
        for (DebtMovement movement : movements) {
            responses.add(toMovementResponse(debt, movement, linesById.get(movement.getId())));
        }
        return new DebtDetailsResponse(toDebtResponse(debt, result), responses);
    }

    private DebtMovementResponse toMovementResponse(Debt debt, DebtMovement movement, DebtCalculator.Line line) {
        Account account = movement.getAccount();
        return new DebtMovementResponse(
                movement.getId(),
                movement.getMovementType(),
                debt.getDirection().cashFlowOf(movement.getMovementType()),
                movement.getAmount(),
                movement.getDate(),
                account == null ? null : account.getId(),
                account == null ? null : account.getName(),
                movement.getDescription(),
                line == null ? 0.0 : line.interestPart().doubleValue(),
                line == null ? 0.0 : line.principalPart().doubleValue(),
                movement.getMirroredTransactionId() != null);
    }

    private void createIncomeOrExpenseIfRequested(Debt debt, DebtMovement movement, CashFlow cashFlow){
        LocalDateTime when = dateTimeOf(movement.getDate());


        if (cashFlow == CashFlow.IN && debt.isCountInflowsAsIncome()) {
            Incomse incomse = new Incomse();
            incomse.setAmount(movement.getAmount());
            incomse.setCategory(DEBT_CATEGORY);
            incomse.setUser(movement.getUser());
            incomse.setAccount(movement.getAccount());
            incomse = incomseRepository.save(incomse);
            // auditing stamps "now" on the first save, so the real date is set with a second save
            incomse.setCreatedAt(when);
            incomse = incomseRepository.save(incomse);
            movement.setMirroredTransactionId(incomse.getId());
            movement.setMirroredTransactionType("incomse");

        } else if (cashFlow == CashFlow.OUT && debt.isCountOutflowsAsExpense()) {
            Expense expense = new Expense();
            expense.setAmount(movement.getAmount());
            expense.setCategory(DEBT_CATEGORY);
            expense.setUser(movement.getUser());
            expense.setAccount(movement.getAccount());
            expense = expenseRepository.save(expense);
            expense.setCreatedAt(when);
            expense = expenseRepository.save(expense);
            movement.setMirroredTransactionId(expense.getId());
            movement.setMirroredTransactionType("expense");
        }
    }


    private void deleteIncomeOrExpense(DebtMovement movement){
        String id = movement.getMirroredTransactionId();
        if (id == null){
            return;
        }
        if ("incomse".equals(movement.getMirroredTransactionType())){
            incomseRepository.deleteById(id);
        }else expenseRepository.deleteById(id);
    }
}

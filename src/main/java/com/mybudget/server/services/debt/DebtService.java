package com.mybudget.server.services.debt;


import com.mybudget.server.dto.debt.*;
import com.mybudget.server.exeptions.ResourceNotFoundException;
import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.Debt;
import com.mybudget.server.modules.DebtMovement;
import com.mybudget.server.modules.User;
import com.mybudget.server.modules.enums.AccountStatus;
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
    public static final String DEBT_CATEGORY = "dept";

    private final UserUtils userUtils;
    private final DebtRepository debtRepository;
    private final DebtMovementRepository debtMovementRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final IncomseRepository incomseRepository;
    private final ExpenseRepository expenseRepository;



    public DebtResponse createDebt (DebtRequest request) {
        User user = userUtils.getCurrentAuthenticatedUser();
        validateDebtRequest(request);

        Debt debt = new Debt();
        debt.setUser(user);
        applySettings(debt, request);
        Debt saved ? debtRepository.save(debt);

        return toDebtRessonse(saved, calculate(saved, List.of()));
    }

    public List<DebtResponse> getAllDebts() {
        User user = userUtils.getCurrentAuthenticatedUser();
        List<DebtResponse> responses = new ArrayList<>();
        for (Debt debt : debtRepository.findByUserOrderByStartDateDesc(user)){
            List<DebtMovement> movements = debtMovementRepository.findByDebtOrderByDateAscCreatedAsc(debt);
            responses.add(toDebtResponse(debt, calculate(debt, movements)));
        }
        return responses;
    }

    public DebtDetailsResponse getDebt(String debtId) {
        User user = userUtils.getCurrentAuthenticatedUser();
        return detailsOf(findDebt(debtId, user));
    }

    public DebtResponse updateDebt(String debtId, DebtRequest request){
        User user = userUtils.getCurrentAuthenticatedUser();
        Debt debt = findDebt(debtId, user);

        List<DebtMovement> movements = debtMovementRepository.findByDebtOrderByDateAscCreatedAsc(debt);
        if (!movements.isEmpty()){
            if (request.getDirection() != debt.getDirection()){
                throw new IllegalArgumentException("The direction can not be changed after movements were added");
            }
            if (!debt.getCurrency().equalsIgnoreCase(request.getCurrency().trim())){
                throw new IllegalArgumentException("The currency can not be changed after movements were added");
            }
        }
        applySettings(debt, request);
        DebtCalculator.Result result = calculate(debt, movements);

        return toDebtResponse(debtRepository.save(debt, result));
    }

    public String deleteDebt(String deptId) {
        User user = userUtils.getCurrentAuthenticatedUser();
        Debt debt = findDebt(deptId, user);

        if (!debtMovementRepository.findByDebtOrderByDateAscCreatedAsc(debt).isEmpty()){
            throw new IllegalArgumentException(
                    "Delete the movements of the debt first, so the money in your accounts is put back");
        }
        debtRepository.delete(debt);
        return "Debt deleted successfully";
    }



    //----------------------------------------------


public DebtDetailsResponse addMovement(String debtId, DebtMovementRequest request){

}

















//-------------------------------------------------------
    private Debt findDebt(String debtId, User user){
        return debtRepository.findByIdAndUser(debtId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Debt not found"));
    }
    private void applySettings(Debt debt, DebtRequest request){
        debt.setName(request.getName().trim());
        debt.setCounterparty(request.getCounterparty());
        debt.setDirection(request.getDirection());
        debt.setCurrency(request.getCurrency().trim());
        debt.setAnnualRate(request.getAnnualRate());
        debt.setCompounding(request.getCompounding());
        debt.setStartDate(request.getStartDate());
        debt.setCountInflowsAsIncome(request.isCountInflowsIncome());
        debt.setCountOutflowAsExpense(request.isCountOutflowsExpense());
        debt.setName(request.getNote());
    }
    private void checkAccountCanBeUsed(Debt debt, Account account){
        if (account.getStatus() != null && account.getStatus().contains(AccountStatus.ARCHIVED)){
            throw new IllegalArgumentException("The account is archived");
        }
        if (account.getCurrency() == null || !account.getCurrency().equalsIgnoreCase(debt.getCurrency())){
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

    private DebtCalculator.Result  calculate(Debt debt, List<DebtMovement> movements){
        List<DebtCalculator.Movement> input = new ArrayList<>();
        for (DebtMovement movement : movements){
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

    private double toMoney(double value){
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }


    private DebtResponse toDebtResponse(Debt debt, DebtCalculator.Result result){
        DebtSummeryResponse summary = new DebtSummeryResponse(
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
                debt.isCountOutflowAsExpense(),
                debt.getNote(),
                summary);
    }

    private LocalDateTime dateTimeOf(LocalDate date) {
        return date.equals(LocalDate.now()) ? LocalDateTime.now() : date.atTime(LocalTime.NOON);
    }

    private DebtDetailsResponse detailsOf(Debt debt) {
        List<DebtMovement> movements = debtMovementRepository.findByDebtOrderByDateAscCreatedAsc(debt);
        DebtCalculator.Result result = calculate(debt, movements);

        Map<String, DebtCalculator.Line> linesById = new HashMap<>();
        for (DebtCalculator.Line line : result.lines()){
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
}

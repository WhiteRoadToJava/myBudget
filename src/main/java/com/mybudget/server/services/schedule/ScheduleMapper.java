package com.mybudget.server.services.schedule;

import com.mybudget.server.modules.Account;
import com.mybudget.server.services.account.AccountBalances;
import com.mybudget.server.dto.expense.ExpenseRequest;
import com.mybudget.server.dto.incomse.IncomseRequset;
import com.mybudget.server.dto.schedule.ScheduleResponse;
import com.mybudget.server.dto.transfer.TransferRequest;
import com.mybudget.server.modules.Schedule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
@Component
@RequiredArgsConstructor
public class ScheduleMapper {

    // a schedule from before the migration has no currency: its account's first one
    private String currencyOr(String currency, Account account) {
        if (currency != null || account == null) {
            return currency;
        }
        return AccountBalances.primaryCurrency(account);
    }

    private Double balanceOf(Account account, String currency) {
        return AccountBalances.find(account, currency).map(p -> p.getTotalBalance()).orElse(null);
    }

    public ScheduleResponse mapToResponse(Schedule schedule) {
        ScheduleResponse response = new ScheduleResponse();
        response.setId(schedule.getId());
        response.setName(schedule.getName());
        response.setDescription(schedule.getDescription());
        response.setCategory(schedule.getCategory());
        response.setTransactionType(schedule.getTransactionTypes().iterator().next());
        response.setScheduleInterval(schedule.getScheduleIntervals().iterator().next());
        response.setAmountSend(schedule.getAmountSend());
        response.setExChangeRate(schedule.getExChangeRate());
        response.setAmountReceived(schedule.getAmountReceived());
        response.setDebtId(schedule.getDebtId());
        response.setSourceCurrency(currencyOr(schedule.getSourceCurrency(), schedule.getSourceAccount()));
        response.setDestinationCurrency(currencyOr(schedule.getDestinationCurrency(), schedule.getDestinationAccount()));
        response.setExecutionDate(schedule.getNextExecutionDate());
        response.setActive(schedule.isActive());
        response.setCreatedAt(schedule.getCreatedAt());
        response.setUpdatedAt(schedule.getUpdatedAt());

        // Map source account
        if (schedule.getSourceAccount() != null) {
            ScheduleResponse.AccountSummary source = new ScheduleResponse.AccountSummary();
            source.setId(schedule.getSourceAccount().getId());
            source.setName(schedule.getSourceAccount().getName());
            source.setCurrency(response.getSourceCurrency());
            source.setTotalBalance(balanceOf(schedule.getSourceAccount(), response.getSourceCurrency()));
            response.setSourceAccount(source);
        }

        // Map destination account
        if (schedule.getDestinationAccount() != null) {
            ScheduleResponse.AccountSummary destination = new ScheduleResponse.AccountSummary();
            destination.setId(schedule.getDestinationAccount().getId());
            destination.setName(schedule.getDestinationAccount().getName());
            destination.setCurrency(response.getDestinationCurrency());
            destination.setTotalBalance(balanceOf(schedule.getDestinationAccount(), response.getDestinationCurrency()));
            response.setDestinationAccount(destination);
        }

        // Map user
        if (schedule.getUser() != null) {
            ScheduleResponse.UserSummary userSummary = new ScheduleResponse.UserSummary();
            userSummary.setId(schedule.getUser().getId());
            userSummary.setName(schedule.getUser().getFirstName() + " " + schedule.getUser().getLastName());
            userSummary.setEmail(schedule.getUser().getUsername());
            response.setCreatedBy(userSummary);
        }

        return response;
    }

    public IncomseRequset mapToIncomstRequest(Schedule schedule){
        return new IncomseRequset(
                schedule.getAmountSend().doubleValue(),
                schedule.getCategory(),
                schedule.getUser(),
                schedule.getSourceAccount(),
                schedule.getDescription(),
                null,
                schedule.getSourceCurrency()
        );
    }
    public ExpenseRequest mapToExpenseRequest(Schedule schedule){
        return new ExpenseRequest(
                schedule.getAmountSend().doubleValue(),
                schedule.getCategory(),
                schedule.getUser(),
                schedule.getSourceAccount(),
                schedule.getDescription(),
                schedule.getCreatedAt(),
                null,
                schedule.getSourceCurrency()
        );
    }
    // Setters instead of the all-args constructor: TransferRequest has more fields now
    // (the fees), and a positional constructor breaks every time a field is added.
    // Scheduled transfers have no fees yet, so those fields stay null (= no fee).
    public TransferRequest mapToTransferRequest(Schedule schedule){
        TransferRequest request = new TransferRequest();
        request.setSourceAccount(schedule.getSourceAccount());
        request.setDestinationAccount(schedule.getDestinationAccount());
        request.setAmountSent(schedule.getAmountSend().doubleValue());
        request.setExChangeRate(schedule.getExChangeRate().doubleValue());
        request.setAmountReceived(schedule.getAmountReceived().doubleValue());
        request.setDescription(schedule.getDescription());
        request.setSourceCurrency(schedule.getSourceCurrency());
        request.setDestinationCurrency(schedule.getDestinationCurrency());
        return request;
    }
}

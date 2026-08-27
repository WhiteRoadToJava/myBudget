package com.mybudget.server.services.schedule;

import com.mybudget.server.dto.expense.ExpenseRequest;
import com.mybudget.server.dto.income.IncomeRequest;
import com.mybudget.server.dto.schedule.ScheduleResponse;
import com.mybudget.server.dto.transfer.TransferRequest;
import com.mybudget.server.modules.Schedule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
@Component
@RequiredArgsConstructor
public class ScheduleMapper {

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
        response.setExecutionDate(schedule.getNextExecutionDate());
        response.setActive(schedule.isActive());
        response.setCreatedAt(schedule.getCreatedAt());
        response.setUpdatedAt(schedule.getUpdatedAt());

        // Map source account
        if (schedule.getSourceAccount() != null) {
            ScheduleResponse.AccountSummary source = new ScheduleResponse.AccountSummary();
            source.setId(schedule.getSourceAccount().getId());
            source.setName(schedule.getSourceAccount().getName());
            source.setTotalBalance(schedule.getSourceAccount().getTotalBalance());
            response.setSourceAccount(source);
        }

        // Map destination account
        if (schedule.getDestinationAccount() != null) {
            ScheduleResponse.AccountSummary destination = new ScheduleResponse.AccountSummary();
            destination.setId(schedule.getDestinationAccount().getId());
            destination.setName(schedule.getDestinationAccount().getName());
            destination.setTotalBalance(schedule.getDestinationAccount().getTotalBalance());
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

    public IncomeRequest mapToIncomstRequest(Schedule schedule){
        return new IncomeRequest(
                schedule.getAmountSend().doubleValue(),
                schedule.getCategory(),
                schedule.getUser(),
                schedule.getSourceAccount(),
                schedule.getDescription(),
                null

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
                null
        );
    }
    public TransferRequest mapToTransferRequest(Schedule schedule){
        return new TransferRequest(
                schedule.getSourceAccount(),
                schedule.getDestinationAccount(),
                schedule.getAmountSend().doubleValue(),
                schedule.getExChangeRate().doubleValue(),
                schedule.getAmountReceived().doubleValue(),
                schedule.getDescription(),
                schedule.getCreatedAt(),
                null
        );
    }
}

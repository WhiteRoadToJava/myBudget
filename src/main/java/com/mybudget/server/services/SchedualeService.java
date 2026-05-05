package com.mybudget.server.services;


import com.mybudget.server.dto.expense.ExpenseRequset;
import com.mybudget.server.dto.incomse.IncomseRequset;
import com.mybudget.server.dto.scheduale.SechedualeRequest;
import com.mybudget.server.dto.scheduale.SechedualeResponse;
import com.mybudget.server.dto.transfer.TransferRequest;
import com.mybudget.server.exeptions.ResourceNotFoundException;
import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.Scheduale;
import com.mybudget.server.modules.User;
import com.mybudget.server.modules.enums.ScheduleInterval;
import com.mybudget.server.modules.enums.TransactionType;
import com.mybudget.server.repositories.AccountRepository;
import com.mybudget.server.repositories.SechedualeRepository;
import com.mybudget.server.util.UserUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedualeService {
    private final SechedualeRepository sechedualeRepository;
    private final IncomseService incomseService;
    private final ExpenseService expenseService;
    private final TransferService transferService;
    private final AccountRepository accountRepository;
    private final UserUtils userUtils;





    public SechedualeResponse executeSchedule(SechedualeRequest request) {
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        System.out.println(request.getAmountReceived());
        // Fetch source account
        Account sourceAccount = accountRepository.findById(request.getSourceAccountId())
                .orElseThrow(() -> new RuntimeException("Source account not found"));

        // Fetch destination account (optional)
        Account destinationAccount;
        if (request.getDestinationAccountId() != null && !request.getDestinationAccountId().isEmpty()) {
            destinationAccount = accountRepository.findById(request.getDestinationAccountId())
                    .orElseThrow(() -> new RuntimeException("Destination account not found"));
        } else {
            destinationAccount = sourceAccount;
        }
        // Build schedule
        Scheduale schedule = Scheduale.builder()
                .name(request.getName())
                .description(request.getDescription())
                .sourceAccount(sourceAccount)
                .destinationAccount(destinationAccount)
                .transactionTypes(request.getTransactionTypes())
                .scheduleIntervals(request.getScheduleIntervals())
                .amountSend(request.getAmountSend())
                .nextExecutionDate(request.getNextExecutionDate())
                .isActive(true)
                .user(currentUser)
                .build();

        Scheduale saved = sechedualeRepository.save(schedule);

        return mapToResponse(saved);
    }



    @Scheduled(cron = "0 */1 * * * *")
    private void runDailySchedules(){
        System.out.println("Running daily schedules");
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59,59);

        List<Scheduale> dueTodaySchedules = sechedualeRepository.findByNextExecutionDateBetweenAndIsActiveTrue(startOfDay, endOfDay);
        System.out.println(dueTodaySchedules.size());
        for (Scheduale schedule : dueTodaySchedules) {
            try {
                processSechedule(schedule);
            }catch (Exception e){
                throw new ResourceNotFoundException("Failed to process schedule id={}, error={}"+  schedule.getId()+ e.getMessage());
            }
        }
    }



private void processSechedule(Scheduale sechedule) {
    TransactionType type = sechedule.getTransactionTypes().iterator().next();
    User user = sechedule.getUser();
    switch (type) {
        case INCOMSE -> {
            IncomseRequset requset = mapToIncomstRequest(sechedule);
            incomseService.excuteIncomse(requset, user);
            updateNextExectionDate(sechedule);
        }
        case EXPENSE -> {
            ExpenseRequset requset = mapToExpenseRequest(sechedule);
            expenseService.addExpense(requset, user);
            updateNextExectionDate(sechedule);
        }
        case TRANSFER -> {
            TransferRequest request = mapToTransferRequest(sechedule);
            transferService.excuteTransfer(request, user);
            updateNextExectionDate(sechedule);
        }
    }
}

    private void updateNextExectionDate(Scheduale sechedule){
        ScheduleInterval interval = sechedule.getScheduleIntervals().iterator().next();

        LocalDateTime next = switch (interval) {
            case DAILY -> sechedule.getNextExecutionDate().plusDays(1);
            case WEEKLY -> sechedule.getNextExecutionDate().plusWeeks(1);
            case MONTHLY -> sechedule.getNextExecutionDate().plusMonths(1);
            case YEARLY -> sechedule.getNextExecutionDate().plusYears(1);
        };
        sechedule.setNextExecutionDate(next);
        sechedualeRepository.save(sechedule);
    }

















    private SechedualeResponse mapToResponse(Scheduale schedule) {
        SechedualeResponse response = new SechedualeResponse();
        response.setId(schedule.getId());
        response.setName(schedule.getName());
        response.setDescription(schedule.getDescription());
        response.setTransactionType(schedule.getTransactionTypes().iterator().next());
        response.setScheduleInterval(schedule.getScheduleIntervals().iterator().next());
        response.setAmount(schedule.getAmountSend());
        response.setExecutionDate(schedule.getNextExecutionDate());
        response.setActive(schedule.isActive());
        response.setCreatedAt(schedule.getCreatedAt());
        response.setUpdatedAt(schedule.getUpdatedAt());

        // Map source account
        if (schedule.getSourceAccount() != null) {
            SechedualeResponse.AccountSummary source = new SechedualeResponse.AccountSummary();
            source.setId(schedule.getSourceAccount().getId());
            source.setName(schedule.getSourceAccount().getName());
            source.setBalance(schedule.getSourceAccount().getBalance());
            response.setSourceAccount(source);
        }

        // Map destination account
        if (schedule.getDestinationAccount() != null) {
            SechedualeResponse.AccountSummary destination = new SechedualeResponse.AccountSummary();
            destination.setId(schedule.getDestinationAccount().getId());
            destination.setName(schedule.getDestinationAccount().getName());
            destination.setBalance(schedule.getDestinationAccount().getBalance());
            response.setDestinationAccount(destination);
        }

        // Map user
        if (schedule.getUser() != null) {
            SechedualeResponse.UserSummary userSummary = new SechedualeResponse.UserSummary();
            userSummary.setId(schedule.getUser().getId());
            userSummary.setName(schedule.getUser().getFirstName() + " " + schedule.getUser().getLastName());
            userSummary.setEmail(schedule.getUser().getUsername());
            response.setCreatedBy(userSummary);
        }

        return response;
    }




    private IncomseRequset mapToIncomstRequest(Scheduale secheduale){
        return new IncomseRequset(
                secheduale.getAmountSend().doubleValue(),
                secheduale.getCategory(),
                secheduale.getUser(),
                secheduale.getSourceAccount(),
                secheduale.getDescription()

        );
    }
    private ExpenseRequset  mapToExpenseRequest(Scheduale secheduale){
        return new ExpenseRequset(
                secheduale.getAmountSend().doubleValue(),
                secheduale.getCategory(),
                secheduale.getUser(),
                secheduale.getSourceAccount(),
                secheduale.getDescription()
        );
    }

    private TransferRequest mapToTransferRequest(Scheduale secheduale){
        return new TransferRequest(
                secheduale.getSourceAccount(),
                secheduale.getDestinationAccount(),
                secheduale.getAmountSend().doubleValue(),
                secheduale.getExChangeRate().doubleValue(),
                secheduale.getAmountReceived().doubleValue(),
                secheduale.getDescription()
        );
    }


}

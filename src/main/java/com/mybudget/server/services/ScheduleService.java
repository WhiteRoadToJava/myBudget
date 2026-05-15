package com.mybudget.server.services;


import com.mybudget.server.dto.expense.ExpenseRequset;
import com.mybudget.server.dto.incomse.IncomseRequset;
import com.mybudget.server.dto.schedule.ScheduleRequest;
import com.mybudget.server.dto.schedule.ScheduleResponse;
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
import com.mybudget.server.util.UserUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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





    public ScheduleResponse executeSchedule(ScheduleRequest request) {
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
        Schedule schedule = Schedule.builder()
                .name(request.getName())
                .description(request.getDescription())
                .sourceAccount(sourceAccount)
                .destinationAccount(destinationAccount)
                .transactionTypes(request.getTransactionTypes())
                .scheduleIntervals(request.getScheduleIntervals())
                .amountSend(request.getAmountSend())
                .nextExecutionDate(request.getNextExecutionDate())
                .exChangeRate(request.getExChangeRate())
                .amountReceived(request.getAmountReceived())
                .isActive(true)
                .user(currentUser)
                .build();

        Schedule saved = scheduleRepository.save(schedule);

        return mapToResponse(saved);
    }



    @Scheduled(cron = "0 0 8 * * *")
    private void runDailySchedules(){
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59,59);

        List<Schedule> dueTodaySchedules = scheduleRepository.findByNextExecutionDateBetweenAndIsActiveTrue(startOfDay, endOfDay);
        for (Schedule schedule : dueTodaySchedules) {
            try {
                processSchedule(schedule);
            }catch (Exception e){
                throw new ResourceNotFoundException("Failed to process schedule id={}, error={}"+  schedule.getId()+ e.getMessage());
            }
        }
    }



private void processSchedule(Schedule schedule) {
    TransactionType type = schedule.getTransactionTypes().iterator().next();
    User user = schedule.getUser();
    switch (type) {
        case INCOMSE -> {
            IncomseRequset requset = mapToIncomstRequest(schedule);
            incomseService.excuteIncomse(requset, user);
            updateNextExecutionDate(schedule);
        }
        case EXPENSE -> {
            ExpenseRequset requset = mapToExpenseRequest(schedule);
            expenseService.addExpense(requset, user);
            updateNextExecutionDate(schedule);
        }
        case TRANSFER -> {
            TransferRequest request = mapToTransferRequest(schedule);
            transferService.excuteTransfer(request, user);
            updateNextExecutionDate(schedule);
        }
    }
}

    private void updateNextExecutionDate(Schedule schedule){
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
        if(currentUser == null) throw new UnauthorizedException("User not found");
        String userId = currentUser.getId();
        List<Schedule> schedules = scheduleRepository.findAllByUser(userId);
        return schedules.stream().map(this::mapToResponse).toList();
    }















    private ScheduleResponse mapToResponse(Schedule schedule) {
        ScheduleResponse response = new ScheduleResponse();
        response.setId(schedule.getId());
        response.setName(schedule.getName());
        response.setDescription(schedule.getDescription());
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


    public void deleteSchedulae(String scheduleId){
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        String userId = currentUser.getId();
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Not Found"));
        if(userId .equals(schedule.getUser().getId())){
            scheduleRepository.delete(schedule);
        }
    }




    private IncomseRequset mapToIncomstRequest(Schedule schedule){
        return new IncomseRequset(
                schedule.getAmountSend().doubleValue(),
                schedule.getCategory(),
                schedule.getUser(),
                schedule.getSourceAccount(),
                schedule.getDescription()

        );
    }
    private ExpenseRequset  mapToExpenseRequest(Schedule schedule){
        return new ExpenseRequset(
                schedule.getAmountSend().doubleValue(),
                schedule.getCategory(),
                schedule.getUser(),
                schedule.getSourceAccount(),
                schedule.getDescription()
        );
    }

    private TransferRequest mapToTransferRequest(Schedule schedule){
        return new TransferRequest(
                schedule.getSourceAccount(),
                schedule.getDestinationAccount(),
                schedule.getAmountSend().doubleValue(),
                schedule.getExChangeRate().doubleValue(),
                schedule.getAmountReceived().doubleValue(),
                schedule.getDescription()
        );
    }


}

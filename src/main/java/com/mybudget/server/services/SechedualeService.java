package com.mybudget.server.services;


import com.mybudget.server.dto.scheduale.SechedualeRequest;
import com.mybudget.server.dto.scheduale.SechedualeResponse;
import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.Secheduale;
import com.mybudget.server.modules.User;
import com.mybudget.server.repositories.AccountRepository;
import com.mybudget.server.repositories.SchedualeRepository;
import com.mybudget.server.repositories.UserRepository;
import com.mybudget.server.util.UserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class SechedualeService {
    private final SchedualeRepository schedualeRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final UserUtils userUtils;





    public SechedualeResponse executeSchedule(SechedualeRequest request) {
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        System.out.println(request.getSourceAccountId());
        // Fetch source account
        Account sourceAccount = accountRepository.findById(request.getSourceAccountId())
                .orElseThrow(() -> new RuntimeException("Source account not found"));

        // Fetch destination account (optional)
        Account destinationAccount = null;
        if (request.getDestinationAccountId() != null) {
            destinationAccount = accountRepository.findById(request.getDestinationAccountId())
                    .orElseThrow(() -> new RuntimeException("Destination account not found"));
        }


        // Build schedule
        Secheduale schedule = Secheduale.builder()
                .name(request.getName())
                .description(request.getDescription())
                .sourceAccount(sourceAccount)
                .destinationAccount(destinationAccount)
                .transactionTypes(Set.of(request.getTransactionType()))
                .scheduleIntervals(Set.of(request.getScheduleInterval()))
                .amount(request.getAmount())
                .nextExecutionDate(request.getExecutionDate())
                .isActive(true)
                .user(currentUser)
                .build();

        Secheduale saved = schedualeRepository.save(schedule);

        return mapToResponse(saved);
    }




    private SechedualeResponse mapToResponse(Secheduale schedule) {
        SechedualeResponse response = new SechedualeResponse();
        response.setId(schedule.getId());
        response.setName(schedule.getName());
        response.setDescription(schedule.getDescription());
        response.setTransactionType(schedule.getTransactionTypes().iterator().next());
        response.setScheduleInterval(schedule.getScheduleIntervals().iterator().next());
        response.setAmount(schedule.getAmount());
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
}

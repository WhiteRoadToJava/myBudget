package com.mybudget.server.services;


import com.mybudget.server.dto.incomse.IncomseRequset;
import com.mybudget.server.dto.incomse.IncomseResponse;
import com.mybudget.server.exeptions.ResourceNotFoundException;
import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.Incomse;
import com.mybudget.server.modules.User;
import com.mybudget.server.repositories.AccountRepository;
import com.mybudget.server.repositories.IncomseRepository;
import com.mybudget.server.util.UserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IncomseService {
    private final UserUtils userUtils;
    private final AccountService accountService;
    private final IncomseRepository incomseRepository;
    private final AccountRepository accountRepository;




@Transactional
        public IncomseResponse addInccmse(IncomseRequset incomseRequest){
            User currentUser  = userUtils.getCurrentAuthenticatedUser();
            Incomse incomse = new Incomse();
            incomse.setAmount(incomseRequest.getAmount());
            incomse.setCategory(incomseRequest.getCategory());incomse.setUser(currentUser);

            String accountId = incomseRequest.getAccount().getId();
            Account  account = accountRepository.findByIdAndUser(accountId, currentUser);
            if(account == null) {
                throw new ResourceNotFoundException("Account not found");
            }
            incomse.setAccount(account);
            accountService.updateTotalBalanceWithIncome(account, incomseRequest.getAmount());

            return  mapToIncomseResponse(incomseRepository.save(incomse));
        }
        public IncomseResponse getIncomseById(String incomseId){
            User currentUser = userUtils.getCurrentAuthenticatedUser();
            Incomse incomse = incomseRepository.findById(incomseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Incomse not found or access denied"));

            if(!incomse.getUser().getId().equals(currentUser.getId())){
                throw new ResourceNotFoundException("Incomse not found or access denied");
            }
            else {
                return mapToIncomseResponse(incomse);
            }
        }

        @Transactional
        public IncomseResponse updateIncomse(String incomseId, IncomseRequset requset){
            User currentUser = userUtils.getCurrentAuthenticatedUser();
            Incomse incomse = incomseRepository.findById(incomseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Incomse not found"));
            String accountId = requset.getAccount().getId();
            Account account = accountRepository.findByIdAndUser(accountId, currentUser);
            accountService.updateTotalBalanceWithUpdateIncome(account, incomse, requset.getAmount());
            incomse.setAccount(account);
            if (incomse.getUser().getId().equals(currentUser.getId())){
                        incomse.setAmount(requset.getAmount());
                        incomse.setCategory(requset.getCategory());
                    }
            return mapToIncomseResponse(incomseRepository.save(incomse));
        }

        public List<IncomseResponse> getAllIncomseByAccount(Account account){
            List<Incomse> incomseResponses = incomseRepository.findAllByAccount(account);
            return  incomseResponses.stream().map(this::mapToIncomseResponse).toList();
        }


        public List<IncomseResponse> getAllIncomseByUser(){
            User user = userUtils.getCurrentAuthenticatedUser();
            List<Incomse> incomseResponses = incomseRepository.findAllByUser(user);
            return  incomseResponses.stream().map(this::mapToIncomseResponse).toList();
        }




    private  IncomseResponse mapToIncomseResponse(Incomse incomse){
    String type ;
    if(incomse.getType() == null) {
        type = "inocmse";
    }
    else  type = incomse.getType();
    LocalDateTime date = incomse.getCreatedAt();
    if(incomse.getCreatedAt() == null)
        date = date.now();
        return new IncomseResponse(
                incomse.getId(),
                incomse.getAmount(),
                incomse.getCategory(),
                incomse.getUser(),
                incomse.getAccount(),
                type,
                date
        );
    }

}

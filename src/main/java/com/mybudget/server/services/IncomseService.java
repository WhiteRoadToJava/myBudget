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

import java.util.List;

@Service
@RequiredArgsConstructor
public class IncomseService {
    private final UserUtils userUtils;
    private final IncomseRepository incomseRepository;
    private final AccountRepository accountRepository;



        public IncomseResponse addInccmse(IncomseRequset incomseRequest){
            User currentUser  = userUtils.getCurrentAuthenticatedUser();
            Incomse incomse = new Incomse();
            incomse.setName(incomseRequest.getName());
            incomse.setAmount(incomseRequest.getAmount());
            incomse.setCategory(incomseRequest.getCategory());
            incomse.setCreatedAt(incomseRequest.getCreatedAt());
            incomse.setUser(currentUser);

            String accountId = incomseRequest.getAccount().getId();
            Account  account = accountRepository.findByIdAndUser(accountId, currentUser);
            if(account == null) {
                throw new ResourceNotFoundException("Account not found");
            }
            incomse.setAccount(account);

            return  mapToIncomseResponse(incomseRepository.save(incomse));
        }


        public List<IncomseResponse> getAllIncomseAccount(Account account){
            List<Incomse> incomseResponses = incomseRepository.findAllByAccount(account);
            return incomseResponses.stream().map(this::mapToIncomseResponse).toList();
        }

        public List<IncomseResponse> getAllIncomseByUser(){
            User user = userUtils.getCurrentAuthenticatedUser();
            List<Incomse> incomseResponses = incomseRepository.findAllByUser(user);
            return  incomseResponses.stream().map(this::mapToIncomseResponse).toList();
        }




    private  IncomseResponse mapToIncomseResponse(Incomse incomse){
        return new IncomseResponse(
                incomse.getId(),
                incomse.getName(),
                incomse.getAmount(),
                incomse.getCategory(),
                incomse.getCreatedAt(),
                incomse.getUser(),
                incomse.getAccount()
        );
    }
}

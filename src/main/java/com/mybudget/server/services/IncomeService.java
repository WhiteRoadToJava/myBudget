package com.mybudget.server.services;


import com.mybudget.server.dto.income.IncomeRequest;
import com.mybudget.server.dto.income.IncomeResponse;
import com.mybudget.server.exceptions.ResourceNotFoundException;
import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.Income;
import com.mybudget.server.modules.User;
import com.mybudget.server.repositories.AccountRepository;
import com.mybudget.server.repositories.IncomeRepository;
import com.mybudget.server.services.account.AccountService;
import com.mybudget.server.util.UserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class IncomeService {
    private final UserUtils userUtils;
    private final AccountService accountService;
    private final IncomeRepository incomeRepository;
    private final AccountRepository accountRepository;




@Transactional
        public IncomeResponse addIncome(IncomeRequest incomeRequest){
            User currentUser  = userUtils.getCurrentAuthenticatedUser();
            Income income = new Income();
            income.setAmount(incomeRequest.getAmount());
            income.setCategory(incomeRequest.getCategory());income.setUser(currentUser);

            String accountId = incomeRequest.getAccount().getId();
            Account  account = accountRepository.findByIdAndUser(accountId, currentUser);
            if(account == null) {
                throw new ResourceNotFoundException("Account not found");
            }
            income.setAccount(account);
            accountService.updateTotalBalanceWithIncome(account, incomeRequest.getAmount());

            return  mapToIncomeResponse(incomeRepository.save(income));
        }

    // this method is used with schedule
    @Transactional
    public IncomeResponse executeIncome(IncomeRequest request, User user){
        Income income = new Income();
        income.setAmount(request.getAmount());
        income.setCategory(request.getCategory());income.setUser(user);

        String accountId = request.getAccount().getId();
        Account  account = accountRepository.findByIdAndUser(accountId, user);
        if(account == null) {
            throw new ResourceNotFoundException("Account not found");
        }
        income.setAccount(account);
        accountService.updateTotalBalanceWithIncome(account, request.getAmount());

        return  mapToIncomeResponse(incomeRepository.save(income));
    }

        public IncomeResponse getIncomeById(String incomeId){
            User currentUser = userUtils.getCurrentAuthenticatedUser();
            Income income = incomeRepository.findById(incomeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Income not found or access denied"));

            if(!income.getUser().getId().equals(currentUser.getId())){
                throw new ResourceNotFoundException("Income not found or access denied");
            }
            else {
                return mapToIncomeResponse(income);
            }
        }

        @Transactional
        public IncomeResponse updateIncome(String incomeId, IncomeRequest request){
            User currentUser = userUtils.getCurrentAuthenticatedUser();
            Income income = incomeRepository.findById(incomeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Income not found"));
            String accountId = request.getAccount().getId();
            Account account = accountRepository.findByIdAndUser(accountId, currentUser);
            accountService.updateTotalBalanceWithUpdateIncome(account, income, request.getAmount());
            income.setAccount(account);
            if (income.getUser().getId().equals(currentUser.getId())){
                        income.setAmount(request.getAmount());
                        income.setCategory(request.getCategory());
                        income.setImage(request.getImage());
                    }
            return mapToIncomeResponse(incomeRepository.save(income));
        }
        @Transactional
        public String deleteIncome(String incomeId){
            User currentUser = userUtils.getCurrentAuthenticatedUser();
            Income income = incomeRepository.findById(incomeId)
                    .orElseThrow(()-> new ResourceNotFoundException("Income not found"));
            if(income.getUser().getId().equals(currentUser.getId())){
                accountService.updateTotalBalanceWithDeleteIncome(income.getAccount(), income.getAmount());
                incomeRepository.delete(income);
                return "Income deleted successfully";
            } else {
                throw new ResourceNotFoundException("Income not found or access denied");
            }
        }

        public List<IncomeResponse> getAllIncomeByAccount(Account account){
            List<Income> incomeResponses = incomeRepository.findAllByAccount(account);
            return  incomeResponses.stream().map(this::mapToIncomeResponse).toList();
        }


        public List<IncomeResponse> getAllIncomeByUser(){
            User user = userUtils.getCurrentAuthenticatedUser();
            List<Income> incomeResponses = incomeRepository.findAllByUser(user);
            return  incomeResponses.stream().map(this::mapToIncomeResponse).toList();
        }




    private  IncomeResponse mapToIncomeResponse(Income income){
    String type ;
    if(income.getType() == null) {
        type = "income";
    }
    else  type = income.getType();
    LocalDateTime date = income.getCreatedAt();
    if(income.getCreatedAt() == null)
        date = date.now();
        Map<String, String> image = income.getImage();
        Map<String, String> imageUrl = new HashMap<>();
        if (image != null && image.get("url") != null) {
            imageUrl.put("url", image.get("url"));
            imageUrl.put("filename", image.get("filename"));
        } else{
            imageUrl.put("url","");
            imageUrl.put("filename", "");
        }
        return new IncomeResponse(
                income.getId(),
                income.getAmount(),
                income.getCategory(),
                income.getUser(),
                income.getAccount(),
                type,
                date,
                image
        );
    }

}

package com.mybudget.server.services.account;


import com.mybudget.server.dto.Transaction;
import com.mybudget.server.repositories.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;



@Service
@RequiredArgsConstructor
public class RapportService {
    private final AccountRepository accountRepository;
    private final AccountService accountService;

    public Transaction getAccountBetweenTwoDates(String date1, String date2){
        return null;
    }


}


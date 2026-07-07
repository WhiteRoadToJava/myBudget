package com.mybudget.server;


import com.mybudget.server.dto.accounts.AccountRequest;
import com.mybudget.server.services.account.AccountService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@RequiredArgsConstructor
public class AccountServiceTest {
    @Autowired
    private final AccountService accountService;

    /*
    @Test
    void createAccountTest(){
        accountService.createAccount("mohammad abbas", "USD", "normal", "descript", 12, 12, "2014-12-12", "user", "active");
    }
    */


}

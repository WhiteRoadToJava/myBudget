package com.mybudget.server.services;

import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.CurrencyBalance;
import com.mybudget.server.services.account.AccountBalances;
import com.mybudget.server.modules.TotalBalance;
import com.mybudget.server.modules.User;
import com.mybudget.server.repositories.AccountRepository;
import com.mybudget.server.repositories.TotalBalanceRepository;
import com.mybudget.server.util.UserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TotalBalanceService {
    private final TotalBalanceRepository totalBalanceRepository;
    private final AccountRepository accountRepository;
    private  final UserUtils userUtils;


    @Scheduled(cron = "0 0 1 * * *")
    public void execute() {
        List<Account> accountList = accountRepository.findAll();

        // Group balances by userId
        Map<String, TotalBalance> balanceByUser = new HashMap<>();

        for (Account account : accountList) {
            String userId = account.getUser().getId();

            // Get existing TotalBalance for this user, or create a new one
            TotalBalance totalBalance = balanceByUser.getOrDefault(
                    userId,
                    new TotalBalance(null, new HashMap<>(), userId, null)
            );

            // every currency of the account
            for (CurrencyBalance pocket : AccountBalances.pockets(account)) {
                totalBalance.getTotalBalanceByCurrency().merge(pocket.getCurrency(),
                        pocket.getTotalBalance() == null ? 0.0 : pocket.getTotalBalance(), Double::sum);
            }

            balanceByUser.put(userId, totalBalance);
        }

        // Save all and return
        if (!balanceByUser.isEmpty()) {
            totalBalanceRepository.saveAll(balanceByUser.values());
        }
    }

    public List<TotalBalance> getAllTotalBalanceByUser(){
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        if(currentUser == null) throw new RuntimeException("User not found");
        String userId = currentUser.getId();
        return totalBalanceRepository.findAllByUser(userId);
    }

}
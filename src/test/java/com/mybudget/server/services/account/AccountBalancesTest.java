package com.mybudget.server.services.account;

import com.mybudget.server.exeptions.ConflictException;
import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.CurrencyBalance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountBalancesTest {

    // an account as the old code saved it: one currency, no "balances"
    private Account oldAccount() {
        Account account = new Account();
        account.setCurrency("SEK");
        account.setBalance(100.0);
        account.setTotalBalance(250.0);
        return account;
    }

    @Test
    void anAccountFromBeforeTheMigrationGetsItsPocketFromTheOldFields() {
        Account account = oldAccount();
        CurrencyBalance pocket = AccountBalances.pockets(account).get(0);
        assertEquals("SEK", pocket.getCurrency());
        assertEquals(100.0, pocket.getOpeningBalance());
        assertEquals(250.0, pocket.getTotalBalance());
    }

    @Test
    void moneyMovesInItsOwnCurrencyAndTheOldFieldsFollowTheFirstCurrency() {
        Account account = oldAccount();
        AccountBalances.add(account, "usd", 50);
        AccountBalances.change(account, "USD", -20.004);
        AccountBalances.change(account, "SEK", 10);

        assertEquals(30.0, AccountBalances.find(account, "USD").get().getTotalBalance());
        assertEquals(260.0, AccountBalances.find(account, "SEK").get().getTotalBalance());
        assertEquals(260.0, account.getTotalBalance());   // old field in step with SEK only
    }

    @Test
    void aRecordWithoutCurrencyUsesTheFirstCurrency() {
        Account account = oldAccount();
        AccountBalances.add(account, "EUR", 0);
        assertEquals("SEK", AccountBalances.resolve(account, null));
        assertEquals("EUR", AccountBalances.resolve(account, " eur "));
        assertThrows(IllegalArgumentException.class, () -> AccountBalances.resolve(account, "USD"));
    }

    @Test
    void aCurrencyIsInTheAccountOnlyOnce() {
        Account account = oldAccount();
        assertThrows(ConflictException.class, () -> AccountBalances.add(account, "sek", 0));
        assertThrows(IllegalArgumentException.class, () -> AccountBalances.add(account, "US", 0));
    }

    @Test
    void aNewOpeningBalanceMovesTheBalanceByTheDifference() {
        Account account = oldAccount();
        AccountBalances.setOpening(account, "SEK", 150);
        CurrencyBalance pocket = AccountBalances.find(account, "SEK").get();
        assertEquals(150.0, pocket.getOpeningBalance());
        assertEquals(300.0, pocket.getTotalBalance());
        assertEquals(150.0, account.getBalance());
        assertTrue(account.getTotalBalance() == 300.0);
    }
}

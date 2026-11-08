package com.mybudget.server.services;

import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.Transfer;
import com.mybudget.server.modules.enums.FeeDirection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransferEffectsTest {

    private static Account account(String id, String firstCurrency) {
        Account account = new Account();
        account.setId(id);
        account.setCurrency(firstCurrency);
        return account;
    }

    private final Account sek = account("sek", "SEK");
    private final Account eur = account("eur", "EUR");
    private final Account cash = account("cash", "SEK");

    // 1000 SEK -> 87 EUR; a transfer from before the migration has no currencies
    private Transfer transfer() {
        Transfer transfer = new Transfer();
        transfer.setSourceAccount(sek);
        transfer.setDestinationAccount(eur);
        transfer.setAmountSent(1000.0);
        transfer.setAmountReceived(87.0);
        return transfer;
    }

    @Test
    void withoutFeesOnlyTheTwoPocketsMove() {
        Transfer t = transfer();
        assertEquals(-1000.0, TransferEffects.netFor(t, "sek", "SEK"));
        assertEquals(87.0, TransferEffects.netFor(t, "eur", "EUR"));
        assertEquals(2, TransferEffects.of(t).size());
    }

    @Test
    void chargedFeesAreTakenFromTheirSide() {
        Transfer t = transfer();
        t.setSendFee(15.0);
        t.setSendFeeDirection(FeeDirection.CHARGE);
        t.setSendFeeAccount(sek);
        t.setReceiveFee(2.0);
        t.setReceiveFeeDirection(FeeDirection.CHARGE);
        t.setReceiveFeeAccount(eur);

        assertEquals(-1015.0, TransferEffects.netFor(t, "sek", "SEK"));
        assertEquals(85.0, TransferEffects.netFor(t, "eur", "EUR"));
        assertEquals(2, TransferEffects.of(t).size()); // same pockets are added together
    }

    @Test
    void creditedFeesAreAdded() {
        Transfer t = transfer();
        t.setSendFee(15.0);
        t.setSendFeeDirection(FeeDirection.CREDIT);
        t.setSendFeeAccount(sek);
        t.setReceiveFee(2.0);
        t.setReceiveFeeDirection(FeeDirection.CREDIT);
        t.setReceiveFeeAccount(eur);

        assertEquals(-985.0, TransferEffects.netFor(t, "sek", "SEK"));
        assertEquals(89.0, TransferEffects.netFor(t, "eur", "EUR"));
    }

    @Test
    void aFeeOnAnotherAccountMovesThatAccount() {
        Transfer t = transfer();
        t.setSendFee(15.0);
        t.setSendFeeDirection(FeeDirection.CHARGE);
        t.setSendFeeAccount(cash);

        assertEquals(-1000.0, TransferEffects.netFor(t, "sek", "SEK"));
        assertEquals(-15.0, TransferEffects.netFor(t, "cash", "SEK"));
        assertEquals(3, TransferEffects.of(t).size());
    }

    @Test
    void anExchangeInsideOneAccountMovesTwoPocketsOfIt() {
        Account wallet = account("w", "USD");
        Transfer t = new Transfer();
        t.setSourceAccount(wallet);
        t.setSourceCurrency("USD");
        t.setDestinationAccount(wallet);
        t.setDestinationCurrency("SYP");
        t.setAmountSent(10.0);
        t.setAmountReceived(130000.0);
        t.setSendFee(1.0);
        t.setSendFeeDirection(FeeDirection.CHARGE);
        t.setSendFeeAccount(wallet);
        t.setSendFeeCurrency("USD");

        assertEquals(-11.0, TransferEffects.netFor(t, "w", "USD"));
        assertEquals(130000.0, TransferEffects.netFor(t, "w", "SYP"));
        assertEquals(2, TransferEffects.of(t).size());
    }

    @Test
    void deletedAccountsAndMissingFeesAreSkipped() {
        Transfer t = transfer();
        t.setSourceAccount(null);
        t.setSendFee(15.0);
        t.setSendFeeAccount(null);
        assertEquals(87.0, TransferEffects.netFor(t, "eur", "EUR"));
        assertEquals(1, TransferEffects.of(t).size());
    }

    @Test
    void aMissingDirectionMeansCharge() {
        assertTrue(TransferEffects.signedFee(5.0, null) < 0);
        assertEquals(0.0, TransferEffects.signedFee(null, FeeDirection.CREDIT));
    }
}

package com.mybudget.server.services;

import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.Transfer;
import com.mybudget.server.modules.enums.FeeDirection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransferEffectsTest {

    private static Account account(String id) {
        Account account = new Account();
        account.setId(id);
        return account;
    }

    private final Account sek = account("sek");
    private final Account eur = account("eur");
    private final Account cash = account("cash");

    // 1000 SEK -> 87 EUR
    private Transfer transfer() {
        Transfer transfer = new Transfer();
        transfer.setSourceAccount(sek);
        transfer.setDestinationAccount(eur);
        transfer.setAmountSent(1000.0);
        transfer.setAmountReceived(87.0);
        return transfer;
    }

    @Test
    void withoutFeesOnlyTheTwoAccountsMove() {
        Transfer t = transfer();
        assertEquals(-1000.0, TransferEffects.netFor(t, "sek"));
        assertEquals(87.0, TransferEffects.netFor(t, "eur"));
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

        assertEquals(-1015.0, TransferEffects.netFor(t, "sek"));
        assertEquals(85.0, TransferEffects.netFor(t, "eur"));
        assertEquals(2, TransferEffects.of(t).size()); // same accounts are added together
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

        assertEquals(-985.0, TransferEffects.netFor(t, "sek"));
        assertEquals(89.0, TransferEffects.netFor(t, "eur"));
    }

    @Test
    void aFeeOnAnotherAccountMovesThatAccount() {
        Transfer t = transfer();
        t.setSendFee(15.0);
        t.setSendFeeDirection(FeeDirection.CHARGE);
        t.setSendFeeAccount(cash);

        assertEquals(-1000.0, TransferEffects.netFor(t, "sek"));
        assertEquals(-15.0, TransferEffects.netFor(t, "cash"));
        assertEquals(3, TransferEffects.of(t).size());
    }

    @Test
    void deletedAccountsAndMissingFeesAreSkipped() {
        Transfer t = transfer();
        t.setSourceAccount(null);
        t.setSendFee(15.0);
        t.setSendFeeAccount(null);
        assertEquals(87.0, TransferEffects.netFor(t, "eur"));
        assertEquals(1, TransferEffects.of(t).size());
    }

    @Test
    void aMissingDirectionMeansCharge() {
        assertTrue(TransferEffects.signedFee(5.0, null) < 0);
        assertEquals(0.0, TransferEffects.signedFee(null, FeeDirection.CREDIT));
    }
}

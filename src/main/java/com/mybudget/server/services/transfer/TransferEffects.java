package com.mybudget.server.services.transfer;

import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.Transfer;
import com.mybudget.server.modules.enums.FeeDirection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TransferEffects {
    private TransferEffects() {
    }

    public record Effect(Account account, double amount) {
    }

    public static List<Effect> of(Transfer transfer) {
        Map<String, Account> accounts = new LinkedHashMap<>();
        Map<String, Double> amounts = new LinkedHashMap<>();
        add(accounts, amounts, transfer.getSourceAccount(), -value(transfer.getAmountSent()));
        add(accounts, amounts, transfer.getDestinationAccount(), transfer.getAmountReceived());
        add(accounts, amounts, transfer.getSendFeeAccount(),
                signedFee(transfer.getSendFee(), transfer.getSendFeeDirection()));
        add(accounts, amounts, transfer.getReceiveFeeAccount(),
                signedFee(transfer.getReceiveFee(), transfer.getReceiveFeeDirection()));
        List<Effect> effects = new ArrayList<>();
        amounts.forEach((id, amount) -> effects.add(new Effect(accounts.get(id), round(amount))));
        return effects;
    }

    public static double netFor(Transfer transfer, String accountId) {
        return of(transfer).stream()
                .filter(a -> a.account.getId().equals(accountId))
                .mapToDouble(Effect::amount)
                .sum();
    }

    public static double signedFee(Double fee, FeeDirection direction) {
        double value = value(fee);
        return direction == FeeDirection.CREDIT ? value : -value;
    }

    private static void add(Map<String, Account> accounts, Map<String, Double> amounts,
                            Account account, double amount) {
        if (account == null || account.getId() == null || amount == 0){
            return;
        };
        accounts.putIfAbsent(account.getId(), account);
        amounts.merge(account.getId(), amount, Double::sum);
    }

    private static double value(Double number){
        return number == null ? 0.0 : number;
    }

    private static double round(double value){
        return Math.round(value * 100) / 100.0;
    }
}

package com.mybudget.server.services;

import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.Transfer;
import com.mybudget.server.modules.enums.FeeDirection;
import com.mybudget.server.services.account.AccountBalances;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The one place that knows what a transfer does to balances, per pocket (account + currency).
 * Creating applies the effects, deleting reverts them, updating reverts the old ones and
 * applies the new ones, and the transaction lists show the same numbers.
 *
 *   source pocket        - amount sent
 *   destination pocket   + amount received
 *   send fee pocket      - fee (CHARGE) or + fee (CREDIT)   (default: the source pocket)
 *   receive fee pocket   - fee (CHARGE) or + fee (CREDIT)   (default: the destination pocket)
 *
 * The source and the destination can be the same account in two currencies: an exchange.
 * Effects on the same pocket are added together. A deleted account (null) is skipped.
 * A transfer from before the migration has no currencies: each side is in its account's
 * first currency.
 */
public final class TransferEffects {

    private TransferEffects() {
    }

    // amount > 0 adds money to the pocket, amount < 0 takes it
    public record Effect(Account account, String currency, double amount) {
        public boolean isPocket(Account other, String otherCurrency) {
            return other != null && other.getId() != null && account.getId().equals(other.getId())
                    && currency != null && currency.equalsIgnoreCase(otherCurrency);
        }
    }

    public static List<Effect> of(Transfer transfer) {
        Map<String, Effect> byPocket = new LinkedHashMap<>();
        add(byPocket, transfer.getSourceAccount(), sourceCurrency(transfer), -value(transfer.getAmountSent()));
        add(byPocket, transfer.getDestinationAccount(), destinationCurrency(transfer), value(transfer.getAmountReceived()));
        add(byPocket, transfer.getSendFeeAccount(), sendFeeCurrency(transfer),
                signedFee(transfer.getSendFee(), transfer.getSendFeeDirection()));
        add(byPocket, transfer.getReceiveFeeAccount(), receiveFeeCurrency(transfer),
                signedFee(transfer.getReceiveFee(), transfer.getReceiveFeeDirection()));

        List<Effect> effects = new ArrayList<>();
        byPocket.values().forEach(e -> effects.add(new Effect(e.account(), e.currency(), round(e.amount()))));
        return effects;
    }

    // the total effect on one pocket (0 if the transfer does not touch it)
    public static double netFor(Transfer transfer, String accountId, String currency) {
        return of(transfer).stream()
                .filter(e -> e.account().getId().equals(accountId) && e.currency().equalsIgnoreCase(currency))
                .mapToDouble(Effect::amount)
                .sum();
    }

    // ---------- the currency of each side ----------

    public static String sourceCurrency(Transfer t) {
        return currencyOr(t.getSourceCurrency(), t.getSourceAccount());
    }

    public static String destinationCurrency(Transfer t) {
        return currencyOr(t.getDestinationCurrency(), t.getDestinationAccount());
    }

    public static String sendFeeCurrency(Transfer t) {
        return currencyOr(t.getSendFeeCurrency(), t.getSendFeeAccount());
    }

    public static String receiveFeeCurrency(Transfer t) {
        return currencyOr(t.getReceiveFeeCurrency(), t.getReceiveFeeAccount());
    }

    private static String currencyOr(String currency, Account account) {
        if (currency != null || account == null) {
            return currency;
        }
        return AccountBalances.primaryCurrency(account);
    }

    public static double signedFee(Double fee, FeeDirection direction) {
        double value = value(fee);
        return direction == FeeDirection.CREDIT ? value : -value;
    }

    private static void add(Map<String, Effect> byPocket, Account account, String currency, double amount) {
        if (account == null || account.getId() == null || currency == null || amount == 0) {
            return;
        }
        String key = account.getId() + "|" + currency.toUpperCase();
        Effect previous = byPocket.get(key);
        byPocket.put(key, new Effect(account, currency.toUpperCase(),
                (previous == null ? 0.0 : previous.amount()) + amount));
    }

    private static double value(Double number) {
        return number == null ? 0.0 : number;
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

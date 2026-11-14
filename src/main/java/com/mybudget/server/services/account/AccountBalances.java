package com.mybudget.server.services.account;

import com.mybudget.server.exeptions.ConflictException;
import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.CurrencyBalance;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The balances of an account, one per currency ("pockets").
 *
 * During the move to multi-currency accounts the old single-currency fields
 * (currency, balance, totalBalance) are kept in step with the pocket of that currency,
 * so code that still reads them stays right. They are removed in the last phase.
 *
 * An account saved before the migration has no "balances" yet: its pocket is built
 * from the old fields the first time it is read, so nothing breaks either way.
 */
public final class AccountBalances {

    private AccountBalances() {
    }

    public static List<CurrencyBalance> pockets(Account account) {
        if (account.getBalances() == null) {
            List<CurrencyBalance> pockets = new ArrayList<>();
            if (account.getCurrency() != null) {
                double opening = account.getBalance() == null ? 0.0 : account.getBalance();
                double total = account.getTotalBalance() == null ? opening : account.getTotalBalance();
                pockets.add(new CurrencyBalance(account.getCurrency(), opening, total));
            }
            account.setBalances(pockets);
        }
        return account.getBalances();
    }

    // the account's first currency (the old single currency for accounts from before)
    public static String primaryCurrency(Account account) {
        if (account.getCurrency() != null) {
            return account.getCurrency();
        }
        List<CurrencyBalance> pockets = pockets(account);
        return pockets.isEmpty() ? null : pockets.get(0).getCurrency();
    }

    public static Optional<CurrencyBalance> find(Account account, String currency) {
        if (currency == null) {
            return Optional.empty();
        }
        return pockets(account).stream()
                .filter(p -> currency.equalsIgnoreCase(p.getCurrency()))
                .findFirst();
    }

    // the currency a record uses: the one asked for, or the account's first currency
    public static String resolve(Account account, String requested) {
        String currency = requested == null || requested.isBlank()
                ? primaryCurrency(account)
                : requested.trim().toUpperCase(Locale.ROOT);
        if (find(account, currency).isEmpty()) {
            throw new IllegalArgumentException("The account has no " + currency);
        }
        return currency;
    }

    // adds (or, negative, takes) money in one currency; keeps the old fields in step
    public static void change(Account account, String currency, double amount) {
        CurrencyBalance pocket = find(account, currency)
                .orElseThrow(() -> new IllegalArgumentException("The account has no " + currency));
        double current = pocket.getTotalBalance() == null ? 0.0 : pocket.getTotalBalance();
        pocket.setTotalBalance(round(current + amount));
        if (currency.equalsIgnoreCase(account.getCurrency())) {
            account.setTotalBalance(pocket.getTotalBalance());
        }
    }

    // a new currency in the account
    public static CurrencyBalance add(Account account, String currency, double openingBalance) {
        String code = currency == null ? "" : currency.trim().toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Choose a currency");
        }
        if (find(account, code).isPresent()) {
            throw new ConflictException("This currency is already in the account");
        }
        CurrencyBalance pocket = new CurrencyBalance(code, round(openingBalance), round(openingBalance));
        pockets(account).add(pocket);
        return pocket;
    }

    // a new opening balance for one currency: the balance moves by the same difference
    public static void setOpening(Account account, String currency, double openingBalance) {
        CurrencyBalance pocket = find(account, currency)
                .orElseThrow(() -> new IllegalArgumentException("The account has no " + currency));
        double oldOpening = pocket.getOpeningBalance() == null ? 0.0 : pocket.getOpeningBalance();
        pocket.setOpeningBalance(round(openingBalance));
        change(account, pocket.getCurrency(), openingBalance - oldOpening);
        if (pocket.getCurrency().equalsIgnoreCase(account.getCurrency())) {
            account.setBalance(pocket.getOpeningBalance());
        }
    }

    public static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

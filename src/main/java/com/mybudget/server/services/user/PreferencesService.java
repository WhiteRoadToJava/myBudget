package com.mybudget.server.services.user;


import com.mybudget.server.dto.user.PreferencesRequest;
import com.mybudget.server.dto.user.PreferencesResponse;
import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.User;
import com.mybudget.server.modules.UserPreferences;
import com.mybudget.server.modules.enums.AccountStatus;
import com.mybudget.server.repositories.AccountRepository;
import com.mybudget.server.repositories.UserRepository;
import com.mybudget.server.services.currency.CurrencyCatalog;
import com.mybudget.server.util.UserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PreferencesService {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final UserUtils userUtils;
    private final CurrencyCatalog currencyCatalog;

    public PreferencesResponse getPreferences() {
        return toResponse(userUtils.getCurrentAuthenticatedUser());
    }

    public PreferencesResponse updatePreferences(PreferencesRequest request) {
        User authenticatedUser = userUtils.getCurrentAuthenticatedUser();
        // users from before preferences have none yet: the first save makes them
        UserPreferences preferences = authenticatedUser.getPreferences() != null
                ? authenticatedUser.getPreferences() : new UserPreferences();

        String language = request.getLanguage() == null ? preferences.getLanguage()
                : PreferenceRules.language(request.getLanguage());
        String currency = request.getBaseCurrency() == null ? preferences.getBaseCurrency()
                : baseCurrency(request.getBaseCurrency());
        String firstDay = request.getFirstDayOfWeek() == null ? preferences.getFirstDayOfWeek()
                : PreferenceRules.firstDayOfWeek(request.getFirstDayOfWeek());
        String accountId = request.getDefaultAccountId() == null ? preferences.getDefaultAccountId()
                : defaultAccount(request.getDefaultAccountId(), authenticatedUser);

        String amountDisplay = request.getAmountDisplay() == null ? preferences.getAmountDisplay()
                : PreferenceRules.amountDisplay(request.getAmountDisplay());

        preferences.setLanguage(language);
        preferences.setBaseCurrency(currency);
        preferences.setFirstDayOfWeek(firstDay);
        preferences.setDefaultAccountId(accountId);
        preferences.setAmountDisplay(amountDisplay);
        authenticatedUser.setPreferences(preferences);
        return toResponse(userRepository.save(authenticatedUser));
    }


    // "" clears it; otherwise a currency of the app's list
    private String baseCurrency(String value) {
        String code = PreferenceRules.currency(value);
        return code == null ? null : currencyCatalog.requireEnabled(code);
    }

    private String defaultAccount(String accountId, User user) {
        if (accountId.isBlank()) return null;
        if (!isUsable(accountRepository.findByIdAndUser(accountId, user))) {
            throw new IllegalArgumentException("The default account must be one of your active accounts");
        }
        return accountId;
    }

    // a default account that was deleted or archived since is not offered any more
    private PreferencesResponse toResponse(User user) {
        UserPreferences p = user.getPreferences();
        if (p == null) {
            return new PreferencesResponse(null, null, null, null, null);
        }
        String accountId = p.getDefaultAccountId() != null
                && isUsable(accountRepository.findByIdAndUser(p.getDefaultAccountId(), user))
                ? p.getDefaultAccountId() : null;
        return new PreferencesResponse(p.getLanguage(), p.getBaseCurrency(), p.getFirstDayOfWeek(), accountId,
                p.getAmountDisplay());    }

    private boolean isUsable(Account account) {
        return account != null
                && (account.getStatus() == null || !account.getStatus().contains(AccountStatus.ARCHIVED));
    }


}

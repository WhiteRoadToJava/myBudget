package com.mybudget.server.services.currency;

import com.mybudget.server.dto.currency.CurrencyResponse;
import com.mybudget.server.dto.currency.UpdateCurrencyRequest;
import com.mybudget.server.exeptions.ConflictException;
import com.mybudget.server.exeptions.ResourceNotFoundException;
import com.mybudget.server.modules.SupportedCurrency;
import com.mybudget.server.repositories.SupportedCurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

// The one list of currencies: what users may choose, and what the admin changes.
@Service
@RequiredArgsConstructor
public class CurrencyCatalog {

    private final SupportedCurrencyRepository repository;

    // the first start fills the list with the currencies the app had before
    @EventListener(ApplicationReadyEvent.class)
    public void fillAtFirstStart() {
        if (repository.count() > 0) return;
        Set<String> codes = new LinkedHashSet<>(CurrencyRules.ENABLED_AT_START);
        codes.addAll(CurrencyRules.PAYMENT_AT_START);
        List<SupportedCurrency> list = new ArrayList<>();
        int position = 0;
        for (String code : codes) {
            list.add(new SupportedCurrency(code,
                    CurrencyRules.ENABLED_AT_START.contains(code),
                    CurrencyRules.PAYMENT_AT_START.contains(code),
                    position++));
        }
        repository.saveAll(list);
    }

    // ---------- users ----------

    // the currencies that can be used for something, in the admin's order
    public List<CurrencyResponse> usable() {
        return repository.findAllByOrderByPositionAsc().stream()
                .filter(c -> c.isEnabled() || c.isPayment())
                .map(CurrencyResponse::of)
                .toList();
    }

    // for accounts, debts and the base currency: the tidied code, or 400
    public String requireEnabled(String value) {
        String code = CurrencyRules.code(value);
        if (repository.findById(code).map(SupportedCurrency::isEnabled).orElse(false)) {
            return code;
        }
        throw new IllegalArgumentException("The currency " + code + " can not be chosen");
    }

    // for a subscription payment: the tidied code, or 400
    public String requirePayment(String value) {
        String code = CurrencyRules.code(value);
        if (repository.findById(code).map(SupportedCurrency::isPayment).orElse(false)) {
            return code;
        }
        throw new IllegalArgumentException("A subscription can not be paid in " + code);
    }

    // ---------- admin ----------

    public List<CurrencyResponse> all() {
        return repository.findAllByOrderByPositionAsc().stream().map(CurrencyResponse::of).toList();
    }

    // a new currency comes last, ready for accounts
    public CurrencyResponse add(String value) {
        String code = CurrencyRules.code(value);
        if (repository.existsById(code)) {
            throw new ConflictException(code + " is already in the list");
        }
        int last = repository.findAllByOrderByPositionAsc().stream()
                .mapToInt(SupportedCurrency::getPosition).max().orElse(-1);
        return CurrencyResponse.of(repository.save(new SupportedCurrency(code, true, false, last + 1)));
    }

    public CurrencyResponse update(String value, UpdateCurrencyRequest request) {
        String code = CurrencyRules.code(value);
        SupportedCurrency currency = repository.findById(code)
                .orElseThrow(() -> new ResourceNotFoundException(code + " is not in the list"));
        if (request.getEnabled() != null) {
            if (!request.getEnabled() && currency.isEnabled() && enabledCount() == 1) {
                throw new IllegalArgumentException("At least one currency must stay available for accounts");
            }
            currency.setEnabled(request.getEnabled());
        }
        if (request.getPayment() != null) {
            currency.setPayment(request.getPayment());
        }
        return CurrencyResponse.of(repository.save(currency));
    }

    public List<CurrencyResponse> order(List<String> requested) {
        List<SupportedCurrency> list = repository.findAllByOrderByPositionAsc();
        Map<String, SupportedCurrency> byCode = list.stream()
                .collect(Collectors.toMap(SupportedCurrency::getCode, Function.identity()));
        List<String> codes = CurrencyRules.order(new ArrayList<>(byCode.keySet()), requested);
        for (int i = 0; i < codes.size(); i++) {
            byCode.get(codes.get(i)).setPosition(i);
        }
        repository.saveAll(list);
        return all();
    }

    private long enabledCount() {
        return repository.findAll().stream().filter(SupportedCurrency::isEnabled).count();
    }
}
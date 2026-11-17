package com.mybudget.server.services.exchange;


import com.mybudget.server.dto.exchange.ExchangeRateRequest;
import com.mybudget.server.dto.exchange.ExchangeRateResponse;
import com.mybudget.server.exeptions.ResourceNotFoundException;
import com.mybudget.server.modules.ExchangeRate;
import com.mybudget.server.modules.User;
import com.mybudget.server.repositories.ExchangeRateRepository;
import com.mybudget.server.util.UserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ExchangeRateService {
    private static final Pattern CODE = Pattern.compile(("[A-Z]{3}"));

    private final ExchangeRateRepository exchangeRateRepository;
    private final UserUtils userUtils;

    public List<ExchangeRateResponse> list() {
        User authenticatedUser = userUtils.getCurrentAuthenticatedUser();
        return exchangeRateRepository.findAllByUserIdOrderByUpdatedAtDesc(authenticatedUser.getId())
                .stream().map(r -> toResponse(r, false))
                .toList();
    }

    public Optional<ExchangeRateResponse> latest(String from, String to) {
        User authenticateduser = userUtils.getCurrentAuthenticatedUser();
        String f = code(from);
        String t = code(to);
        if (f.equals(t)) {
            return Optional.empty();
        }
        Optional<ExchangeRate> direct = exchangeRateRepository.findByUserIdAndFromCurrencyAndToCurrency(authenticateduser.getId(), f, t);
        if (direct.isPresent()) {
            return Optional.of(toResponse(direct.get(), false));
        }
        return exchangeRateRepository.findByUserIdAndFromCurrencyAndToCurrency(authenticateduser.getId(), t, f )
                .map(r -> toResponse(r, true));
    }

    public ExchangeRateResponse add(ExchangeRateRequest request){
        User authenticatedUser = userUtils.getCurrentAuthenticatedUser();
        if (request.getRate() == null || request.getRate() <= 0) {
            throw new IllegalArgumentException("The exchange rate must be greater than zero ");
        }
        String from  = code(request.getFrom());
        String to = code(request.getTo());
        if (from.equals(to)){
            throw new IllegalArgumentException("the exchange rate needs two different currencies");
        }
        return toResponse(save(authenticatedUser, from, to, request.getRate(), null), false);
    }

    public void delete(String id){
        User authenticatedUser = userUtils.getCurrentAuthenticatedUser();
        ExchangeRate rate = exchangeRateRepository.findByIdAndUserId(id, authenticatedUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Exchange rate not found"));
        exchangeRateRepository.delete(rate);
    }


    public void remember(User user, String from, String to, Double rate, Long transferNumber){
        if (user == null || from == null || to == null || from.equals(to) || rate == null || rate <= 0) {
            return;
        }
        try {
            save(user, from, to, rate, transferNumber);
        }catch (RuntimeException ignore){

        }
    }


    private ExchangeRate save(User user, String from, String to, Double rate, Long transferNumber){
        exchangeRateRepository.findByUserIdAndFromCurrencyAndToCurrency(user.getId(), to, from)
                .ifPresent(exchangeRateRepository::delete);
        ExchangeRate row = exchangeRateRepository.findByUserIdAndFromCurrencyAndToCurrency(user.getId(), from, to)
                .orElseGet(ExchangeRate::new);
        row.setUserId(user.getId());
        row.setFromCurrency(from);
        row.setToCurrency(to);
        row.setRate(rate);
        row.setUpdatedAt(LocalDateTime.now());
        row.setTransferNumber(transferNumber);
        return exchangeRateRepository.save(row);
    }

    private String code(String value){
        String code = value == null ? "" : value.trim().toUpperCase();
        if (!CODE.matcher(code).matches()){
            throw new IllegalArgumentException("A currency is a code of thee letter, like USD");
        }
        return code;
    }

    private ExchangeRateResponse toResponse(ExchangeRate r, boolean inverted){
        return inverted
                ? new ExchangeRateResponse(r.getId(), r.getToCurrency(), r.getFromCurrency(), 1.0 / r.getRate(),
                r.getUpdatedAt(), r.getTransferNumber(), true)
                : new ExchangeRateResponse(r.getId(), r.getFromCurrency(), r.getToCurrency(), r.getRate(),
                r.getUpdatedAt(), r.getTransferNumber(), false);
    }
}

package com.mybudget.server.controllers.admin;

import com.mybudget.server.dto.currency.AddCurrencyRequest;
import com.mybudget.server.dto.currency.CurrencyOrderRequest;
import com.mybudget.server.dto.currency.CurrencyResponse;
import com.mybudget.server.dto.currency.UpdateCurrencyRequest;
import com.mybudget.server.services.currency.CurrencyCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/currencies")
@RequiredArgsConstructor
public class AdminCurrencyController {
    private final CurrencyCatalog currencyCatalog;

    // every currency of the list, the disabled ones too
    @GetMapping
    public ResponseEntity<List<CurrencyResponse>> getCurrencies() {
        return ResponseEntity.ok(currencyCatalog.all());
    }

    // body: { "code": "NOK" }
    @PostMapping
    public ResponseEntity<CurrencyResponse> addCurrency(@RequestBody AddCurrencyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(currencyCatalog.add(request.getCode()));
    }

    // body: any of { "enabled": false, "payment": true }
    @PatchMapping("/{code}")
    public ResponseEntity<CurrencyResponse> updateCurrency(@PathVariable String code,
                                                           @RequestBody UpdateCurrencyRequest request) {
        return ResponseEntity.ok(currencyCatalog.update(code, request));
    }

    // body: { "codes": ["SEK", "EUR", ...] }
    @PutMapping("/order")
    public ResponseEntity<List<CurrencyResponse>> orderCurrencies(@RequestBody CurrencyOrderRequest request) {
        return ResponseEntity.ok(currencyCatalog.order(request.getCodes()));
    }
}
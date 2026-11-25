package com.mybudget.server.controllers;

import com.mybudget.server.dto.currency.CurrencyResponse;
import com.mybudget.server.services.currency.CurrencyCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Outside /api/user so admins (who may not have the USER role) can read it too; any signed-in user.
@RestController
@RequestMapping("/api/currencies")
@RequiredArgsConstructor
public class CurrencyController {
    private final CurrencyCatalog currencyCatalog;

    @GetMapping
    public ResponseEntity<List<CurrencyResponse>> getCurrencies() {
        return ResponseEntity.ok(currencyCatalog.usable());
    }
}
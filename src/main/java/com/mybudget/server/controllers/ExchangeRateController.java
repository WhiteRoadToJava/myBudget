package com.mybudget.server.controllers;


import com.mybudget.server.dto.exchange.ExchangeRateRequest;
import com.mybudget.server.services.exchange.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user/exchange-rates")
public class ExchangeRateController {
    private final ExchangeRateService exchangeRateService;

    @GetMapping
    public ResponseEntity<?> list(){
        return ResponseEntity.ok(exchangeRateService.list());
    }

    @GetMapping("/latest")
    public ResponseEntity<?> latest(String from, @RequestParam String to){
        return exchangeRateService.latest(from, to)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping
    public ResponseEntity<?> add(@RequestBody ExchangeRateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(exchangeRateService.add(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        exchangeRateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

package com.mybudget.server.controllers;

import com.mybudget.server.services.mongo.MongoSyncrService;
import com.mybudget.server.services.mongo.SyncReport;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Admin only (SecurityConfig: /api/mongo/** requires ROLE_ADMIN)
@RestController
@RequestMapping("/api/mongo")
@RequiredArgsConstructor
public class MongoController {

    private final MongoSyncrService mongoSyncrService;

    // restore local from Atlas
    @PostMapping("/pull")
    public ResponseEntity<SyncReport> pullAllFromAtlas() {
        return ResponseEntity.ok(mongoSyncrService.pullAllfromAtlasToLocal());
    }

    // run the hourly push now, and see what it did
    @PostMapping("/push")
    public ResponseEntity<SyncReport> pushToAtlas() {
        return ResponseEntity.ok(mongoSyncrService.pushLocalToAtlas());
    }

    // the result of the last run (scheduled or manual); 204 before the first one
    @GetMapping("/status")
    public ResponseEntity<SyncReport> status() {
        SyncReport report = mongoSyncrService.getLastReport();
        return report == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(report);
    }
}
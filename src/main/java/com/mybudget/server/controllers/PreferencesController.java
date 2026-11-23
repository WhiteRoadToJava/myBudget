package com.mybudget.server.controllers;


import com.mybudget.server.dto.user.PreferencesRequest;
import com.mybudget.server.dto.user.PreferencesResponse;
import com.mybudget.server.services.user.PreferencesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/preferences")
@RequiredArgsConstructor
public class PreferencesController {

    private final PreferencesService preferencesService;

    @GetMapping
    public ResponseEntity<PreferencesResponse> getPreferences() {
        return ResponseEntity.ok(preferencesService.getPreferences());
    }

    @PatchMapping
    public ResponseEntity<PreferencesResponse> updatePreferences(@RequestBody PreferencesRequest request) {
        return ResponseEntity.ok(preferencesService.updatePreferences(request));
    }
}

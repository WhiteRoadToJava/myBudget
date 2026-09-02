package com.mybudget.server.controllers;


import com.mybudget.server.services.mongo.MongoSyncrService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mongo")
@RequiredArgsConstructor
public class MongoController {

    
    private final MongoSyncrService mongoSyncrService;

    @PostMapping("/pull")
    public ResponseEntity<String> pullAllFromAtlas(){
        String result = mongoSyncrService.pullAllfromAtlasToLocal();
        return ResponseEntity.ok(result);
    }
}

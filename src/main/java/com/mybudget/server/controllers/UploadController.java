package com.mybudget.server.controllers;

import com.mybudget.server.services.UploadServer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.util.Map;


@RestController
@RequiredArgsConstructor
@RequestMapping("/user/upload")
public class UploadController {

    private final UploadServer uploadServer;
    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file")MultipartFile file) throws IOException {
        Map<String,String> response = uploadServer.uploadimage(file);
        return ResponseEntity.ok(response);
    }
}

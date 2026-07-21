package com.mybudget.server.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/user/upload")
public class UploadController {

    @Value("${upload.dir}")
    private String uploadDir;

    @Value("${server.address:localhost}")
    private String serverAddress;

    @Value("${server.port:8080}")
    private String serverPort;
    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file")MultipartFile file) throws IOException {
        String filename = UUID.randomUUID() + "_" + StringUtils.cleanPath(file.getOriginalFilename());
        Path tragetPath = Paths.get(uploadDir).resolve(filename);
        Files.createDirectories(tragetPath.getParent());
        Files.copy(file.getInputStream(), tragetPath, StandardCopyOption.REPLACE_EXISTING);


        String fullUrl = "http://" + serverAddress + ":" +  serverPort + "/user/images/" + filename;

        Map<String, String> response = new HashMap<>();
        response.put("filename", filename);
        response.put("url", fullUrl);

        return ResponseEntity.ok(response);
    }
}

package com.mybudget.server.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@Service
public class UploadServer {

    @Value("${upload.dir}")
    private String uploadDir;

    @Value("${server.address:localhost}")
    private String serverAddress;

    @Value("${server.port:8080}")
    private String serverPort;


    public Map<String, String> uploadimage(MultipartFile requestedFile) throws IOException {
        String filenme = UUID.randomUUID() + "_" + StringUtils.cleanPath((requestedFile.getOriginalFilename()));
        Path targetPath = Paths.get(uploadDir).resolve(filenme);
        Files.createDirectories(targetPath.getParent());
        Files.copy(requestedFile.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        String fullUrl = "http://" + serverAddress + ":" + serverPort + "/user/images" + filenme;

        Map<String, String> response = new HashMap<>();
        response.put("filename", filenme);
        response.put("url", fullUrl);
        return response;
    }
}

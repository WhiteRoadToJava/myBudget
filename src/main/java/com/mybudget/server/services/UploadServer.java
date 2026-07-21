package com.mybudget.server.services;

import org.springframework.beans.factory.annotation.Value;

public class UploadServer {


    @Value("${upload.dir")
    private String upload;


}

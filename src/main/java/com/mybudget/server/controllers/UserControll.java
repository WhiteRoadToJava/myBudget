package com.mybudget.server.controllers;

import org.springframework.web.bind.annotation.GetMapping;

public class UserControll {
    public UserControll() {
        
    }
    @GetMapping("/user")
        public String getUser() {
            return "Hello, User!";
        }   
}
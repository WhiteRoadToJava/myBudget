package com.mybudget.server.exeptions;

public class ConflictException extends RuntimeException{
    public ConflictException(String message){
        super(message);
    }
}

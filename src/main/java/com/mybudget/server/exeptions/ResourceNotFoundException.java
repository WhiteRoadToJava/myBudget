package com.mybudget.server.exeptions;



public class ResourceNotFoundException extends  RuntimeException {
    public  ResourceNotFoundException(String message){
        super(message);
    }
}

package com.mybudget.server.exeptions;

public class InvalidTokenException extends RuntimeException{
    public enum Reason {TOKEN_INVALID, TOKEN_EXPIRED}
    private final Reason reason;


    public InvalidTokenException(Reason reason){
        super(reason.name());
        this.reason = reason;
    }

    public Reason getReason(){
        return reason;
    }
}

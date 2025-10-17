package com.ivoyant.booking_service.exception;

public class SlotNotFoundException extends RuntimeException{
    public SlotNotFoundException(String message){
        super(message);
    }
}

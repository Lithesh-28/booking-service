package com.ivoyant.booking_service.exception;

public class VehicleNotFoundException extends RuntimeException{
    public VehicleNotFoundException(String message){
        super(message);
    }
}

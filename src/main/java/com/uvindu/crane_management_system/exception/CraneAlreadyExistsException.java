package com.uvindu.crane_management_system.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class CraneAlreadyExistsException extends RuntimeException {

    public CraneAlreadyExistsException(String message) {
        super(message);
    }
}
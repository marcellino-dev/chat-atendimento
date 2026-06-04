package com.empresa.chat.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
    public static NotFoundException of(String entidade, Long id) {
        return new NotFoundException(entidade + " não encontrado(a) com id: " + id);
    }
}

package org.example.shared.exceptions;

public class InvalidRequestException extends RuntimeException{
    public InvalidRequestException(String  message) {
        super(message);
    }
}

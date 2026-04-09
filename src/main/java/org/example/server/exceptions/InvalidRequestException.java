package org.example.server.exceptions;

public class InvalidRequestException extends RuntimeException{
    public InvalidRequestException(String  message) {
        super(message);
    }
}

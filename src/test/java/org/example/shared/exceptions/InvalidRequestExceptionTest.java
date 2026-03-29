package org.example.shared.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class InvalidRequestExceptionTest {

    @Test
    void isException() {
        InvalidRequestException e = new InvalidRequestException("msg");
        assertInstanceOf(Exception.class, e);
    }

    @Test
    void getMessage_returnsProvidedMessage() {
        InvalidRequestException e = new InvalidRequestException("invalid move");
        assertEquals("invalid move", e.getMessage());
    }

    @Test
    void canBeThrownAndCaught() {
        assertThrows(InvalidRequestException.class, () -> {
            throw new InvalidRequestException("test");
        });
    }
}

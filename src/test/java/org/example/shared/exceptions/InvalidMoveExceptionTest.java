package org.example.shared.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class InvalidMoveExceptionTest {

    @Test
    void isException() {
        InvalidMoveException e = new InvalidMoveException("msg");
        assertInstanceOf(Exception.class, e);
    }

    @Test
    void getMessage_returnsProvidedMessage() {
        InvalidMoveException e = new InvalidMoveException("invalid move");
        assertEquals("invalid move", e.getMessage());
    }

    @Test
    void canBeThrownAndCaught() {
        assertThrows(InvalidMoveException.class, () -> {
            throw new InvalidMoveException("test");
        });
    }
}

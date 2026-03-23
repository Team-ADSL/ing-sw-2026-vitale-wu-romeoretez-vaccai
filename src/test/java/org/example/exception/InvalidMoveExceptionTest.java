package org.example.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class InvalidMoveExceptionTest {

    @Test
    void constructor_storesMessage() {
        InvalidMoveException ex = new InvalidMoveException("bad move");
        assertEquals("bad move", ex.getMessage());
    }

    @Test
    void isException_isCheckedException() {
        assertInstanceOf(Exception.class, new InvalidMoveException("test"));
    }

    @Test
    void thrownAndCaught_preservesMessage() {
        String msg = "invalid input";
        Exception caught = assertThrows(InvalidMoveException.class, () -> {
            throw new InvalidMoveException(msg);
        });
        assertEquals(msg, caught.getMessage());
    }
}

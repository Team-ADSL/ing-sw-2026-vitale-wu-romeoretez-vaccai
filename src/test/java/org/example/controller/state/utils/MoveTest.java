package org.example.controller.state.utils;

import org.example.controller.utils.Move;
import org.example.controller.utils.Row;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MoveTest {

    @Test
    void getRowIndex_returnsCorrectValue() {
        Move move = new Move(3, Row.UPPER);
        assertEquals(3, move.getRowIndex());
    }

    @Test
    void getRow_returnsCorrectValue() {
        Move move = new Move(0, Row.LOWER);
        assertEquals(Row.LOWER, move.getRow());
    }

    @Test
    void getRow_offerRow_returnsCorrectValue() {
        Move move = new Move(1, Row.OFFER);
        assertEquals(Row.OFFER, move.getRow());
    }
}

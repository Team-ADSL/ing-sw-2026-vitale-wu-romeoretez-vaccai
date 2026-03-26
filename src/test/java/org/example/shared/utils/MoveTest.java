package org.example.shared.utils;

import org.example.shared.enums.Row;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MoveTest {

    @Test
    void getRowIndex_returnsCorrectValue() {
        Move move = new Move(2, Row.UPPER);
        assertEquals(2, move.getRowIndex());
    }

    @Test
    void getRow_returnsCorrectValue() {
        Move move = new Move(0, Row.LOWER);
        assertEquals(Row.LOWER, move.getRow());
    }

    @Test
    void getRow_offer_returnsOffer() {
        Move move = new Move(1, Row.OFFER);
        assertEquals(Row.OFFER, move.getRow());
    }
}

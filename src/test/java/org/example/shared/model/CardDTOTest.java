package org.example.shared.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CardDTOTest {
    @Test
    void getId_withId_returnsCorrectId() {
        CardDTO dto = new CardDTO("hunter_01");
        assertEquals("hunter_01", dto.id());
    }
}

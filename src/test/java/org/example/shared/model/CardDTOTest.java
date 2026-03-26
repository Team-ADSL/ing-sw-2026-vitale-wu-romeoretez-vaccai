package org.example.shared.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CardDTOTest {

    @Test
    void getId_defaultValue_returnsZero() {
        CardDTO dto = new CardDTO();
        assertEquals(0, dto.getId());
    }

    @Test
    void implementsRenderable() {
        CardDTO dto = new CardDTO();
        assertInstanceOf(Renderable.class, dto);
    }
}

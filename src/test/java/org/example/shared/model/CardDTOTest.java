package org.example.shared.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CardDTOTest {

    @Test
    void getId_defaultValue_returnsNull() {
        CardDTO dto = new CardDTO();
        assertNull(dto.getId());
    }

    @Test
    void getId_withId_returnsCorrectId() {
        CardDTO dto = new CardDTO("hunter_01");
        assertEquals("hunter_01", dto.getId());
    }

    @Test
    void implementsRenderable() {
        CardDTO dto = new CardDTO();
        assertInstanceOf(Renderable.class, dto);
    }

    @Test
    void twoInstancesWithSameId_areIndependent() {
        CardDTO dto1 = new CardDTO("a");
        CardDTO dto2 = new CardDTO("b");
        assertNotEquals(dto1.getId(), dto2.getId());
    }
}

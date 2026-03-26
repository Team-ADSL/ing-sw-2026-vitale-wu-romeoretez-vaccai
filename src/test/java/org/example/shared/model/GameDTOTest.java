package org.example.shared.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GameDTOTest {

    @Test
    void canBeInstantiated() {
        assertDoesNotThrow(() -> new GameDTO());
    }
}

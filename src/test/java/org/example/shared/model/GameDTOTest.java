package org.example.shared.model;

import org.example.shared.enums.Phase;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

public class GameDTOTest {

    @Test
    void canBeInstantiated() {
        assertDoesNotThrow(() -> new GameDTO(100, 5, 5,0, new HashSet<>(), null, null, null));
    }
}
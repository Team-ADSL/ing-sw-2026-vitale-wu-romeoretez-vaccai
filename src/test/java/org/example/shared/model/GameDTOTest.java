package org.example.shared.model;

import org.example.shared.enums.Phase;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class GameDTOTest {

    @Test
    void canBeInstantiated() {
        assertDoesNotThrow(() -> new GameDTO(0, 1, new HashSet<>(), Optional.empty(), null, Phase.LOBBY));
    }
}
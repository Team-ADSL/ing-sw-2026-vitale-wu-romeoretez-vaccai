package org.example.server.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ControllerStateFactoryTest {

    @Test
    void canBeInstantiated() {
        assertDoesNotThrow(() -> new StateFactory());
    }
}

package org.example.server.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ServerControllerTest {

    @Test
    void canBeInstantiated() {
        assertDoesNotThrow(() -> new ServerController());
    }
}

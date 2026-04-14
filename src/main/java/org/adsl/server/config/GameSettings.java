package org.adsl.server.config;

public record GameSettings(
        int numLowTribeCard,
        int numTopTribeCard,
        int numBuildingEra1,
        int numBuildingEra2,
        int numBuildingEra3
) {}

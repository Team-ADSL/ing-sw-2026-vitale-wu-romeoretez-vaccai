package org.adsl.server.config;

/**
 * Immutable configuration values for a game session scaled to a specific player count.
 *
 * @param numLowTribeCard  number of tribe-card slots in the lower card row
 * @param numTopTribeCard  number of tribe-card slots in the upper card row
 * @param numBuildingEra1  building cards exposed at era 1
 * @param numBuildingEra2  building cards exposed at era 2
 * @param numBuildingEra3  building cards exposed at era 3
 */
public record GameSettings(
        int numLowTribeCard,
        int numTopTribeCard,
        int numBuildingEra1,
        int numBuildingEra2,
        int numBuildingEra3
) {}

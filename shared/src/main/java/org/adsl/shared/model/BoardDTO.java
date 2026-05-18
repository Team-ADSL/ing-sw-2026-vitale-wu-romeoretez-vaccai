package org.adsl.shared.model;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Immutable snapshot of the game board sent to clients on each game update.
 *
 * @param lowRow             cards in the lower tribe-card row (null slots included)
 * @param topRow             cards in the upper tribe-card row (null slots included)
 * @param offerTrack         current offer-tile occupancy and move allowances
 * @param orderTile          current turn-order track with per-cell bonus/malus
 * @param remainingBuildings one {@code boolean} per future-era building deck;
 *                           {@code true} means that deck is non-empty
 * @param isDeckEmpty        {@code true} when the main draw pile is exhausted
 */
public record BoardDTO(
        ArrayList<CardDTO> lowRow,
        ArrayList<CardDTO> topRow,
        ArrayList<OfferTileDTO> offerTrack,
        OrderTileDTO orderTile,
        ArrayList<Boolean> remainingBuildings, // indicate the presence of the related deck
        boolean isDeckEmpty
) implements Serializable {}

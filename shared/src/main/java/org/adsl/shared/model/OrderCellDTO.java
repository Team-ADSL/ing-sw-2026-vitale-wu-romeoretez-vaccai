package org.adsl.shared.model;

import org.adsl.shared.enums.Totem;

import java.io.Serializable;

/**
 * Immutable snapshot of a single order-tile cell sent inside {@code OrderTileDTO}.
 *
 * @param totem   totem of the player in this cell, or {@code null} if empty
 * @param bonus   food tokens granted when a player places here ({@code > 0}),
 *                or the magnitude of a penalty ({@code < 0}) when {@code isMalus} is true
 * @param isMalus {@code true} if this cell penalises the player (lose 1 food or 2 PP)
 */
public record OrderCellDTO(Totem totem, int bonus, boolean isMalus) implements Serializable {}

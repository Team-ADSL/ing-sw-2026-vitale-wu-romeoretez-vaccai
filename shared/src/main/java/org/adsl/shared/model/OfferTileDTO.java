package org.adsl.shared.model;

import org.adsl.shared.enums.Totem;

import java.io.Serializable;

import java.util.Map;
import org.adsl.shared.enums.Row;

/**
 * Immutable snapshot of a single offer-tile slot sent inside {@code BoardDTO}.
 *
 * @param id        unique tile identifier
 * @param totem     totem of the player occupying this slot, or {@code null} if empty
 * @param moves     allowed card draws per row ({@code Row.UPPER}/{@code Row.LOWER})
 * @param givesFood {@code true} if this tile grants 3 food instead of card draws
 */
public record OfferTileDTO(String id, Totem totem, Map<Row, Integer> moves, boolean givesFood)
implements Serializable {}

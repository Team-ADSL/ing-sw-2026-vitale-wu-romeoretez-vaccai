package org.adsl.shared.model;

import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Totem;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * Immutable snapshot of a player's state sent as part of {@code GameDTO}.
 *
 * @param name                player username
 * @param food                current food tokens
 * @param pp                  current prestige points
 * @param totem               the player's chosen totem colour
 * @param cards               all cards in the player's hand, grouped by {@code CardType}
 * @param builderPP           total end-game PP from all builders in hand
 * @param builderDiscount     total food discount granted by all builders in hand
 * @param gathererDiscount    total food discount granted by all gatherers in hand
 * @param shamanStars         total shaman stars across all shamans in hand
 * @param inventorUniqueIcons number of distinct inventor icons in hand
 */
public record PlayerDTO(
        String name,
        int food,
        int pp,
        Totem totem,
        Map<CardType, Set<CardDTO>> cards,
        int builderPP,
        int builderDiscount,
        int gathererDiscount,
        int shamanStars,
        int inventorUniqueIcons
) implements Serializable {}

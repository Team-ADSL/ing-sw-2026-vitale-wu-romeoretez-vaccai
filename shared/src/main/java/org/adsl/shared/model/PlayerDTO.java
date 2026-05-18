package org.adsl.shared.model;

import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Totem;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * Immutable snapshot of a player's state sent as part of {@code GameDTO}.
 *
 * @param name  player username
 * @param food  current food tokens
 * @param pp    current prestige points
 * @param totem the player's chosen totem colour
 * @param cards all cards in the player's hand, grouped by {@code CardType}
 */
public record PlayerDTO(
        String name,
        int food,
        int pp,
        Totem totem,
        Map<CardType, Set<CardDTO>> cards
) implements Serializable {}

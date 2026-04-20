package org.adsl.shared.model;

import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Totem;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public record PlayerDTO(
        String name,
        int food,
        int pp,
        Totem totem,
        Map<CardType, Set<CardDTO>> cards
) implements Serializable {}

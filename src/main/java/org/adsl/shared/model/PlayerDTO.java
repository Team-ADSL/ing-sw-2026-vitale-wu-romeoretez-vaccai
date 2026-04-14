package org.adsl.shared.model;

import org.adsl.shared.enums.CardType;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public record PlayerDTO(
        Map<CardType, Set<CardDTO>> cards
) implements Serializable {}

package org.example.shared.model;

import org.example.shared.enums.CardType;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public record PlayerDTO(
        Map<CardType, Set<CardDTO>> cards
) implements Serializable {}

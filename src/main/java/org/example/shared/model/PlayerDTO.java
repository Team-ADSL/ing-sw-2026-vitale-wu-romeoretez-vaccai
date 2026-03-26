package org.example.shared.model;

import org.example.shared.enums.CardType;

import java.util.Map;
import java.util.Set;

public record PlayerDTO(
        int id,
        Map<CardType, Set<CardDTO>> cards
) implements Renderable{}

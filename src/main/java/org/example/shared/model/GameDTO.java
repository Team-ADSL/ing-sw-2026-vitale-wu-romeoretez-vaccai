package org.example.shared.model;

import org.example.shared.enums.Phase;

import java.io.Serializable;
import java.util.Optional;
import java.util.Set;

public record GameDTO (
        int round,
        int era,
        Set<PlayerDTO> players,
        Optional<PlayerDTO> currentPlayer,
        BoardDTO board,
        Phase phase
) implements Serializable { }

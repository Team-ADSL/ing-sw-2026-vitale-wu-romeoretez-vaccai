package org.example.shared.model;

import org.example.shared.enums.Phase;

import java.io.Serializable;
import java.util.Set;

public record GameDTO (
        int id,
        int numPlayer,
        int round,
        int era,
        Set<PlayerDTO> players,
        PlayerDTO currentPlayer,
        BoardDTO board,
        Phase phase
) implements Serializable { }

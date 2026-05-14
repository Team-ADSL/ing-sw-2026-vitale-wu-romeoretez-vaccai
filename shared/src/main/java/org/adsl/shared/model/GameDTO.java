package org.adsl.shared.model;

import org.adsl.shared.enums.Phase;
import org.adsl.shared.enums.Totem;

import java.io.Serializable;
import java.util.Set;

public record GameDTO(
        int id,
        int numPlayer,
        int round,
        int era,
        Set<PlayerDTO> players,
        BoardDTO board,
        Phase phase,
        Totem currentPlayerTotem
) implements Serializable { }

package org.adsl.shared.model;

import org.adsl.shared.enums.Phase;
import org.adsl.shared.enums.Totem;

import java.io.Serializable;
import java.util.Set;

/**
 * Immutable full snapshot of a game session sent to all clients after every
 * state change. The client renders the entire UI from this record.
 *
 * @param id                 unique game identifier
 * @param numPlayer          total number of players in this game
 * @param round              current round number (1–10)
 * @param era                current era (1–3)
 * @param players            snapshot of all players' resources and cards
 * @param board              snapshot of the board (rows, offer track, order tile)
 * @param phase              current game phase
 * @param currentPlayerTotem totem of the player whose turn it is, or {@code null}
 */
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

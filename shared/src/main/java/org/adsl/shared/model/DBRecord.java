package org.adsl.shared.model;

import java.io.Serializable;

/**
 * A single entry in the end-game leaderboard, returned by
 * {@code GameDAO.getLeaderboard()} and broadcast to clients via
 * {@code GameEnded}.
 *
 * @param rank     dense rank position (1 = best; ties share the same rank)
 * @param nickname player username
 * @param score    cumulative prestige points across all matches of this player count
 */
public record DBRecord(
        int rank,
        String nickname,
        int score)
implements Serializable {}

package org.adsl.shared.model;

import java.io.Serializable;

/**
 * Final result of a single player at the end of a match, sent to clients as
 * part of the end-game results (see {@code GameEnded}).
 *
 * @param nickname player username
 * @param pp       final prestige points scored by the player
 * @param food     final amount of food tokens held by the player
 */
public record MatchResult(String nickname, int pp, int food)
        implements Serializable {}

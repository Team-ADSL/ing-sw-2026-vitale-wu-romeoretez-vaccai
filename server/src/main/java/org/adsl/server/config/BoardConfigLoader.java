package org.adsl.server.config;

import org.adsl.server.model.board.OfferTrack;
import org.adsl.server.model.board.OrderTile;
import org.adsl.server.model.cards.Card;
import org.adsl.server.model.cards.buildings.Building;

import java.util.ArrayList;
import java.util.Set;

/**
 * Strategy interface for loading board configuration from an external source.
 * <p>
 * The production implementation is {@link JsonBoardConfigLoader}, which reads
 * JSON resource files bundled in the classpath.
 * </p>
 */
public interface BoardConfigLoader {

    /**
     * Returns card sets grouped by era (index 0 = era 1, 1 = era 2, 2 = era 3,
     * index 3 = final events). Each set contains all cards valid for the given
     * player count.
     *
     * @param numPlayers number of players (2–5)
     * @return list of card sets, one per era plus one for final events
     */
    ArrayList<Set<Card>> getCards(int numPlayers);

    /**
     * Returns the offer track populated with tiles valid for the given player count.
     *
     * @param numPlayers number of players (2–5)
     * @return fully constructed {@link OfferTrack}
     */
    OfferTrack getOfferTrack(int numPlayers);

    /**
     * Returns the order tile (turn-order track) matching the given player count.
     *
     * @param numPlayers number of players (2–5)
     * @return {@link OrderTile} for that player count
     */
    OrderTile getOrderTile(int numPlayers);

    /**
     * Returns all building cards across all eras as a flat set.
     * Era separation and per-era selection are handled by
     * {@code InitGameState.makeBuildingDecks()}.
     *
     * @return set of all available {@link Building} cards
     */
    Set<Building> getBuildings();

    /**
     * Returns gameplay settings (row sizes, building counts per era) for the
     * given player count.
     *
     * @param numPlayers number of players (2–5)
     * @return {@link GameSettings} for that player count
     */
    GameSettings getSettings(int numPlayers);
}

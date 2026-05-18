package org.adsl.server.model.board;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.model.BoardDTO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable record holding all board components for a game session.
 *
 * @param lowRow             the lower tribe-card row (cards moved from top row at end of round)
 * @param topRow             the upper tribe-card row (freshly drawn from deck each round)
 * @param offerTrack         the offer track where players place their totems to choose an action
 * @param orderTile          the turn-order track that determines player sequence
 * @param remainingBuildings building decks queued for future eras (era 2 at index 0, era 3 at index 1)
 * @param deck               the shuffled draw pile of tribe cards and events
 */
public record Board(CardRow lowRow, CardRow topRow, OfferTrack offerTrack, OrderTile orderTile,
                    ArrayList<Set<Card>> remainingBuildings, Deck deck) implements Serializable {
    public Board(int numLowCard, int numLowTribeCards, int numTopCard, int numTopTribeCards,
                 OfferTrack offerTrack, OrderTile orderQueue, Deck deck) {
        this(new CardRow(numLowCard, numLowTribeCards), new CardRow(numTopCard, numTopTribeCards),
                offerTrack, orderQueue, new ArrayList<>(), deck);
    }

    public BoardDTO createDTO() {
        ArrayList<Boolean> remainingBuildingsDTO = (ArrayList<Boolean>) remainingBuildings.stream()
                .map(s -> !s.isEmpty())
                .collect(Collectors.toList());
        return new BoardDTO(lowRow.createDTO(), topRow.createDTO(), offerTrack.createDTO(),
                orderTile.createDTO(), remainingBuildingsDTO, deck.isEmpty());
    }
}

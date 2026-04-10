package org.example.server.model.board;

import org.example.server.model.cards.Card;
import org.example.shared.model.BoardDTO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

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

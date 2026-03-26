package org.example.server.model.board;

import org.example.server.model.cards.Card;

import java.util.ArrayList;
import java.util.Set;

public class Board {
    private final CardRow lowRow;
    private final CardRow topRow;
    private final OfferTrack offerTrack;
    private final OrderTile orderQueue;
    private final ArrayList<Set<Card>> remainingBuildings;
    private final Deck deck;

    public Board (CardRow lowRow, CardRow topRow, OfferTrack offerTrack,
                  OrderTile orderQueue, ArrayList<Set<Card>> remainingBuildings, ArrayList<Set<Card>> cards) {
        this.lowRow = lowRow;
        this.topRow = topRow;
        this.offerTrack = offerTrack;
        this.orderQueue = orderQueue;
        this.remainingBuildings = remainingBuildings;
        this.deck = Deck.createDeck(cards);
    }

    // Initialiser
    public Board (int numLowCard, int numTopCard, OfferTrack offerTrack,
                  OrderTile orderQueue, ArrayList<Set<Card>> cards, int nonBuildingCards) {
        this.lowRow = new CardRow(numLowCard, nonBuildingCards);
        this.topRow = new CardRow(numTopCard, nonBuildingCards);
        this.offerTrack = offerTrack;
        this.orderQueue = orderQueue;
        this.remainingBuildings = new ArrayList<Set<Card>>();
        this.deck = Deck.createDeck(cards);
    }

    public OfferTrack getOfferTrack() {
        return offerTrack;
    }
    public OrderTile getOrderTile() {
        return orderQueue;
    }
    public Deck getDeck() {
        return deck;
    }
    public CardRow getLowRow() {
        return lowRow;
    }
    public CardRow getTopRow() {
        return topRow;
    }

    public ArrayList<Set<Card>> getRemainingBuildings() {
        return remainingBuildings;
    }
}

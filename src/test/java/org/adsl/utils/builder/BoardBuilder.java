package org.adsl.utils.builder;

import org.adsl.server.model.board.*;
import org.adsl.server.model.cards.Card;
import org.adsl.utils.fakes.FakeCardRow;

import java.util.ArrayList;
import java.util.Set;


public class BoardBuilder {
    public CardRow lowRow = new FakeCardRow();
    public CardRow topRow = new FakeCardRow();
    public OfferTrack offerTrack;
    public OrderTile orderTile;
    public ArrayList<Set<Card>> remainingBuildings = new ArrayList<>();
    public Deck deck = new Deck();


    public Board build() {
        return new Board(lowRow, topRow, offerTrack, orderTile,
                remainingBuildings, deck);
    }

    public BoardBuilder withTopRow(CardRow cardRow) {
        topRow = cardRow;
        return this;
    }

    public BoardBuilder withLowRow(CardRow cardRow) {
        lowRow = cardRow;
        return this;
    }

}

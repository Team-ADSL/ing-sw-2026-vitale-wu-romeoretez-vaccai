package org.adsl.utils.fakes;

import org.adsl.server.model.board.CardRow;

import java.util.ArrayList;

public class FakeCardRow extends CardRow {
    public ArrayList<FakeCard> tribeCards;
    public ArrayList<FakeCard> buildings;

    public FakeCardRow(int numCard, int numTribeCards) {
        super(numCard, numTribeCards);
        this.tribeCards = new ArrayList<>();
        this.buildings = new ArrayList<>();
    }


}

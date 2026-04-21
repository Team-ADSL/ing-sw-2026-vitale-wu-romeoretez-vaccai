package org.adsl.utils.fakes;

import org.adsl.server.model.board.CardRow;
import org.adsl.server.model.cards.Card;

import java.util.ArrayList;

public class FakeCardRow extends CardRow {
    public FakeCardRow(int numCard, int numTribeCards) {
        super(numCard, numTribeCards);
    }
}

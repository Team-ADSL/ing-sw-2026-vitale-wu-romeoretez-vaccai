package org.adsl.utils.builder;

import org.adsl.server.model.board.*;
import org.adsl.server.model.cards.Card;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BoardBuilder {
    public CardRow lowRow = null;
    public CardRow topRow = null;
    public OfferTrack offerTrack;
    public OrderTile orderTile;
    public ArrayList<Set<Card>> remainingBuildings = new ArrayList<>();
    public Deck deck = new Deck();

    public static class Param {
        public int numCardTopRow;
        public int numTribeCardTopRow;
        public int numCardLowRow;
        public int numTribeCardLowRow;

        public Param(int numCardTopRow, int numTribeCardTopRow, int numCardLowRow, int numTribeCardLowRow) {
            this.numCardTopRow = numCardTopRow;
            this.numTribeCardTopRow = numTribeCardTopRow;
            this.numCardLowRow = numCardLowRow;
            this.numTribeCardLowRow = numTribeCardLowRow;
        }
    }

    public Map<Integer, Param> parameters;

    public BoardBuilder() {
        this.parameters = new HashMap<>();
        parameters.put(2, new Param(9, 6, 6, 3));
        parameters.put(3, new Param(11, 7, 8, 4));
        parameters.put(4, new Param(12, 8, 9, 5));
        parameters.put(5, new Param(14, 9, 11, 6));
    }

    public Board build(int numPlayer) {
        Param param = parameters.get(numPlayer);
        return new Board(param.numCardLowRow, param.numTribeCardLowRow,
                param.numCardTopRow, param.numTribeCardTopRow,
                offerTrack, orderTile, deck);
    }
}

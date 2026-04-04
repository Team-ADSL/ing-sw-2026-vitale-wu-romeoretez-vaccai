package org.example.server.config;

import org.example.server.model.board.OfferTrack;
import org.example.server.model.board.OrderTile;
import org.example.server.model.cards.Card;
import org.example.server.model.cards.buildings.Building;

import java.util.ArrayList;
import java.util.Set;

public interface BoardConfigLoader {
    ArrayList<Set<Card>> getCards(int numPlayers);
    OfferTrack getOfferTrack(int numPlayers);
    OrderTile getOrderTile(int numPlayers);
    Set<Building> getBuildings();
}

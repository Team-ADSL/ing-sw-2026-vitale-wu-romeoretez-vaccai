package org.adsl.server.config;

import org.adsl.server.model.board.OfferTrack;
import org.adsl.server.model.board.OrderTile;
import org.adsl.server.model.cards.Card;
import org.adsl.server.model.cards.buildings.Building;

import java.util.ArrayList;
import java.util.Set;

public interface BoardConfigLoader {
    ArrayList<Set<Card>> getCards(int numPlayers);
    OfferTrack getOfferTrack(int numPlayers);
    OrderTile getOrderTile(int numPlayers);
    Set<Building> getBuildings();
    GameSettings getSettings(int numPlayers);
}

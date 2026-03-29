package org.example.server.controller.states;

import org.example.server.model.Game;
import org.example.server.model.Player;
import org.example.server.model.board.Board;
import org.example.server.model.board.CardRow;
import org.example.server.model.board.OfferTrack;
import org.example.server.model.board.OrderTile;
import org.example.server.model.cards.buildings.Building;
import org.example.shared.enums.Color;
import org.example.shared.enums.Trigger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class InitGameStateTest {

    private static class StubBuilding extends Building {
        public StubBuilding(int era) {
            super(0, 0, era, Optional.empty());
        }
        @Override
        public void activeEffect(Set<Player> players, Trigger t) {}
    }

    private InitGameState state;
    private Game game;

    @BeforeEach
    void setUp() {
        CardRow lowRow = new CardRow(3, 3);
        CardRow topRow = new CardRow(10, 0);
        OfferTrack offerTrack = new OfferTrack(new ArrayList<>());
        OrderTile orderTile = new OrderTile(new ArrayList<>());
        Board board = new Board(lowRow, topRow, offerTrack, orderTile, new ArrayList<>(), new ArrayList<>());
        Player p = new Player("P1", 10, 0, Color.RED);
        Set<Player> players = new HashSet<>();
        players.add(p);
        game = new Game(1, 1, players, p, board);
        state = new InitGameState(game);
    }

    @Test
    void calcNumTopCard_returnsNumPlayersPlus4() {
        assertEquals(6, state.calcNumTopCard(2));
        assertEquals(7, state.calcNumTopCard(3));
        assertEquals(8, state.calcNumTopCard(4));
    }

    @Test
    void calcNumLowCard_returnsNumPlayersPlus1() {
        assertEquals(3, state.calcNumLowCard(2));
        assertEquals(4, state.calcNumLowCard(3));
        assertEquals(5, state.calcNumLowCard(4));
    }

    @Test
    void makeBuildingDecks_2players_addsEra1ToTopRow() {
        Set<Building> buildings = buildBuildingSet(2, 3, 4);
        state.makeBuildingDecks(buildings, 2);
        // 2 players: era1 = 1, era2 = 2, era3 = 3
        assertEquals(1, game.getBoard().getTopRow().getBuildings().size());
    }

    @Test
    void makeBuildingDecks_2players_remainingHasEra2AndEra3() {
        Set<Building> buildings = buildBuildingSet(2, 3, 4);
        state.makeBuildingDecks(buildings, 2);
        ArrayList<Set<?>> remaining = (ArrayList) game.getBoard().getRemainingBuildings();
        assertEquals(2, remaining.size());
        assertEquals(2, remaining.get(0).size()); // era2
        assertEquals(3, remaining.get(1).size()); // era3
    }

    @Test
    void makeBuildingDecks_3players_addsEra1ToTopRow() {
        Set<Building> buildings = buildBuildingSet(3, 3, 5);
        state.makeBuildingDecks(buildings, 3);
        // 3 players: era1 = 2
        assertEquals(2, game.getBoard().getTopRow().getBuildings().size());
    }

    @Test
    void checkMove_doesNotThrow() {
        assertDoesNotThrow(() -> state.checkMove(new HashSet<>(), null));
    }

    @Test
    void nextState_returnsNull() {
        assertNull(state.nextState());
    }

    // Builds a set with the given number of buildings per era
    private Set<Building> buildBuildingSet(int numEra1, int numEra2, int numEra3) {
        Set<Building> set = new HashSet<>();
        for (int i = 0; i < numEra1; i++) set.add(new StubBuilding(1));
        for (int i = 0; i < numEra2; i++) set.add(new StubBuilding(2));
        for (int i = 0; i < numEra3; i++) set.add(new StubBuilding(3));
        return set;
    }
}

package org.adsl.server.controller.states;

import org.adsl.server.config.BoardConfigLoader;
import org.adsl.server.config.GameSettings;
import org.adsl.server.config.JsonBoardConfigLoader;
import org.adsl.server.controller.GameController;
import org.adsl.server.model.Game;
import org.adsl.server.model.board.Board;
import org.adsl.server.model.board.CardRow;
import org.adsl.server.model.board.Deck;
import org.adsl.server.model.cards.Card;
import org.adsl.server.model.cards.buildings.Building;
import org.adsl.server.persistence.GameDAO;
import org.adsl.server.persistence.GamePersistenceManager;
import org.adsl.utils.fakes.FakeGameDAO;
import org.adsl.utils.fakes.FakeGamePersistenceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class InitGameStateTest {

    private InitGameState state;
    private Game game;
    private BoardConfigLoader loader;
    private GameDAO gameDAO;
    private GamePersistenceManager persistenceManager;
    private GameController controller;

    @BeforeEach
    void setUp() {
        loader = new JsonBoardConfigLoader();
        gameDAO = new FakeGameDAO();
        persistenceManager = new FakeGamePersistenceManager();
        controller = new GameController(loader, persistenceManager, gameDAO);
        game = new Game(1, 5);
        state = new InitGameState(game, controller);
    }

    // ──────────────────────────────────────────────
    // TEST MAKE BUILDING DECKS
    // ──────────────────────────────────────────────

    @Test
    void testMakeBuildingDecks_populatesRemainingBuildings() {
        initBoardForGame(game, 2);
        state.makeBuildingDecks(loader.getBuildings(), 2, loader.getSettings(2));
        assertEquals(2, game.getBoard().remainingBuildings().size());
    }

    @Test
    void testMakeBuildingDecks_2players_addsOneEra1BuildingToTopRow() {
        initBoardForGame(game, 2);
        int before = countNonNull(game.getBoard().topRow());
        state.makeBuildingDecks(loader.getBuildings(), 2, loader.getSettings(2));
        assertEquals(1, countNonNull(game.getBoard().topRow()) - before);
    }

    @Test
    void testMakeBuildingDecks_3players_addsTwoEra1BuildingsToTopRow() {
        Game g = new Game(2, 5);
        initBoardForGame(g, 3);
        InitGameState s = new InitGameState(g, null);
        int before = countNonNull(g.getBoard().topRow());
        s.makeBuildingDecks(loader.getBuildings(), 3, loader.getSettings(3));
        assertEquals(2, countNonNull(g.getBoard().topRow()) - before);
    }

    @Test
    void testMakeBuildingDecks_remainingBuildingsIndex0HasEra2Cards() {
        initBoardForGame(game, 2);
        state.makeBuildingDecks(loader.getBuildings(), 2, loader.getSettings(2));
        List<Set<Card>> remaining = game.getBoard().remainingBuildings();
        remaining.getFirst().forEach(c -> assertEquals(2, ((Building) c).getEra()));
    }

    @Test
    void testMakeBuildingDecks_remainingBuildingsIndex1HasEra3Cards() {
        initBoardForGame(game, 2);
        state.makeBuildingDecks(loader.getBuildings(), 2, loader.getSettings(2));
        List<Set<Card>> remaining = game.getBoard().remainingBuildings();
        remaining.get(1).forEach(c -> assertEquals(3, ((Building) c).getEra()));
    }

    // ──────────────────────────────────────────────
    // TEST FILL LOW ROW
    // ──────────────────────────────────────────────

    @Test
    void testFillLowRow_2players_fillsThreeCharacterSlots() {
        initBoardForGame(game, 2);
        state.fillLowRow(2 + 1);
        ArrayList<Card> tribe = game.getBoard().lowRow().getTribeCards();
        int filled = 0;
        for (Card c : tribe) if (c != null) filled++;
        assertEquals(3, filled);
    }

    @Test
    void testFillLowRow_lowRowContainsOnlyDrawableCards() {
        initBoardForGame(game, 2);
        state.fillLowRow(2);
        for (Card c : game.getBoard().lowRow().getTribeCards()) {
            if (c != null) {
                assertTrue(c.canBeDrawn(null), "Low row should only contain character cards (canBeDrawn=true)");
            }
        }
    }

    // ──────────────────────────────────────────────
    // TEST FILL TOP ROW
    // ──────────────────────────────────────────────

    @Test
    void testFillTopRow_2players_fillsSixCards() {
        initBoardForGame(game, 2);

        state.fillTopRow(2 + 4);

        assertEquals(6, countNonNull(game.getBoard().topRow()));
    }

    // ──────────────────────────────────────────────
    // TEST CALC NEXT STATE
    // ──────────────────────────────────────────────

    @Test
    void testCalcNextState_defaultState_returnsTotemPlacementState() {
        assertInstanceOf(TotemPlacementState.class, state.calcNextState());
    }

    // ──────────────────────────────────────────────
    // TEST ON ENTRY - STARTING FOOD BONUS
    // ──────────────────────────────────────────────

    @Test
    void testOnEntry_5players_grantsStartingFoodBonusByOrderPosition() {
        for (int i = 0; i < 5; i++) {
            game.getPlayers().add(new org.adsl.server.model.Player("P" + i));
        }

        state.onEntry();

        int[] expectedBonus = {2, 3, 3, 4, 4};
        for (int i = 0; i < 5; i++) {
            int position = i;
            game.getBoard().orderTile().getPlayerAt(position)
                    .ifPresentOrElse(
                            p -> assertEquals(expectedBonus[position], p.getFood(),
                                    "Player at order position " + position + " should receive food bonus " + expectedBonus[position]),
                            () -> fail("No player at order position " + position)
                    );
        }
    }

    @Test
    void testOnEntry_alreadyInitialized_skipsFoodBonus() {
        for (int i = 0; i < 5; i++) {
            game.getPlayers().add(new org.adsl.server.model.Player("P" + i));
        }
        initBoardForGame(game, 5);
        game.getBoard().orderTile().placePlayersRandom(game.getPlayers());
        game.setInitialized(true);

        state.onEntry();

        for (org.adsl.server.model.Player p : game.getPlayers()) {
            assertEquals(0, p.getFood(), "Food bonus should not be granted again on a recovered game");
        }
    }

    // ──────────────────────────────────────────────
    // HELPERS
    // ──────────────────────────────────────────────

    private void initBoardForGame(Game g, int numPlayers) {
        GameController gc = new GameController(loader, persistenceManager, gameDAO);
        InitGameState s = new InitGameState(g, gc);
        Deck deck = Deck.createDeck(loader.getCards(numPlayers));
        GameSettings gameSettings = loader.getSettings(numPlayers);
        int maxBuildings = s.maxBuildings(gameSettings);
        Board board = new Board(
                gameSettings.numLowTribeCard() + maxBuildings,
                gameSettings.numLowTribeCard(),
                gameSettings.numTopTribeCard() + maxBuildings,
                gameSettings.numTopTribeCard(),
                loader.getOfferTrack(numPlayers),
                loader.getOrderTile(numPlayers),
                deck
        );
        g.setBoard(board);
    }

    private int countNonNull(CardRow row) {
        int count = 0;
        for (Card c : row.getTribeCards()) if (c != null) count++;
        count += row.getBuildings().size();
        return count;
    }
}

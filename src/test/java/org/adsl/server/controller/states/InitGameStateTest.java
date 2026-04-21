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
import org.adsl.shared.model.GameDTO;
import org.adsl.shared.model.MatchResult;
import org.adsl.utils.TestDummies;
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
    BoardConfigLoader loader = new TestDummies.DummyBoardConfigLoader();
    GameDAO gameDAO = new FakeGameDAO();
    GamePersistenceManager persistenceManager = new FakeGamePersistenceManager();

    @BeforeEach
    void setUp() {
        game = new Game(1, 5);
        GameController controller = new GameController(loader, persistenceManager,gameDAO);
        state = new InitGameState(game, controller);
    }

    // ──────────────────────────────────────────────
    // makeBuildingDecks
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

    @Test
    void makeBuildingDecks_populatesRemainingBuildings() {
        initBoardForGame(game, 2);
        state.makeBuildingDecks(loader.getBuildings(), 2, loader.getSettings(2));
        assertEquals(2, game.getBoard().remainingBuildings().size());
    }

    @Test
    void makeBuildingDecks_2players_addsOneEra1BuildingToTopRow() {
        initBoardForGame(game, 2);
        int before = countTopRowCards(game.getBoard().topRow());
        state.makeBuildingDecks(loader.getBuildings(), 2, loader.getSettings(2));
        assertEquals(1, countTopRowCards(game.getBoard().topRow()) - before);
    }

    @Test
    void makeBuildingDecks_3players_addsTwoEra1BuildingsToTopRow() {
        Game g = new Game(2, 5);
        initBoardForGame(g, 3);
        GameController gc = new GameController(loader, NO_OP_PERSISTENCE, NO_OP_DAO);
        InitGameState s = new InitGameState(g, gc);
        int before = countTopRowCards(g.getBoard().topRow());
        s.makeBuildingDecks(loader.getBuildings(), 3, loader.getSettings(3));
        assertEquals(2, countTopRowCards(g.getBoard().topRow()) - before);
    }

    @Test
    void makeBuildingDecks_remainingBuildingsIndex0HasEra2Cards() {
        initBoardForGame(game, 2);
        state.makeBuildingDecks(loader.getBuildings(), 2, loader.getSettings(2));
        List<Set<Card>> remaining = game.getBoard().remainingBuildings();
        remaining.get(0).forEach(c -> assertEquals(2, ((Building) c).getEra()));
    }

    @Test
    void makeBuildingDecks_remainingBuildingsIndex1HasEra3Cards() {
        initBoardForGame(game, 2);
        state.makeBuildingDecks(loader.getBuildings(), 2, loader.getSettings(2));
        List<Set<Card>> remaining = game.getBoard().remainingBuildings();
        remaining.get(1).forEach(c -> assertEquals(3, ((Building) c).getEra()));
    }

    // ──────────────────────────────────────────────
    // fillLowRow
    // ──────────────────────────────────────────────

    @Test
    void fillLowRow_2players_fillsThreeCharacterSlots() {
        initBoardForGame(game, 2);
        state.fillLowRow(2);
        ArrayList<Card> tribe = game.getBoard().lowRow().getTribeCards();
        int filled = 0;
        for (Card c : tribe) if (c != null) filled++;
        assertEquals(3, filled);
    }

    @Test
    void fillLowRow_lowRowContainsOnlyDrawableCards() {
        initBoardForGame(game, 2);
        state.fillLowRow(2);
        for (Card c : game.getBoard().lowRow().getTribeCards()) {
            if (c != null) {
                assertTrue(c.canBeDrawn(null), "Low row should only contain character cards (canBeDrawn=true)");
            }
        }
    }

    // ──────────────────────────────────────────────
    // fillTopRow
    // ──────────────────────────────────────────────

    @Test
    void fillTopRow_2players_fillsSixCards(){
        initBoardForGame(game, 2);

        assertEquals(6, game.getBoard().topRow().size());
    }

    // ──────────────────────────────────────────────
    // nextState
    // ──────────────────────────────────────────────

    @Test
    void nextState_returnsTotemPlacementState() {
        assertInstanceOf(TotemPlacementState.class, state.calcNextState());
    }

    // ──────────────────────────────────────────────
    // Helper
    // ──────────────────────────────────────────────

    private int countTopRowCards(CardRow row) {
        int count = 0;
        for (Card c : row.getTribeCards()) if (c != null) count++;
        count += row.getBuildings().size();
        return count;
    }
}

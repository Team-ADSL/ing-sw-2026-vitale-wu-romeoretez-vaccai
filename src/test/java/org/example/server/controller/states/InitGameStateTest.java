package org.example.server.controller.states;

import org.example.server.config.BoardConfigLoader;
import org.example.server.config.JsonBoardConfigLoader;
import org.example.server.controller.GameController;
import org.example.server.model.EndGameObserver;
import org.example.server.model.Game;
import org.example.server.model.Player;
import org.example.server.model.board.Board;
import org.example.server.model.board.CardRow;
import org.example.server.model.board.OfferTrack;
import org.example.server.model.board.OrderTile;
import org.example.server.model.cards.Card;
import org.example.server.model.cards.buildings.Building;
import org.example.server.persistence.GameDAO;
import org.example.server.persistence.GamePersistenceManager;
import org.example.shared.model.GameDTO;
import org.example.shared.model.MatchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class InitGameStateTest {

    private static final EndGameObserver NO_OP_END = (id, r) -> {};
    private static final GamePersistenceManager NO_OP_PERSISTENCE = new GamePersistenceManager() {
        @Override public List<Game> recoverGames() { return List.of(); }
        @Override public void removeGame(int id) {}
        @Override public void updateLobby(List<String> p) {}
        @Override public void updateGame(GameDTO g) {}
    };
    private static final GameDAO NO_OP_DAO = new GameDAO() {
        @Override public int createMatch() { return 0; }
        @Override public void deleteMatch(int id) {}
        @Override public void saveMatch(int id, int c, List<String> n, List<Integer> s) {}
        @Override public List<MatchResult> getLeaderboard(int c) { return List.of(); }
    };

    private BoardConfigLoader loader;
    private InitGameState state;
    private Game game;

    @BeforeEach
    void setUp() {
        loader = new JsonBoardConfigLoader();
        game = new Game(1, NO_OP_END);
        GameController controller = new GameController(loader, NO_OP_PERSISTENCE, NO_OP_DAO);
        state = new InitGameState(game, controller);
    }

    // ──────────────────────────────────────────────
    // calcNumTopCard
    // ──────────────────────────────────────────────

    @Test
    void calcNumTopCard_2players_returns6() {
        assertEquals(6, state.calcNumTopCard(2));
    }

    @Test
    void calcNumTopCard_3players_returns7() {
        assertEquals(7, state.calcNumTopCard(3));
    }

    @Test
    void calcNumTopCard_5players_returns9() {
        assertEquals(9, state.calcNumTopCard(5));
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5})
    void calcNumTopCard_isNumPlayersPlusFour(int n) {
        assertEquals(n + 4, state.calcNumTopCard(n));
    }

    // ──────────────────────────────────────────────
    // calcNumLowCard
    // ──────────────────────────────────────────────

    @Test
    void calcNumLowCard_2players_returns3() {
        assertEquals(3, state.calcNumLowCard(2));
    }

    @Test
    void calcNumLowCard_5players_returns6() {
        assertEquals(6, state.calcNumLowCard(5));
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5})
    void calcNumLowCard_isNumPlayersPlusOne(int n) {
        assertEquals(n + 1, state.calcNumLowCard(n));
    }

    // ──────────────────────────────────────────────
    // makeBuildingDecks
    // ──────────────────────────────────────────────

    private void initBoardForGame(Game g, int numPlayers) {
        GameController gc = new GameController(loader, NO_OP_PERSISTENCE, NO_OP_DAO);
        InitGameState s = new InitGameState(g, gc);
        Board board = new Board(
                s.calcNumLowCard(numPlayers),
                s.calcNumTopCard(numPlayers),
                loader.getOfferTrack(numPlayers),
                loader.getOrderTile(numPlayers),
                loader.getCards(numPlayers),
                numPlayers + 1
        );
        g.setBoard(board);
    }

    @Test
    void makeBuildingDecks_populatesRemainingBuildings() {
        initBoardForGame(game, 2);
        state.makeBuildingDecks(loader.getBuildings(), 2);
        assertEquals(2, game.getBoard().getRemainingBuildings().size());
    }

    @Test
    void makeBuildingDecks_2players_addsOneEra1BuildingToTopRow() {
        initBoardForGame(game, 2);
        int before = countTopRowCards(game.getBoard().getTopRow());
        state.makeBuildingDecks(loader.getBuildings(), 2);
        assertEquals(1, countTopRowCards(game.getBoard().getTopRow()) - before);
    }

    @Test
    void makeBuildingDecks_3players_addsTwoEra1BuildingsToTopRow() {
        Game g = new Game(2, NO_OP_END);
        initBoardForGame(g, 3);
        GameController gc = new GameController(loader, NO_OP_PERSISTENCE, NO_OP_DAO);
        InitGameState s = new InitGameState(g, gc);
        int before = countTopRowCards(g.getBoard().getTopRow());
        s.makeBuildingDecks(loader.getBuildings(), 3);
        assertEquals(2, countTopRowCards(g.getBoard().getTopRow()) - before);
    }

    @Test
    void makeBuildingDecks_remainingBuildingsIndex0HasEra2Cards() {
        initBoardForGame(game, 2);
        state.makeBuildingDecks(loader.getBuildings(), 2);
        List<Set<Card>> remaining = game.getBoard().getRemainingBuildings();
        remaining.get(0).forEach(c -> assertEquals(2, ((Building) c).getEra()));
    }

    @Test
    void makeBuildingDecks_remainingBuildingsIndex1HasEra3Cards() {
        initBoardForGame(game, 2);
        state.makeBuildingDecks(loader.getBuildings(), 2);
        List<Set<Card>> remaining = game.getBoard().getRemainingBuildings();
        remaining.get(1).forEach(c -> assertEquals(3, ((Building) c).getEra()));
    }

    // ──────────────────────────────────────────────
    // fillLowRow
    // ──────────────────────────────────────────────

    @Test
    void fillLowRow_2players_fillsThreeCharacterSlots() {
        initBoardForGame(game, 2);
        state.fillLowRow(2);
        Card[] tribe = game.getBoard().getLowRow().getTribeCards();
        int filled = 0;
        for (Card c : tribe) if (c != null) filled++;
        assertEquals(3, filled);
    }

    @Test
    void fillLowRow_lowRowContainsOnlyDrawableCards() {
        initBoardForGame(game, 2);
        state.fillLowRow(2);
        for (Card c : game.getBoard().getLowRow().getTribeCards()) {
            if (c != null) {
                assertTrue(c.canBeDrawn(null), "Low row should only contain character cards (canBeDrawn=true)");
            }
        }
    }

    // ──────────────────────────────────────────────
    // fillTopRow
    // ──────────────────────────────────────────────

    @Test
    void fillTopRow_2players_doesNotThrow() {
        // fillTopRow computes cardsToDraw = targetSize - topRow.size() (array length).
        // Since Board creates topRow with exactly targetSize slots, cardsToDraw == 0.
        initBoardForGame(game, 2);
        assertDoesNotThrow(() -> state.fillTopRow(2));
    }

    // ──────────────────────────────────────────────
    // nextState
    // ──────────────────────────────────────────────

    @Test
    void nextState_returnsTotemPlacementState() {
        assertInstanceOf(TotemPlacementState.class, state.nextState());
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

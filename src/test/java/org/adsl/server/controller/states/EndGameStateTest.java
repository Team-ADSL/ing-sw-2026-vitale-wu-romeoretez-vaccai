package org.adsl.server.controller.states;

import org.adsl.server.controller.GameController;
import org.adsl.server.exceptions.ServerException;
import org.adsl.server.model.Player;
import org.adsl.server.model.cards.characters.Artist;
import org.adsl.server.model.cards.characters.Builder;
import org.adsl.server.model.cards.characters.Inventor;
import org.adsl.server.persistence.GameDAO;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Icon;
import org.adsl.shared.model.MatchResult;

import java.time.LocalDateTime;
import org.adsl.utils.builder.GameControllerBuilder;
import org.adsl.utils.fakes.FakeGame;
import org.adsl.utils.fakes.FakeGameDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EndGameStateTest {

    static class ThrowingGameDAO extends FakeGameDAO {
        @Override
        public void saveMatch(int gameId, int playerCount, List<String> nicknames, List<Integer> scores)
                throws SQLException {
            throw new SQLException("DB unavailable");
        }
    }

    private FakeGame fakeGame;
    private EndGameState state;

    @BeforeEach
    void setUp() {
        fakeGame = new FakeGame(1, 2);
        GameController controller = new GameControllerBuilder().build();
        state = new EndGameState(fakeGame, controller);
    }

    // ──────────────────────────────────────────────
    // HAPPY PATH
    // ──────────────────────────────────────────────

    @Test
    void testOnEntry_noPlayers_sendsEndGameResultsAndUpdatesGame() throws ServerException {
        state.onEntry();

        assertTrue(fakeGame.endGameResultsSent, "sendEndGameResults must be called");
    }

    @Test
    void testOnEntry_success_leaderboardResultsForwardedToGame() throws ServerException {
        FakeGameDAO daoWithResults = new FakeGameDAO() {
            @Override
            public List<MatchResult> getLeaderboard(int playerCount) {
                return List.of(new MatchResult(1, "Alice", 100));
            }
        };
        GameController controller = new GameControllerBuilder().build();
        state = new EndGameState(fakeGame, controller);

        state.onEntry();

        assertNotNull(fakeGame.capturedResults, "Leaderboard results must be forwarded to the game");
    }

    // ──────────────────────────────────────────────
    // SQL EXCEPTION HANDLING
    // ──────────────────────────────────────────────

    @Test
    void testOnEntry_sqlExceptionOnSaveMatch_throwsServerException() {
        GameController controller = new GameControllerBuilder()
                .withGameDAO(new ThrowingGameDAO())
                .build();
        state = new EndGameState(fakeGame, controller);

        assertThrows(ServerException.class, () -> state.onEntry(),
                "A SQLException during saveMatch must be wrapped in ServerException");
    }

    // ──────────────────────────────────────────────
    // SCORING: Builder
    // ──────────────────────────────────────────────

    @Test
    void testOnEntry_playerWithBuilderCards_accumulatesBuilderPP() throws ServerException {
        Player player = new Player("Alice");
        Builder builder = new Builder("b1", 0, 5, 1, null);
        player.getCards().get(CardType.BUILDER).add(builder);
        fakeGame.getPlayers().add(player);

        state.onEntry();

        assertEquals(5, player.getPp(), "Builder PP must be added to the player's score");
    }

    // ──────────────────────────────────────────────
    // SCORING: Inventor
    // ──────────────────────────────────────────────

    @Test
    void testOnEntry_playerWithInventorsDistinctIcons_scoreIsNumInventorsTimesDistinctIcons() throws ServerException {
        Player player = new Player("Alice");
        Inventor inv1 = new Inventor("i1", Icon.BOAT, 1, null);
        Inventor inv2 = new Inventor("i2", Icon.ROPE, 1, null);
        player.getCards().get(CardType.INVENTOR).add(inv1);
        player.getCards().get(CardType.INVENTOR).add(inv2);
        fakeGame.getPlayers().add(player);

        state.onEntry();

        assertEquals(4, player.getPp(), "2 inventors × 2 distinct icons = 4 PP");
    }

    @Test
    void testOnEntry_playerWithInventorsDuplicateIcon_scoreCountsDistinctOnly() throws ServerException {
        Player player = new Player("Alice");
        Inventor inv1 = new Inventor("i1", Icon.BOAT, 1, null);
        Inventor inv2 = new Inventor("i2", Icon.BOAT, 1, null);
        player.getCards().get(CardType.INVENTOR).add(inv1);
        player.getCards().get(CardType.INVENTOR).add(inv2);
        fakeGame.getPlayers().add(player);

        state.onEntry();

        assertEquals(2, player.getPp(), "2 inventors × 1 distinct icon = 2 PP");
    }

    // ──────────────────────────────────────────────
    // SCORING: Artist
    // ──────────────────────────────────────────────

    @Test
    void testOnEntry_playerWithTwoArtists_scoresTenPoints() throws ServerException {
        Player player = new Player("Alice");
        player.getCards().get(CardType.ARTIST).add(new Artist("a1", 1, null));
        player.getCards().get(CardType.ARTIST).add(new Artist("a2", 1, null));
        fakeGame.getPlayers().add(player);

        state.onEntry();

        assertEquals(10, player.getPp(), "2 artists must score 10 PP (10 * 2 / 2)");
    }

    @Test
    void testOnEntry_playerWithOneArtist_scoresFivePoints() throws ServerException {
        Player player = new Player("Alice");
        player.getCards().get(CardType.ARTIST).add(new Artist("a1", 1, null));
        fakeGame.getPlayers().add(player);

        state.onEntry();

        assertEquals(5, player.getPp(), "1 artist must score 5 PP (10 * 1 / 2)");
    }
}

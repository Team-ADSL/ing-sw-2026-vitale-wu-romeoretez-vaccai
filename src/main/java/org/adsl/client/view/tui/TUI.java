package org.adsl.client.view.tui;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.swing.SwingTerminalFrame;
import com.googlecode.lanterna.terminal.swing.TerminalEmulatorAutoCloseTrigger;
import org.adsl.client.local.LocalGameCoordinator;
import org.adsl.client.view.GameUI;
import org.adsl.client.view.tui.screens.*;
import org.adsl.client.view.tui.OfferTileCatalog;
import org.adsl.shared.enums.Phase;
import org.adsl.shared.enums.Row;
import org.adsl.shared.enums.Totem;
import org.adsl.shared.model.GameDTO;
import org.adsl.shared.model.MatchResult;
import org.adsl.shared.model.OfferTileDTO;
import org.adsl.shared.network.responses.*;
import org.adsl.shared.utils.Move;

import java.io.IOException;
import java.util.*;

/**
 * Text User Interface for MESOS.
 *
 * In local mode (--local flag), this class drives the entire game session:
 *   1. Shows setup dialogs (player count + names) via Lanterna GUI layer
 *   2. Creates a LocalGameCoordinator that wires the game logic without a network
 *   3. Renders the game board using direct Screen drawing with keyboard navigation
 *   4. Handles the "pass the keyboard" mechanic for hotseat multiplayer
 *
 * The class also implements GameUI for future network mode (--client --tui),
 * where an AppCoordinator drives the flow via the observer callbacks.
 */
public class TUI implements GameUI {
    private Screen screen;
    private MultiWindowTextGUI setupGui;        // used only for setup dialogs
    private LocalGameCoordinator coordinator;

    // Screens
    private GameScreen gameScreen;
    private LobbyScreen lobbyScreen;
    private EndGameScreen endGameScreen;

    // Game state shared between callbacks and the game loop
    private volatile GameDTO currentGame;
    private volatile List<MatchResult> endGameResults;
    private volatile boolean gameEnded = false;
    private volatile int totalPlayers;

    // Totem of the player we last showed a "pass" screen for
    private Totem lastPassedTotem = null;

    public TUI() {}

    // ── GameUI lifecycle ──────────────────────────────────────────────────────

    @Override
    public void start() {
        try {
            Terminal terminal;
            if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                // On Windows, DefaultTerminalFactory requires javaw.exe for Swing mode.
                // We bypass this by explicitly creating a SwingTerminalFrame, which works
                // with java.exe too and opens the game in a dedicated window.
                SwingTerminalFrame frame = new SwingTerminalFrame("MESOS – Ancient Tribe Strategy",
                        TerminalEmulatorAutoCloseTrigger.CloseOnExitPrivateMode);
                frame.setVisible(true);
                terminal = frame;
            } else {
                terminal = new DefaultTerminalFactory().createTerminal();
            }
            screen = new TerminalScreen(terminal);
            screen.startScreen();

            // Use GUI layer only for setup dialogs
            setupGui = new MultiWindowTextGUI(screen);

            runLocalMode();

        } catch (Exception e) {
            // Show the full error inside the Swing window before closing
            e.printStackTrace();
            if (screen != null) {
                try {
                    screen.clear();
                    TextGraphics errTg = screen.newTextGraphics();
                    errTg.setForegroundColor(TextColor.ANSI.RED);
                    errTg.putString(2, 1, "FATAL ERROR: " + e.getClass().getSimpleName());
                    errTg.putString(2, 2, e.getMessage() != null ? e.getMessage() : "(no message)");
                    errTg.setForegroundColor(TextColor.ANSI.WHITE);
                    errTg.putString(2, 4, "Full stack trace printed to PowerShell.");
                    errTg.putString(2, 5, "Press ENTER to close.");
                    screen.refresh();
                    screen.readInput();
                } catch (Exception ignored) {}
            }
        } finally {
            shutdown();
        }
    }

    @Override
    public void shutdown() {
        try {
            if (screen != null) {
                screen.stopScreen();
                screen = null;
            }
        } catch (IOException ignored) {}
    }

    // ── Local mode flow ───────────────────────────────────────────────────────

    private void runLocalMode() {
        // Phase 1: Setup dialogs
        SetupScreen setup = new SetupScreen(setupGui);
        int numPlayers = setup.selectPlayerCount();
        List<String> names = setup.enterPlayerNames(numPlayers);
        totalPlayers = numPlayers;

        // Phase 2: Create lobby and coordinator
        lobbyScreen = new LobbyScreen(screen);
        lobbyScreen.render(Collections.emptyList(), totalPlayers);

        // Phase 3: Init game — callbacks (onLobbyUpdate, onGameUpdate) arrive synchronously here
        coordinator = new LocalGameCoordinator(names, this::handleResponse);
        gameScreen  = new GameScreen(screen, coordinator);
        endGameScreen = new EndGameScreen(screen);
        coordinator.initializeGame();

        // Phase 4: Run the interactive game loop
        if (!gameEnded) {
            runGameLoop();
        }

        // Phase 5: Show end game screen
        if (endGameResults != null) {
            endGameScreen.show(endGameResults);
        }
    }

    /**
     * Central game loop. Reads the current game phase and collects player input
     * for interactive phases (TOTEM_PLACEMENT and ACTION_EXECUTION/EXTRA_MOVE).
     * Automatic phases (EVENTS_EXECUTION, END_ROUND) are rendered briefly and
     * the loop continues once the coordinator returns from makeMove().
     */
    private void runGameLoop() {
        while (!gameEnded && currentGame != null) {
            GameDTO game = currentGame;
            Phase phase = game.phase();

            if (phase == Phase.END_GAME) {
                break;
            }

            // Show "pass the keyboard" overlay whenever the active player changes
            Totem currentTotem = game.currentPlayerTotem();
            if (currentTotem != null && !currentTotem.equals(lastPassedTotem)) {
                String name = coordinator.getTotemToName().getOrDefault(currentTotem, "?");
                gameScreen.showPassScreen(name, currentTotem);
                lastPassedTotem = currentTotem;
            }

            // Render current state
            gameScreen.renderStatic(game);

            // Collect input for interactive phases
            switch (phase) {
                case TOTEM_PLACEMENT -> {
                    Set<Move> moves = gameScreen.collectTotemPlacement(game);
                    coordinator.makeMove(moves);
                }
                case ACTION_EXECUTION, EXTRA_MOVE -> {
                    int[] counts = resolveMoveCounts(game);
                    Set<Move> moves = gameScreen.collectCardSelection(game, counts[0], counts[1]);
                    coordinator.makeMove(moves);
                }
                default -> {
                    // EVENTS_EXECUTION / END_ROUND: rendered briefly, then the loop
                    // iterates again after the coordinator's synchronous callbacks finish
                    gameScreen.renderStatic(game);
                    sleep(1500);
                }
            }
            // After makeMove() returns, currentGame has been updated by onGameUpdate()
        }
    }

    /**
     * Determines how many cards the current player must draw from each row,
     * based on the offer tile they placed their totem on.
     *
     * Returns int[]{upperCount, lowerCount}.
     */
    private int[] resolveMoveCounts(GameDTO game) {
        Totem currentTotem = game.currentPlayerTotem();
        for (OfferTileDTO tile : game.board().offerTrack()) {
            if (tile.totem() == currentTotem) {
                int upper = OfferTileCatalog.upperMoves(tile.id());
                int lower = OfferTileCatalog.lowerMoves(tile.id());
                return new int[]{upper, lower};
            }
        }
        return new int[]{1, 0}; // fallback: should not happen in a valid game state
    }

    // ── GameUI callbacks (used by both local and network modes) ───────────────

    /** Handles all server responses routed by LocalGameCoordinator. */
    private void handleResponse(ServerResponse response) {
        if (response instanceof LobbyUpdate update) {
            onLobbyUpdate(update.getPlayers());
        } else if (response instanceof GameUpdate update) {
            onGameUpdate(update.getGame());
        } else if (response instanceof GameEnded update) {
            onEndGame(update.getResults());
        } else if (response instanceof ErrorResponse update) {
            onErrorReceived(update.getMessage());
        }
    }

    @Override
    public void showUsernameField() {
        // In network mode: show a login dialog. Not used in local mode.
        // TODO: implement for network mode
    }

    @Override
    public void onHomeUpdate(List<Integer> activeGames) {
        // Not used in local mode. For network mode: show game list.
    }

    @Override
    public void onLobbyUpdate(List<String> players) {
        if (lobbyScreen != null) {
            lobbyScreen.render(players, totalPlayers);
        }
    }

    @Override
    public void onGameUpdate(GameDTO game) {
        currentGame = game;
        // If we're still in setup phase (lobbyScreen active), close it
        // The game loop in runLocalMode() will pick up the new state
    }

    @Override
    public void onEndGame(List<MatchResult> results) {
        endGameResults = results;
        gameEnded = true;
    }

    @Override
    public void onErrorReceived(String error) {
        // In the game loop context, errors are shown briefly on screen
        try {
            if (screen != null) {
                screen.newTextGraphics().putString(2, screen.getTerminalSize().getRows() - 2,
                        "! ERROR: " + error);
                screen.refresh();
            }
        } catch (IOException ignored) {}
    }

    @Override
    public void onServerDisconnected() {
        try {
            if (screen != null) {
                screen.clear();
                screen.newTextGraphics().putString(2, 2, "Server disconnected. Press any key to exit.");
                screen.refresh();
                screen.readInput();
            }
        } catch (IOException ignored) {}
        shutdown();
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}

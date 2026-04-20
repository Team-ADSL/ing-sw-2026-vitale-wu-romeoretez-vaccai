package org.adsl.client.view.tui;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.swing.SwingTerminalFrame;
import com.googlecode.lanterna.terminal.swing.TerminalEmulatorAutoCloseTrigger;
import org.adsl.client.AppCoordinator;
import org.adsl.client.view.GameUI;
import org.adsl.client.view.tui.screens.*;
import org.adsl.shared.enums.Phase;
import org.adsl.shared.enums.Totem;
import org.adsl.shared.model.GameDTO;
import org.adsl.shared.model.MatchResult;
import org.adsl.shared.model.OfferTileDTO;
import org.adsl.shared.model.PlayerDTO;
import org.adsl.shared.utils.Move;

import java.io.IOException;
import java.util.*;

/**
 * Text User Interface for MESOS (network mode).
 *
 * The TUI is a pure GameUI: it never creates a local controller or touches the
 * server directly. All outgoing actions go through {@link AppCoordinator}
 * (createLoginRequest, createGameRequest, enterGameRequest, startGameRequest,
 * makeMoveRequest, ...) and all incoming state arrives via the GameUI callbacks
 * invoked by {@code AppCoordinator.visit(...)} on the network thread.
 *
 * The main thread in {@link #start()} drives a linear state machine
 * (login → home → lobby → game → end) that consumes the volatile state written
 * by the callbacks. A single monitor ({@code stateLock}) is used to let the main
 * thread wait for the next relevant update without busy-looping.
 */
public class TUI implements GameUI {
    private Screen screen;
    private MultiWindowTextGUI setupGui;
    private AppCoordinator appCoordinator;

    // Screens
    private SetupScreen setupScreen;
    private GameScreen gameScreen;
    private LobbyScreen lobbyScreen;
    private EndGameScreen endGameScreen;

    // State reflected from server callbacks (network thread → main thread)
    private final Object stateLock = new Object();
    private volatile boolean loginPrompted = false;
    private volatile List<Integer> pendingHomeGames = null;
    private volatile List<String> lobbyPlayers = null;
    private volatile GameDTO currentGame = null;
    private volatile List<MatchResult> endResults = null;
    private volatile String lastError = null;
    private volatile boolean disconnected = false;

    // Local identity + per-session info
    private String myUsername;
    private int knownTotalPlayers = -1;
    private Totem lastPassedTotem = null;

    public TUI() {}

    /** Wires the coordinator used to send requests to the server. */
    public void setAppCoordinator(AppCoordinator appCoordinator) {
        this.appCoordinator = appCoordinator;
    }

    // ── GameUI lifecycle ──────────────────────────────────────────────────────

    @Override
    public void start() {
        try {
            initLanterna();

            setupScreen   = new SetupScreen(setupGui);
            lobbyScreen   = new LobbyScreen(screen);
            gameScreen    = new GameScreen(screen);
            endGameScreen = new EndGameScreen(screen);

            // Ask the server for the initial handshake; the server will reply with LoginNeeded.
            try {
                appCoordinator.connectRequest();
            } catch (Exception e) {
                showFatal("Cannot reach server", e);
                return;
            }

            // 1) Wait for LoginNeeded → show username dialog → send login
            awaitUntil(() -> loginPrompted || disconnected, 0);
            if (disconnected) { onServerDisconnected(); return; }
            myUsername = setupScreen.askUsername();
            try { appCoordinator.createLoginRequest(myUsername); }
            catch (Exception e) { showFatal("Login failed", e); return; }

            // 2) Wait for HomeUpdate → show home dialog → create or join a game
            awaitUntil(() -> pendingHomeGames != null || disconnected, 0);
            if (disconnected) { onServerDisconnected(); return; }
            SetupScreen.HomeChoice choice = setupScreen.showHome(pendingHomeGames);
            pendingHomeGames = null;
            try {
                if (choice.kind() == SetupScreen.HomeChoice.Kind.CREATE) {
                    knownTotalPlayers = choice.value();
                    appCoordinator.createGameRequest(choice.value());
                } else {
                    appCoordinator.enterGameRequest(choice.value());
                }
            } catch (Exception e) { showFatal("Could not create/join game", e); return; }

            // 3) Lobby phase: re-render on LobbyUpdate, exit when GameUpdate arrives
            runLobbyPhase();
            if (disconnected) { onServerDisconnected(); return; }

            // 4) Game loop: render state, collect moves when it's our turn
            if (currentGame != null) runGameLoop();

            // 5) End-game screen
            if (endResults != null) endGameScreen.show(endResults);
            else if (disconnected)  onServerDisconnected();

        } catch (Exception e) {
            e.printStackTrace();
            showFatal("Unexpected error", e);
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

    // ── Phases ────────────────────────────────────────────────────────────────

    private void runLobbyPhase() {
        // Initial render (may be empty)
        lobbyScreen.render(safeList(lobbyPlayers), knownTotalPlayers);
        List<String> lastRendered = lobbyPlayers;

        while (currentGame == null && !disconnected && endResults == null) {
            // Re-render on any lobby change
            List<String> players = lobbyPlayers;
            if (players != null && players != lastRendered) {
                lobbyScreen.render(players, knownTotalPlayers);
                lastRendered = players;
            }

            // Poll for 'S' (host starts the game)
            try {
                KeyStroke key = screen.pollInput();
                if (key != null && key.getKeyType() == KeyType.Character) {
                    char c = Character.toLowerCase(key.getCharacter());
                    if (c == 's') {
                        try { appCoordinator.startGameRequest(); }
                        catch (Exception e) { flashBottom("Start failed: " + e.getMessage()); }
                    }
                }
            } catch (IOException ignored) {}

            sleep(100);
        }
    }

    private void runGameLoop() {
        Totem myTotem = findMyTotem(currentGame);

        while (!disconnected && endResults == null) {
            GameDTO game = currentGame;
            if (game == null) { sleep(100); continue; }

            Phase phase = game.phase();
            if (phase == Phase.END_GAME) break;

            // "Pass the keyboard" overlay when the active player changes
            Totem currentTotem = game.currentPlayerTotem();
            if (currentTotem != null && !currentTotem.equals(lastPassedTotem)) {
                String name = nameFor(currentTotem, game);
                gameScreen.showPassScreen(name, currentTotem);
                lastPassedTotem = currentTotem;
            }

            gameScreen.renderStatic(game);

            boolean myTurn = (myTotem != null && myTotem.equals(currentTotem));
            if (myTurn) {
                Set<Move> moves = collectMovesForPhase(game, phase);
                if (moves != null) {
                    try { appCoordinator.makeMoveRequest(moves); }
                    catch (Exception e) { flashBottom("Move failed: " + e.getMessage()); }
                }
                waitForStateChangeFrom(game);
            } else {
                // Other player's turn / automatic phase: wait for the next update.
                waitForStateChangeFrom(game);
            }
        }
    }

    private Set<Move> collectMovesForPhase(GameDTO game, Phase phase) {
        switch (phase) {
            case TOTEM_PLACEMENT -> { return gameScreen.collectTotemPlacement(game); }
            case ACTION_EXECUTION, EXTRA_MOVE -> {
                int[] counts = resolveMoveCounts(game);
                return gameScreen.collectCardSelection(game, counts[0], counts[1]);
            }
            default -> {
                // EVENTS_EXECUTION / END_ROUND are server-driven: just wait
                sleep(800);
                return null;
            }
        }
    }

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

    // ── GameUI callbacks (network thread → main thread) ───────────────────────

    @Override
    public void showUsernameField() {
        synchronized (stateLock) {
            loginPrompted = true;
            stateLock.notifyAll();
        }
    }

    @Override
    public void onHomeUpdate(List<Integer> activeGames) {
        synchronized (stateLock) {
            pendingHomeGames = (activeGames != null) ? activeGames : Collections.emptyList();
            stateLock.notifyAll();
        }
    }

    @Override
    public void onLobbyUpdate(List<String> players) {
        synchronized (stateLock) {
            lobbyPlayers = (players != null) ? players : Collections.emptyList();
            stateLock.notifyAll();
        }
    }

    @Override
    public void onGameUpdate(GameDTO game) {
        synchronized (stateLock) {
            currentGame = game;
            stateLock.notifyAll();
        }
    }

    @Override
    public void onEndGame(List<MatchResult> results) {
        synchronized (stateLock) {
            endResults = results;
            stateLock.notifyAll();
        }
    }

    @Override
    public void onErrorReceived(String error) {
        synchronized (stateLock) {
            lastError = error;
            stateLock.notifyAll();
        }
        flashBottom("! ERROR: " + error);
    }

    @Override
    public void onServerDisconnected() {
        synchronized (stateLock) {
            disconnected = true;
            stateLock.notifyAll();
        }
        try {
            if (screen != null) {
                screen.clear();
                screen.newTextGraphics().putString(2, 2, "Server disconnected. Press any key to exit.");
                screen.refresh();
                screen.readInput();
            }
        } catch (IOException ignored) {}
    }

    // ── Synchronization helpers ───────────────────────────────────────────────

    /** Waits until {@code condition} becomes true, or {@code timeoutMs} elapses (0 = forever). */
    private void awaitUntil(java.util.function.BooleanSupplier condition, long timeoutMs) {
        long deadline = timeoutMs > 0 ? System.currentTimeMillis() + timeoutMs : Long.MAX_VALUE;
        synchronized (stateLock) {
            while (!condition.getAsBoolean()) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) return;
                try { stateLock.wait(Math.min(remaining, 500)); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            }
        }
    }

    /** Blocks until {@code currentGame} is replaced (different reference) or the session ends. */
    private void waitForStateChangeFrom(GameDTO previous) {
        synchronized (stateLock) {
            while (currentGame == previous && !disconnected && endResults == null) {
                try { stateLock.wait(500); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            }
        }
    }

    // ── Identity helpers ──────────────────────────────────────────────────────

    private Totem findMyTotem(GameDTO game) {
        if (game == null || myUsername == null) return null;
        for (PlayerDTO p : game.players()) {
            if (myUsername.equals(p.name())) return p.totem();
        }
        return null;
    }

    private String nameFor(Totem totem, GameDTO game) {
        if (game == null || totem == null) return "?";
        return game.players().stream()
                .filter(p -> p.totem() == totem)
                .map(PlayerDTO::name)
                .findFirst()
                .orElse("?");
    }

    // ── Lanterna bootstrap / error rendering ──────────────────────────────────

    private void initLanterna() throws IOException {
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
        setupGui = new MultiWindowTextGUI(screen);
    }

    private void showFatal(String label, Exception e) {
        try {
            if (screen == null) return;
            screen.clear();
            TextGraphics tg = screen.newTextGraphics();
            tg.setForegroundColor(TextColor.ANSI.RED);
            tg.putString(2, 1, "FATAL: " + label + " (" + e.getClass().getSimpleName() + ")");
            tg.putString(2, 2, e.getMessage() != null ? e.getMessage() : "(no message)");
            tg.setForegroundColor(TextColor.ANSI.WHITE);
            tg.putString(2, 4, "Press ENTER to exit.");
            screen.refresh();
            screen.readInput();
        } catch (IOException ignored) {}
    }

    private void flashBottom(String msg) {
        try {
            if (screen == null) return;
            int row = screen.getTerminalSize().getRows() - 2;
            screen.newTextGraphics().putString(2, row, msg);
            screen.refresh();
        } catch (IOException ignored) {}
    }

    // ── Misc ──────────────────────────────────────────────────────────────────

    private static <T> List<T> safeList(List<T> l) { return l != null ? l : Collections.emptyList(); }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }
}

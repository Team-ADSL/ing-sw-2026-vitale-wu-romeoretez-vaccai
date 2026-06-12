package org.adsl.client.view;

import org.adsl.client.AppCoordinator;
import org.adsl.client.serverEvents.*;
import org.adsl.shared.enums.Totem;
import org.adsl.shared.model.GameDTO;
import org.adsl.shared.model.DBRecord;
import org.adsl.shared.model.MatchResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Abstract base for UI implementations (TUI and GUI). Converts incoming
 * {@code ServerResponse} callbacks (called by {@code AppCoordinator}) into
 * {@code ServerEvent} objects and dispatches them to the active screen via
 * {@link #dispatch(ServerEvent)}.
 * <p>
 * Concrete subclasses ({@code TUI}, {@code GUI}) implement {@link #dispatch},
 * {@link #start()}, and {@link #shutdown()} for their respective rendering
 * technology.
 * </p>
 */
public abstract class GameUI {
    private AppCoordinator appCoordinator;

    /** Dispatches a {@link LoginNeededEvent}, prompting the user for a username. */
    public void showUsernameField() {
        dispatch(new LoginNeededEvent());
    }

    /**
     * Dispatches a home-screen update with no extra player/capacity info or message.
     *
     * @param activeGames IDs of games currently open on the server
     */
    public void onHomeUpdate(List<Integer> activeGames) {
        onHomeUpdate(activeGames, null, null, null);
    }

    /**
     * Dispatches a home-screen update with a status message.
     *
     * @param activeGames IDs of games currently open on the server
     * @param message     status message to display, may be {@code null}
     */
    public void onHomeUpdate(List<Integer> activeGames, String message) {
        onHomeUpdate(activeGames, null, null, message);
    }

    /**
     * Dispatches a full home-screen update.
     *
     * @param activeGames  IDs of games currently open on the server
     * @param gamePlayers  for each game ID, the usernames of joined players; may be {@code null}
     * @param gameCapacity for each game ID, the maximum number of players; may be {@code null}
     * @param message      status message to display, may be {@code null}
     */
    public void onHomeUpdate(List<Integer> activeGames, Map<Integer, List<String>> gamePlayers,
                              Map<Integer, Integer> gameCapacity, String message) {
        dispatch(new HomeUpdateEvent(
                activeGames != null ? activeGames : Collections.emptyList(),
                gamePlayers != null ? gamePlayers : Collections.emptyMap(),
                gameCapacity != null ? gameCapacity : Collections.emptyMap(),
                message));
    }

    /**
     * Dispatches a lobby update with no status message.
     *
     * @param gameId            ID of the lobby's game
     * @param players           usernames currently in the lobby
     * @param numPlayersAllowed maximum number of players for this game
     */
    public void onLobbyUpdate(int gameId, List<String> players, int numPlayersAllowed) {
        onLobbyUpdate(gameId, players, numPlayersAllowed, null);
    }

    /**
     * Dispatches a lobby update.
     *
     * @param gameId            ID of the lobby's game
     * @param players           usernames currently in the lobby
     * @param numPlayersAllowed maximum number of players for this game
     * @param message           status message to display, may be {@code null}
     */
    public void onLobbyUpdate(int gameId, List<String> players, int numPlayersAllowed, String message) {
        dispatch(new LobbyUpdateEvent(gameId,
                players != null ? players : Collections.emptyList(),
                numPlayersAllowed,
                message));
    }

    /**
     * Dispatches the list of totems still available for picking.
     *
     * @param totemList totems not yet chosen by another player
     * @param message   status message to display, may be {@code null}
     */
    public void onTotemAvailableUpdate(List<Totem> totemList, String message) {
        dispatch(new TotemAvailableEvent(totemList, message));
    }

    /**
     * Dispatches a game state update with no status message.
     *
     * @param game the updated game state
     */
    public void onGameUpdate(GameDTO game) {
        dispatch(new GameUpdateEvent(game, null));
    }

    /**
     * Dispatches a game state update.
     *
     * @param game    the updated game state
     * @param message status message to display, may be {@code null}
     */
    public void onGameUpdate(GameDTO game, String message) {
        dispatch(new GameUpdateEvent(game, message));
    }

    /**
     * Dispatches the end-of-match results.
     *
     * @param results final standings for each player
     * @param records updated persistent records (e.g. leaderboard entries)
     * @param message status message to display, may be {@code null}
     */
    public void onEndGame(List<MatchResult> results, List<DBRecord> records, String message) {
        dispatch(new EndGameEvent(results, records, message));
    }

    /**
     * Dispatches notice of a triggered game event (e.g. event card resolution).
     *
     * @param eventTitle short title of the event
     * @param logMessage detailed description appended to the game log
     */
    public void onEventTriggered(String eventTitle, String logMessage) {
        dispatch(new EventsTriggeredEvent(eventTitle, logMessage));
    }

    /**
     * Dispatches a server-authoritative replacement of the game log transcript,
     * used on reconnect.
     *
     * @param history the full transcript, or {@code null} for an empty log
     */
    public void onGameLogRestore(List<String> history) {
        dispatch(new GameLogRestoreEvent(history != null ? history : Collections.emptyList()));
    }

    /**
     * Dispatches an error message received from the server.
     *
     * @param error the error message to display
     */
    public void onErrorReceived(String error) {
        dispatch(new ErrorEvent(error));
    }

    /**
     * Stops the ping scheduler and dispatches a {@link DisconnectedEvent}
     * notifying the active screen that the connection to the server was lost.
     */
    public void onServerDisconnected() {
        if (appCoordinator != null) appCoordinator.stopPingScheduler();
        dispatch(new DisconnectedEvent("Server disconnected. Check your network connection."));
    }

    /**
     * Delivers a server event to the active screen. Implementations decide
     * how/when the event is processed (e.g. via an event queue for the TUI).
     *
     * @param event the event to deliver
     */
    public abstract void dispatch(ServerEvent event);

    /** Initializes and runs the UI until the user exits or the connection is lost. */
    public abstract void start();

    /** Tears down the UI and releases any held resources (terminal, windows, etc.). */
    public abstract void shutdown();

    /** @return the coordinator mediating between this UI and the network layer */
    public AppCoordinator getAppCoordinator() {
        return appCoordinator;
    }

    /**
     * @param appCoordinator the coordinator mediating between this UI and the network layer
     */
    public void setAppCoordinator(AppCoordinator appCoordinator) {
        this.appCoordinator = appCoordinator;
    }
}
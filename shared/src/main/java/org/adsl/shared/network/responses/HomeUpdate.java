package org.adsl.shared.network.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.adsl.shared.exceptions.InvalidResponseException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Response sent to clients on the home screen when the list of open games
 * changes.
 * <p>
 * Contains the current list of active game IDs ({@link #getActiveGames()}),
 * a per-game map of player names ({@link #getGamePlayers()}), and a per-game
 * capacity map ({@link #getGameCapacity()}) so the TUI and GUI can render
 * "N / M players" without a separate request. The maps are populated by
 * {@code ServerController.buildGamePlayersMap()} and
 * {@code ServerController.buildGameCapacityMap()}; they are empty for
 * backwards-compatible single-arg construction.
 * </p>
 */
public class HomeUpdate extends ServerResponse {
    private final List<Integer> activeGames;
    private final Map<Integer, List<String>> gamePlayers;
    private final Map<Integer, Integer> gameCapacity;

    /**
     * Creates a response with the current list of open games and their
     * player/capacity details.
     *
     * @param activeGames  IDs of the games currently open on the server
     * @param gamePlayers  per-game map from game ID to the names of players
     *                     currently in that game; {@code null} is treated as empty
     * @param gameCapacity per-game map from game ID to the maximum number of
     *                      players allowed; {@code null} is treated as empty
     */
    @JsonCreator
    public HomeUpdate(
            @JsonProperty("activeGames") List<Integer> activeGames,
            @JsonProperty("gamePlayers") Map<Integer, List<String>> gamePlayers,
            @JsonProperty("gameCapacity") Map<Integer, Integer> gameCapacity) {
        this.activeGames = activeGames;
        this.gamePlayers = gamePlayers != null ? gamePlayers : Collections.emptyMap();
        this.gameCapacity = gameCapacity != null ? gameCapacity : Collections.emptyMap();
    }

    /**
     * Backwards-compatible convenience constructor that sets only the list of
     * active game IDs, leaving the per-game player and capacity maps empty.
     *
     * @param activeGames IDs of the games currently open on the server
     */
    public HomeUpdate(List<Integer> activeGames) {
        this(activeGames, Collections.emptyMap(), Collections.emptyMap());
    }

    /**
     * Dispatches this response to {@link ResponseVisitor#visit(HomeUpdate)}.
     *
     * @param visitor the visitor that will handle this response
     * @throws InvalidResponseException if the visitor cannot process this response
     */
    @Override
    public void accept(ResponseVisitor visitor) throws InvalidResponseException {
        visitor.visit(this);
    }

    /**
     * Returns the IDs of the games currently open on the server.
     *
     * @return the list of active game IDs
     */
    public List<Integer> getActiveGames() { return activeGames; }

    /**
     * Returns the per-game map of player names, keyed by game ID.
     *
     * @return map from game ID to the names of players currently in that game
     */
    public Map<Integer, List<String>> getGamePlayers() { return gamePlayers; }

    /**
     * Returns the per-game map of player capacities, keyed by game ID.
     *
     * @return map from game ID to the maximum number of players allowed
     */
    public Map<Integer, Integer> getGameCapacity() { return gameCapacity; }

    /**
     * Returns the active game IDs as a single space-separated string.
     *
     * @return the active game IDs joined by spaces
     */
    @Override
    public String toString() {
        return activeGames.stream().map(String::valueOf).collect(Collectors.joining(" "));
    }
}

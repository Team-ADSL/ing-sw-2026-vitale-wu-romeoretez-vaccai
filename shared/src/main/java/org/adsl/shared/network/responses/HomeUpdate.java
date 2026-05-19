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
 * changes. Contains the current list of active game IDs, along with per-game
 * player lists and capacities for rich UI rendering.
 */
public class HomeUpdate extends ServerResponse {
    private final List<Integer> activeGames;
    private final Map<Integer, List<String>> gamePlayers;
    private final Map<Integer, Integer> gameCapacity;

    @JsonCreator
    public HomeUpdate(
            @JsonProperty("activeGames") List<Integer> activeGames,
            @JsonProperty("gamePlayers") Map<Integer, List<String>> gamePlayers,
            @JsonProperty("gameCapacity") Map<Integer, Integer> gameCapacity) {
        this.activeGames = activeGames;
        this.gamePlayers = gamePlayers != null ? gamePlayers : Collections.emptyMap();
        this.gameCapacity = gameCapacity != null ? gameCapacity : Collections.emptyMap();
    }

    public HomeUpdate(List<Integer> activeGames) {
        this(activeGames, Collections.emptyMap(), Collections.emptyMap());
    }

    @Override
    public void accept(ResponseVisitor visitor) throws InvalidResponseException {
        visitor.visit(this);
    }

    public List<Integer> getActiveGames() { return activeGames; }
    public Map<Integer, List<String>> getGamePlayers() { return gamePlayers; }
    public Map<Integer, Integer> getGameCapacity() { return gameCapacity; }

    @Override
    public String toString() {
        return activeGames.stream().map(String::valueOf).collect(Collectors.joining(" "));
    }
}

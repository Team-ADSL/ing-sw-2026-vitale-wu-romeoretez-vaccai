package org.adsl.server.model;

import org.adsl.server.network.HomeObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class HomeTest {

    private Home home;
    private List<List<Integer>> received;
    private HomeObserver recordingObserver;

    @BeforeEach
    void setUp() {
        home = new Home();
        received = new ArrayList<>();
        recordingObserver = new HomeObserver() {
            @Override public void updateHome(List<Integer> activeGames) { received.add(new ArrayList<>(activeGames)); }
        };
    }

    @Test
    void testUpdate_withNoObservers_doesNotThrow() {
        assertDoesNotThrow(() -> home.update());
    }

    @Test
    void testUpdate_notifiesRegisteredObserver() {
        home.addObserver(recordingObserver);
        home.update();
        assertEquals(1, received.size());
    }

    @Test
    void testUpdate_notifiesAllObservers() {
        List<Integer> secondReceived = new ArrayList<>();
        HomeObserver second = new HomeObserver() {
            @Override public void updateHome(List<Integer> activeGames) { secondReceived.addAll(activeGames); }
        };
        home.addObserver(recordingObserver);
        home.addObserver(second);
        home.update();
        assertEquals(1, received.size());
    }

    @Test
    void testUpdate_passesEmptyGameListInitially() {
        home.addObserver(recordingObserver);
        home.update();
        assertTrue(received.get(0).isEmpty());
    }

    @Test
    void testRemoveObserver_observerIsNoLongerNotified() {
        home.addObserver(recordingObserver);
        home.removeObserver(recordingObserver);
        home.update();
        assertTrue(received.isEmpty());
    }

    @Test
    void testAddObserver_multipleTimesNotifiesMultipleTimes() {
        home.addObserver(recordingObserver);
        home.addObserver(recordingObserver);
        home.update();
        assertEquals(2, received.size());
    }

    @Test
    void testRemoveObserver_onlyRemovesOneInstance() {
        home.addObserver(recordingObserver);
        home.addObserver(recordingObserver);
        home.removeObserver(recordingObserver);
        home.update();
        assertEquals(1, received.size());
    }

    // ──────────────────────────────────────────────
    // TEST update(gamePlayers, gameCapacity) overloads
    // ──────────────────────────────────────────────

    @Test
    void testUpdate_withGamePlayersAndCapacity_passesDataToObserver() {
        Map<Integer, List<String>> gamePlayers = Map.of(1, List.of("Alice", "Bob"));
        Map<Integer, Integer> capacity = Map.of(1, 4);
        List<Map<Integer, List<String>>> capturedPlayers = new ArrayList<>();
        List<Map<Integer, Integer>> capturedCapacity = new ArrayList<>();

        HomeObserver richObserver = new HomeObserver() {
            @Override public void updateHome(List<Integer> activeGames) {}
            @Override public void updateHome(List<Integer> activeGames,
                    Map<Integer, List<String>> players, Map<Integer, Integer> cap) {
                capturedPlayers.add(players);
                capturedCapacity.add(cap);
            }
        };

        home.addGame(1);
        home.addObserver(richObserver);
        home.update(gamePlayers, capacity);

        assertEquals(1, capturedPlayers.size());
        assertEquals(gamePlayers, capturedPlayers.get(0));
        assertEquals(capacity, capturedCapacity.get(0));
    }

    @Test
    void testUpdate_withGamePlayersCapacityAndMessage_passesAllDataToObserver() {
        Map<Integer, List<String>> gamePlayers = Map.of(2, List.of("Charlie"));
        Map<Integer, Integer> capacity = Map.of(2, 3);
        List<String> capturedMessages = new ArrayList<>();
        List<Map<Integer, List<String>>> capturedPlayers = new ArrayList<>();

        HomeObserver richObserver = new HomeObserver() {
            @Override public void updateHome(List<Integer> activeGames) {}
            @Override public void updateHome(List<Integer> activeGames,
                    Map<Integer, List<String>> players, Map<Integer, Integer> cap,
                    String message) {
                capturedPlayers.add(players);
                capturedMessages.add(message);
            }
        };

        home.addGame(2);
        home.addObserver(richObserver);
        home.update(gamePlayers, capacity, "game created");

        assertEquals(1, capturedPlayers.size());
        assertEquals(gamePlayers, capturedPlayers.get(0));
        assertEquals("game created", capturedMessages.get(0));
    }

    @Test
    void testUpdate_withGamePlayersAndCapacity_includesCorrectActiveGames() {
        List<List<Integer>> capturedGames = new ArrayList<>();

        HomeObserver richObserver = new HomeObserver() {
            @Override public void updateHome(List<Integer> activeGames) {}
            @Override public void updateHome(List<Integer> activeGames,
                    Map<Integer, List<String>> players, Map<Integer, Integer> cap) {
                capturedGames.add(new ArrayList<>(activeGames));
            }
        };

        home.addGame(5);
        home.addGame(7);
        home.addObserver(richObserver);
        home.update(Map.of(), Map.of());

        assertEquals(1, capturedGames.size());
        assertTrue(capturedGames.get(0).contains(5));
        assertTrue(capturedGames.get(0).contains(7));
    }
}

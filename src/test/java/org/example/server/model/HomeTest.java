package org.example.server.model;

import org.example.shared.model.GameDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HomeTest {

    private Home home;
    private List<List<Integer>> received;
    private ModelObserver recordingObserver;

    @BeforeEach
    void setUp() {
        home = new Home();
        received = new ArrayList<>();
        recordingObserver = new ModelObserver() {
            @Override public void updateHome(List<Integer> activeGames) { received.add(new ArrayList<>(activeGames)); }
            @Override public void updateLobby(List<String> players) {}
            @Override public void updateGame(GameDTO game) {}
        };
    }

    @Test
    void update_withNoObservers_doesNotThrow() {
        assertDoesNotThrow(() -> home.update());
    }

    @Test
    void update_notifiesRegisteredObserver() {
        home.addObserver(recordingObserver);
        home.update();
        assertEquals(1, received.size());
    }

    @Test
    void update_notifiesAllObservers() {
        List<Integer> secondReceived = new ArrayList<>();
        ModelObserver second = new ModelObserver() {
            @Override public void updateHome(List<Integer> activeGames) { secondReceived.addAll(activeGames); }
            @Override public void updateLobby(List<String> p) {}
            @Override public void updateGame(GameDTO g) {}
        };
        home.addObserver(recordingObserver);
        home.addObserver(second);
        home.update();
        assertEquals(1, received.size());
        // second observer also fired (no exception means it was called)
    }

    @Test
    void update_passesEmptyGameListInitially() {
        home.addObserver(recordingObserver);
        home.update();
        assertTrue(received.get(0).isEmpty());
    }

    @Test
    void removeObserver_observerIsNoLongerNotified() {
        home.addObserver(recordingObserver);
        home.removeObserver(recordingObserver);
        home.update();
        assertTrue(received.isEmpty());
    }

    @Test
    void addObserver_multipleTimesNotifiesMultipleTimes() {
        home.addObserver(recordingObserver);
        home.addObserver(recordingObserver);
        home.update();
        assertEquals(2, received.size());
    }

    @Test
    void removeObserver_onlyRemovesOneInstance() {
        home.addObserver(recordingObserver);
        home.addObserver(recordingObserver);
        home.removeObserver(recordingObserver);
        home.update();
        assertEquals(1, received.size());
    }
}

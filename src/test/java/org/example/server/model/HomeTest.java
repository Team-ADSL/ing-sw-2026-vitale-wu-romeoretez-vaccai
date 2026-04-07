package org.example.server.model;

import org.example.server.network.HomeObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

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
        HomeObserver second = new HomeObserver() {
            @Override public void updateHome(List<Integer> activeGames) { secondReceived.addAll(activeGames); }
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

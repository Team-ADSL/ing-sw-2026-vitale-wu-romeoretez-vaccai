package org.adsl.client.view.tui;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared, in-memory history of server-pushed game messages for the TUI.
 * Persists across screen transitions so the player keeps the full transcript
 * when the game ends or the lobby reopens.
 */
public final class TuiGameLog {

    public static final TuiGameLog INSTANCE = new TuiGameLog();

    private final List<String> history = new ArrayList<>();

    private TuiGameLog() {}

    public synchronized void append(String text) {
        if (text == null || text.isBlank()) return;
        history.add(text);
    }

    public synchronized List<String> recent(int n) {
        int from = Math.max(0, history.size() - n);
        return new ArrayList<>(history.subList(from, history.size()));
    }

    public synchronized List<String> all() {
        return new ArrayList<>(history);
    }

    public synchronized int size() {
        return history.size();
    }

    public synchronized void clear() {
        history.clear();
    }
}
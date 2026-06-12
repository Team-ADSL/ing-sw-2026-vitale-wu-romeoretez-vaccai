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

    /**
     * Appends a message to the transcript. No-op if {@code text} is
     * {@code null} or blank.
     *
     * @param text the message to append
     */
    public synchronized void append(String text) {
        if (text == null || text.isBlank()) return;
        history.add(text);
    }

    /**
     * @param n maximum number of most recent entries to return
     * @return a copy of the last {@code n} entries (or fewer if the log is shorter)
     */
    public synchronized List<String> recent(int n) {
        int from = Math.max(0, history.size() - n);
        return new ArrayList<>(history.subList(from, history.size()));
    }

    /** @return a copy of the entire transcript, oldest first */
    public synchronized List<String> all() {
        return new ArrayList<>(history);
    }

    /** @return the number of entries in the transcript */
    public synchronized int size() {
        return history.size();
    }

    /** Removes all entries from the transcript. */
    public synchronized void clear() {
        history.clear();
    }

    /**
     * Replaces the whole transcript with {@code entries} (server-authoritative,
     * used on reconnect). Replacing rather than appending keeps a same-process
     * reconnect from duplicating lines. Null or blank entries are skipped.
     *
     * @param entries the authoritative transcript, or {@code null} to just clear the log
     */
    public synchronized void restore(List<String> entries) {
        history.clear();
        if (entries == null) return;
        for (String e : entries) {
            if (e != null && !e.isBlank()) history.add(e);
        }
    }
}
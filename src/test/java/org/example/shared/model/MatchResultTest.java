package org.example.shared.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class MatchResultTest {

    private final LocalDateTime now = LocalDateTime.of(2026, 3, 26, 12, 0);
    private final MatchResult result = new MatchResult(1, "Alice", 42, now);

    @Test
    void getRank_returnsCorrectValue() {
        assertEquals(1, result.rank());
    }

    @Test
    void getNickname_returnsCorrectValue() {
        assertEquals("Alice", result.nickname());
    }

    @Test
    void getScore_returnsCorrectValue() {
        assertEquals(42, result.score());
    }

    @Test
    void getPlayedAt_returnsCorrectValue() {
        assertEquals(now, result.playedAt());
    }

    @Test
    void toString_containsRankNicknameAndScore() {
        String s = result.toString();
        assertTrue(s.contains("1"));
        assertTrue(s.contains("Alice"));
        assertTrue(s.contains("42"));
    }
}

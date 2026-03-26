package org.example.shared.model;

import java.time.LocalDateTime;

public record MatchResult(
        int rank,
        String nickname,
        int score,
        LocalDateTime playedAt)
{}

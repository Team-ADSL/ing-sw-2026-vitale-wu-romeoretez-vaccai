package org.adsl.shared.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public record MatchResult(
        int rank,
        String nickname,
        int score)
implements Serializable {}

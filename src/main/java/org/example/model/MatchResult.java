package org.example.model;

import java.time.LocalDateTime;

public class MatchResult {

    private final int           rank;
    private final String        nickname;
    private final int           score;
    private final LocalDateTime playedAt;

    public MatchResult(int rank, String nickname, int score, LocalDateTime playedAt) {
        this.rank     = rank;
        this.nickname = nickname;
        this.score    = score;
        this.playedAt = playedAt;
    }

    public int           getRank()     { return rank; }
    public String        getNickname() { return nickname; }
    public int           getScore()    { return score; }
    public LocalDateTime getPlayedAt() { return playedAt; }

    @Override
    public String toString() {
        return rank + ". " + nickname + " — " + score + " pts";
    }
}

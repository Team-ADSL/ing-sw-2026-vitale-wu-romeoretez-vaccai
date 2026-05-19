package org.adsl.shared.model;

import java.io.Serializable;

public record MatchResult(String nickname, int pp, int food)
        implements Serializable {}

package org.adsl.shared.model;

import java.io.Serializable;

public record CardDTO(String id, String typeLabel, String effectsLabel, String costLabel) implements Serializable {}
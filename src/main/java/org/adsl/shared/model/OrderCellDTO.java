package org.adsl.shared.model;

import org.adsl.shared.enums.Totem;

import java.io.Serializable;

public record OrderCellDTO(Totem totem, int bonus, boolean isMalus) implements Serializable {}

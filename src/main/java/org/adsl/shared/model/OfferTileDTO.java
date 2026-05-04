package org.adsl.shared.model;

import org.adsl.shared.enums.Totem;

import java.io.Serializable;

import java.util.Map;
import org.adsl.shared.enums.Row;

public record OfferTileDTO(String id, Totem totem, Map<Row, Integer> moves)
implements Serializable {}

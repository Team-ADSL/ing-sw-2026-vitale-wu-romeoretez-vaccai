package org.adsl.shared.model;

import org.adsl.shared.enums.Totem;

import java.io.Serializable;

public record OfferTileDTO(String id, Totem totem)
implements Serializable {}

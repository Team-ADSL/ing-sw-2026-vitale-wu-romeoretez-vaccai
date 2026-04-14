package org.adsl.shared.model;

import org.adsl.shared.enums.Totem;

import java.io.Serializable;
import java.util.ArrayList;

public record OrderTileDTO(String id, ArrayList<Totem> totems)
implements Serializable {}

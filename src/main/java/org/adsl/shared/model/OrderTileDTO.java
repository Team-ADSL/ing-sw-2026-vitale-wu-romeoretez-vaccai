package org.adsl.shared.model;

import java.io.Serializable;
import java.util.ArrayList;

public record OrderTileDTO(String id, ArrayList<OrderCellDTO> cells)
implements Serializable {}

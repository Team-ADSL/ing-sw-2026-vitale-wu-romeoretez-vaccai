package org.adsl.shared.model;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Immutable snapshot of the turn-order tile sent inside {@code BoardDTO}.
 *
 * @param id    unique tile identifier
 * @param cells ordered list of cells; index 0 is the first-player position
 */
public record OrderTileDTO(String id, ArrayList<OrderCellDTO> cells)
implements Serializable {}

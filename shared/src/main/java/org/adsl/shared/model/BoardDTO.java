package org.adsl.shared.model;

import java.io.Serializable;
import java.util.ArrayList;

public record BoardDTO(
        ArrayList<CardDTO> lowRow,
        ArrayList<CardDTO> topRow,
        ArrayList<OfferTileDTO> offerTrack,
        OrderTileDTO orderTile,
        ArrayList<Boolean> remainingBuildings, // indicate the presence of the related deck
        boolean isDeckEmpty
) implements Serializable {}

package org.example.shared.model;


import org.example.shared.enums.Totem;

import java.io.Serializable;
import java.util.ArrayList;

public record BoardDTO(
        ArrayList<CardDTO> lowRow,
        ArrayList<CardDTO> topRow,
        ArrayList<Totem> offerTrack,
        ArrayList<Totem> orderQueue,
        ArrayList<Boolean> remainingBuildings, // indicate the presence of the related deck
        boolean isDeckEmpty
) implements Serializable {}

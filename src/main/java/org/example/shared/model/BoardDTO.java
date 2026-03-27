package org.example.shared.model;


import java.io.Serializable;
import java.util.ArrayList;

public record BoardDTO(
        ArrayList<CardDTO> lowRow,
        ArrayList<CardDTO> topRow,
        ArrayList<PlayerDTO> offerTrack,
        ArrayList<PlayerDTO> orderQueue,
        ArrayList<Boolean> remainingBuildings,
        boolean isDeckEmpty
) implements Serializable {}

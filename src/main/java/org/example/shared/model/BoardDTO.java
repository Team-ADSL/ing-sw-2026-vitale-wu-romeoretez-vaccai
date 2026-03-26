package org.example.shared.model;


import java.util.ArrayList;

public record BoardDTO(
        int id,
        ArrayList<CardDTO> lowRow,
        ArrayList<CardDTO> topRow,
        ArrayList<PlayerDTO> offerTrack,
        ArrayList<PlayerDTO> orderQueue,
        ArrayList<Boolean> remainingBuildings,
        boolean isDeckEmpty
) implements Renderable{}

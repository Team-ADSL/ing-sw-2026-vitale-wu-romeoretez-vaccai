package org.example.model.game;

import java.util.ArrayList;
import java.util.Optional;

public class OrderTile {
    private ArrayList<OrderCell> orderQueue;

    public OrderTile() {
        this.orderQueue = new ArrayList<>();
    }

    public Optional<Player> getPlayerAt(int i){
        return orderQueue.get(i).getPlayer();
    }

    public void randomPlacement(){
        //
    }
}

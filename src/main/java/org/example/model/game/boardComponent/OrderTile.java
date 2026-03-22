package org.example.model.game.boardComponent;

import org.example.model.game.Player;

import java.util.ArrayList;
import java.util.Optional;

public class OrderTile {
    private final ArrayList<OrderCell> orderQueue;

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

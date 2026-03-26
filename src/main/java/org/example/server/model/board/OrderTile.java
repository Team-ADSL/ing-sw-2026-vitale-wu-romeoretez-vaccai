package org.example.server.model.board;

import org.example.server.model.Player;

import java.util.ArrayList;
import java.util.Optional;

public class OrderTile {
    private final ArrayList<OrderCell> orderQueue;

    public OrderTile(ArrayList<OrderCell> orderQueue) {
        this.orderQueue = orderQueue;
    }

    public Optional<Player> getPlayerAt(int i){
        return orderQueue.get(i).getPlayer();
    }

    public void randomPlacement(){
        //
    }

    public int placePlayerAtNext(Player p){
        int i = 0;
        while(i < orderQueue.size() && orderQueue.get(i).getPlayer().isPresent()){
            i++;
        }
        orderQueue.get(i).setPlayer(Optional.of(p));
        return i;
    }

    public OrderCell getCellAt(int i){
        return orderQueue.get(i);
    }

    public int size(){
        return orderQueue.size();
    }
}

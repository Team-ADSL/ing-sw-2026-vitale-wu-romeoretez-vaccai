package org.example.server.model.board;

import org.example.server.model.Player;
import org.example.shared.enums.Totem;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class OrderTile implements Serializable {
    private final ArrayList<OrderCell> orderQueue;

    public OrderTile(ArrayList<OrderCell> orderQueue) {
        this.orderQueue = orderQueue;
    }

    public Optional<Player> getPlayerAt(int i){
        return orderQueue.get(i).getPlayer();
    }

    public void placePlayersRandom(Set<Player> players){
        List<Player> shuffledPlayers = new ArrayList<>(players);
        Collections.shuffle(shuffledPlayers);
        IntStream.range(0, shuffledPlayers.size())
                .forEach(i -> orderQueue.get(i).setPlayer(shuffledPlayers.get(i)));
    }

    public int placePlayerAtNext(Player p){
        int i = 0;
        while(i < orderQueue.size() && orderQueue.get(i).getPlayer().isPresent()){
            i++;
        }
        orderQueue.get(i).setPlayer(p);
        return i;
    }

    public void removePlayer(Player player){
        for (OrderCell orderCell : orderQueue) {
            Optional<Player> cellPlayer = orderCell.getPlayer();
            if (cellPlayer.isPresent()) {
                if (cellPlayer.get().equals(player)) {
                    orderCell.setPlayer(null);
                }
            }
        }
    }

    public ArrayList<Totem> createDTO(){
        return (ArrayList<Totem>) orderQueue.stream()
                .map(tile -> tile.getPlayer().orElse(null))
                .map(p -> p != null ? p.getColor() : null)
                .collect(Collectors.toList());
    }

    public OrderCell getCellAt(int i){
        return orderQueue.get(i);
    }

    public int size(){
        return orderQueue.size();
    }
}

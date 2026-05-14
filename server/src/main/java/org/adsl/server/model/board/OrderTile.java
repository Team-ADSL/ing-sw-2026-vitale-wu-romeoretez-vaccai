package org.adsl.server.model.board;

import org.adsl.server.model.Player;
import org.adsl.shared.model.OrderCellDTO;
import org.adsl.shared.model.OrderTileDTO;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class OrderTile implements Serializable {
    private final String id;
    private final ArrayList<OrderCell> orderQueue;

    public OrderTile(String id, ArrayList<OrderCell> orderQueue) {
        this.id = id;
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

    public OrderTileDTO createDTO(){
        ArrayList<OrderCellDTO> cells = orderQueue.stream()
                .map(cell -> new OrderCellDTO(
                        cell.getPlayer().map(Player::getColor).orElse(null),
                        cell.getBonus(),
                        cell.isMalus()))
                .collect(Collectors.toCollection(ArrayList::new));
        return new OrderTileDTO(id, cells);
    }

    public OrderCell getCellAt(int i){
        return orderQueue.get(i);
    }

    public int size(){
        return orderQueue.size();
    }
}

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

public class OrderCell extends Game {
    private Optional<Player> player;
    private int bonus;
    private boolean isMalus;

public OrderCell(Optional<Player> player, int bonus, boolean isMalus, int round, int turn, int era, Phase phase,
                 ArrayList<Card> lowRow, ArrayList<Card> upRow, ArrayList<OfferTile> offerQueue,
                 ArrayList<OrderCell> orderQueue, Set<Building> buildings, Set<Player> players) {

    super(round, turn, era, phase, lowRow, upRow,
            offerQueue, orderQueue, buildings, players);

    this.player = player;
    this.bonus = bonus;
    this.isMalus = isMalus;
}

}

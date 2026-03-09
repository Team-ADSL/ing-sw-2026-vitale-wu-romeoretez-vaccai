import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

public class OfferTile extends Game {
    private Optional<Player> player;
    private Action action;

public OfferTile(Optional<Player> player, Action action, int round, int turn, int era, Phase phase,
                 ArrayList<Card> lowRow, ArrayList<Card> upRow, ArrayList<OfferTile> offerQueue,
                 ArrayList<OrderCell> orderQueue, Set<Building> buildings, Set<Player> players) {

    super(round, turn, era, phase, lowRow, upRow,
            offerQueue, orderQueue, buildings, players);

    this.player = player;
    this.action = action;
}

}

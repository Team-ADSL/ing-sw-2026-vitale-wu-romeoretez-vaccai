import java.util.Optional;

public class Hunt extends Event {

    private int multiplierPP;

public Hunt (int multiplierPP, boolean isFinal, int id, int era, Optional<Integer> numPlayers, boolean isEvent) {

    super(isFinal, id, era, numPlayers, isEvent);

    this.multiplierPP = multiplierPP;

}

}

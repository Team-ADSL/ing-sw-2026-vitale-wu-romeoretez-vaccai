import java.util.Optional;

public class ShamanicRitual extends Event {

    private int lostPP;
    private int gainedPP;

public ShamanicRitual (int lostPP, int gainedPP, boolean isFinal, int id,
                       int era, Optional<Integer> numPlayers, boolean isEvent) {

    super(isFinal, id, era, numPlayers, isEvent);

    this.lostPP = lostPP;
    this.gainedPP = gainedPP;

}

}

import java.util.Optional;

public class Card {
    private final int id; //final???
    private int era;
    private Optional<Integer> numPlayers;
    private boolean isEvent;

public Card (int id, int era, Optional<Integer> numPlayers, boolean isEvent) {

    this.id = id;
    this.era = era;
    this.numPlayers = numPlayers;
    this.isEvent = isEvent;

}

}

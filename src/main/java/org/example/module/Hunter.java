import java.util.Optional;

public class Hunter extends Card implements Charachter {

    private boolean extraFood;

public Hunter (boolean extraFood, int id, int era, Optional<Integer> numPlayers, boolean isEvent) {

    super(id, era, numPlayers, isEvent);

    this.extraFood = extraFood;

}

}

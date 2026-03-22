package org.example.model.card.event;

import org.example.model.card.CardType;
import org.example.model.card.Trigger;
import org.example.model.card.building.utils.BuildingBonus;
import org.example.model.game.Player;

import java.util.Optional;
import java.util.Set;

public class Sustenance extends Event {

    private final int lostPP;

    public Sustenance(int lostPP, boolean isFinal, int era, Optional<Integer> numPlayers) {
        super(isFinal, era, numPlayers);
        this.lostPP = lostPP;
    }

    public int getLostPP() {
        return lostPP;
    }

    @Override
    public void activeEffect(Set<Player> players, Trigger t) {
        if (t == Trigger.EVENT_EXECUTION) {
            for (Player p : players) {
                activateBuildings(p, Trigger.SUSTENANCE);
                BuildingBonus bonus = p.getBuildingBonus();
                int totalCharacters = p.getCards().entrySet().stream()
                        .filter(e -> e.getKey() != CardType.BUILDINGS)
                        .mapToInt(e -> e.getValue().size())
                        .sum();
                int discount = p.getCards().get(CardType.GATHERER).size() * 3 + bonus.getSustenanceDiscount();
                int foodToPay = Math.max(0, totalCharacters - discount);
                int foodAvailable = p.getFood();
                int actualFoodPaid = Math.min(foodAvailable, foodToPay);
                p.changeFood(-actualFoodPaid);
                int unfedCharacters = foodToPay - actualFoodPaid;
                p.changePP(-(unfedCharacters * lostPP));
                bonus.reset();
            }
        }
    }
}

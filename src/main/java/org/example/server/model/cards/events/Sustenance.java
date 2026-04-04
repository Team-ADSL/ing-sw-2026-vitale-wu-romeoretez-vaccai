package org.example.server.model.cards.events;

import org.example.shared.enums.CardType;
import org.example.shared.enums.Trigger;
import org.example.server.model.cards.buildings.utils.BuildingBonus;
import org.example.server.model.Player;

import java.util.Optional;
import java.util.Set;

public class Sustenance extends Event {

    private final int lostPP;

    public Sustenance(String id, int lostPP, boolean isFinal, int era, Optional<Integer> numPlayers) {
        super(id, isFinal, era, numPlayers);
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
                        .mapToInt(e -> e.getValue().size()).sum();
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

package org.adsl.server.model.cards.buildings;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.cards.buildings.utils.BuildingEffect;
import org.adsl.server.model.Player;
import org.adsl.shared.model.CardToken;

import java.util.*;

public class SinceBuilt extends Building {
    private final BuildingEffect buildingEffect;
    private Map<CardType, Set<Card>> characterInUse;

    public SinceBuilt(String id, int endGamePP, int cost, Trigger trigger,
                      int era, Integer numPlayers, BuildingEffect buildingEffect) {
        super(id, endGamePP, cost, era, numPlayers);
        this.characterInUse = new HashMap<>();
        this.characterInUse.put(CardType.HUNTER, new HashSet<>());
        this.characterInUse.put(CardType.GATHERER, new HashSet<>());
        this.characterInUse.put(CardType.SHAMAN, new HashSet<>());
        this.characterInUse.put(CardType.BUILDER, new HashSet<>());
        this.characterInUse.put(CardType.INVENTOR, new HashSet<>());
        this.characterInUse.put(CardType.ARTIST, new HashSet<>());
        this.buildingEffect = buildingEffect;
    }

    public SinceBuilt(String id, int endGamePP, int cost, Trigger trigger, Map<CardType, Set<Card>> characterInUse,
                      int era, Integer numPlayers, BuildingEffect buildingEffect) {
        super(id, endGamePP, cost, era, numPlayers);
        this.characterInUse = characterInUse;
        this.buildingEffect = buildingEffect;
    }

    @Override
    public void activeEffect(Set<Player> players, Trigger t) {
        if(t == Trigger.DRAWING){
            Optional<Player> playerContainer = players.stream().findFirst();
            if (playerContainer.isEmpty()) {
                return; // ERRORE DA GESTIRE?
            }

            Player p = playerContainer.get();
            Card lastPick = p.getLastPick();
            if(lastPick != null) {
                lastPick.insert(characterInUse);

                if (buildingEffect == BuildingEffect.FOOD_COMPLETE_SET) {
                    int numDifferentType = (int)characterInUse.entrySet().stream()
                            .filter(e -> !e.getValue().isEmpty())
                            .count();
                    if(numDifferentType == 6){
                        p.changeFood(5);
                        characterInUse.values().forEach(set -> {
                            Iterator<Card> it = set.iterator();
                            if (it.hasNext()) {
                                it.next();
                                it.remove();
                            }
                        });
                    }

                } else if (buildingEffect == BuildingEffect.COUPLE_INVENTOR) {
                    int numInventor = characterInUse.get(CardType.INVENTOR).size();
                    if(numInventor == 2){
                        p.changeFood(3);
                        characterInUse = new HashMap<>();
                    }
                }
            }
        }
    }

    @Override
    protected String getEffectsLabel() {
        return switch (buildingEffect) {
            case FOOD_COMPLETE_SET -> "+5" + CardToken.FOOD + " X" + CardToken.SET;
            case COUPLE_INVENTOR   -> "2" + CardToken.INVENTOR + "=>+" + CardToken.FOOD;
            default                -> CardToken.PP + getEndGamePP();
        };
    }
}

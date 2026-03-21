package org.example.model.card.building;

import org.example.model.card.Trigger;
import org.example.model.card.building.utils.BuildingEffect;
import org.example.model.card.character.Character;
import org.example.model.game.Player;

import java.util.Optional;
import java.util.Set;

public class SinceBuilt extends Building {
    private BuildingEffect buildingEffect;
    private Set<Character> characterInUse;
    //private BuildingTypeCounter (distinguere tra coppie di artist e full set) // bro che cosa significa? intendi buildingEffect?

    public SinceBuilt(int endGamePP, int cost, Trigger trigger, Set<Character> characterInUse, int era, Optional<Integer> numPlayers) {
        super(endGamePP, cost, era, numPlayers);
        this.characterInUse = characterInUse;
        this.buildingEffect = buildingEffect;
    }


    @Override
    public void activeEffect(Set<Player> players, Trigger t) {
        Optional<Player> playerContainer = players.stream().findFirst();
        if (playerContainer.isEmpty()) {
            return; // ERRORE DA GESTIRE?
        }
        Player p = playerContainer.get();
        // if per distringuere i due casi, poi conta i character nel set per capire se dare i punti
        if (buildingEffect == BuildingEffect.FOOD_COMPLETE_SET) {
            /*qui non saprei come muovermi, avevo pensato di contare i set già presenti alla chiamata di questo metodo
            e segnarli da qualche parte per poi contare ogni volta i set nuovi e sottrarre i set precedenti, ma quando
            viene chiamato questo metodo? ogni quante volte viene fatto il check? sicuramente mi sfugge qualcosa, probabilmente
            non ce ne occupiamo in buildings e io devo solo aggiungere 5 a ogni chiamata, tipo così?
             */
            p.changeFood(5);
        }
        if (buildingEffect == BuildingEffect.COUPLE_INVENTOR) {
            p.changeFood(3);
        }
    }
}

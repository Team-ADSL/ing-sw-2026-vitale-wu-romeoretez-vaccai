package org.example.model.card.building;

import org.example.model.card.Card;
import org.example.model.card.CardType;
import org.example.model.card.Trigger;
import org.example.model.card.character.Character;
import org.example.model.game.Player;

import java.util.Optional;
import java.util.Set;

public class EndGame extends Building {
    Building_Effect buildingEffect;
    public EndGame (int endGamePP, int cost, Trigger trigger,
                    Set<Character> characterUse, int era, Optional<Integer> numPlayers) {
        super(endGamePP, cost, trigger, characterUse, era, numPlayers);
        this.buildingEffect=buildingEffect;

    }

    @Override
    public void activeEffect(Set<Player> players, Trigger t) {
        super.activeEffect(players, t);
        if (buildingEffect==Building_Effect.END_BUILDER_MULTIPLIER){
            for (Player p : players) {
                int countBuilderPP;
                Set<Card> builderCards = p.getCards().get(CardType.BUILDER);
                for (Card c : builderCards) {
                    //non so come accedere alla variabile pp di Builder
                }
            }
        }
    }
}

package org.example.model.card.building;

import org.example.model.card.CardType;
import org.example.model.card.character.Character;
import org.example.model.card.Trigger;
import org.example.model.card.Card;
import org.example.model.game.Player;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Building extends Card {

    private int endGamePP;
    private int cost;
    private Trigger trigger;
    private Set<Character> characterUse;

    public Building (int endGamePP, int cost, Trigger trigger,
                     Set<Character> characterUse, int era, Optional<Integer> numPlayers) {

        super(era, numPlayers);

        this.endGamePP = endGamePP;
        this.cost = cost;
        this.trigger = trigger;
        this.characterUse = characterUse;

    }

    @Override
    public boolean canBeDrawn(Player p) {
        return false;
    }

    @Override
    public void insert(Map<CardType, Set<Card>> cards) {

    }

    @Override
    public void activeEffect(Set<Player> players, Trigger t) {

    }
}

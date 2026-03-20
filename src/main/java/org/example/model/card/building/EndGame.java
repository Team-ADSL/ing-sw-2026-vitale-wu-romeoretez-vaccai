package org.example.model.card.building;

import org.example.model.card.Card;
import org.example.model.card.CardType;
import org.example.model.card.Trigger;
import org.example.model.card.building.utils.BuildingEffect;
import org.example.model.card.character.Character;
import org.example.model.game.Player;

import java.util.Optional;
import java.util.Set;

public class EndGame extends Building {
    private BuildingEffect buildingEffect;

    public EndGame (int endGamePP, int cost, int era, Optional<Integer> numPlayers, BuildingEffect buildingEffect) {
        super(endGamePP, cost, era, numPlayers);
        this.buildingEffect = buildingEffect;
    }

    @Override
    public void activeEffect(Set<Player> players, Trigger t) {
        // Per builder multiplier basta settare un attributo a true nel building bonus (controllato da controller)
        // Per full set basta contare i set completi (set di carte a dimensione minore) e dare al player i PP
        // Per il moltiplicatore di punti in base al numero di personaggi di un certo set basta calcolare
        // il size del corrispondente set di carte e dare i punti al Player. Nota che serve un attributo
        // nella classe per dire quale tipo sfruttare nel calcolo (DEFAULT value per gli altri building)
        // Per i punti bonus basta cambiare i punti del player
        if (buildingEffect== BuildingEffect.END_BUILDER_MULTIPLIER){
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

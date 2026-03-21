package org.example.model.card.building;

import org.example.model.card.CardType;
import org.example.model.card.Trigger;
import org.example.model.card.building.utils.BuildingEffect;
import org.example.model.game.Player;

import java.util.Optional;
import java.util.Set;

public class EndGame extends Building {
    private BuildingEffect buildingEffect;
    private CardType characterTypeForMultiplier; //serve per END_CHARACTER_MULTIPLIER, non riesco a trovare un nome migliore


    public EndGame(int endGamePP, int cost, int era, Optional<Integer> numPlayers, BuildingEffect buildingEffect, CardType characterTypeForMultiplier) {
        super(endGamePP, cost, era, numPlayers);
        this.buildingEffect = buildingEffect;
        this.characterTypeForMultiplier = characterTypeForMultiplier;
    }

    @Override
    public void activeEffect(Set<Player> players, Trigger t) {
        // Per builder multiplier basta settare un attributo a true nel building bonus (controllato da controller)
        // Per full set basta contare i set completi (set di carte a dimensione minore) e dare al player i PP
        // Per il moltiplicatore di punti in base al numero di personaggi di un certo set basta calcolare
        // il size del corrispondente set di carte e dare i punti al Player. Nota che serve un attributo
        // nella classe per dire quale tipo sfruttare nel calcolo (DEFAULT value per gli altri building)
        // Per i punti bonus basta cambiare i punti del player
        Optional<Player> playerContainer = players.stream().findFirst();
        if (playerContainer.isEmpty()) {
            return; // ERRORE DA GESTIRE?
        }
        Player p = playerContainer.get();
        switch (buildingEffect) {
            case BuildingEffect.END_BUILDER_MULTIPLIER -> p.getBuildingBonus().setBuilderMultiplierPP(2);
            case BuildingEffect.PP_COMPLETE_SET -> {
                int min = p.getCards().values().stream()
                        .mapToInt(Set::size)
                        .min()
                        .orElse(0);
                p.changePP(min * 6);
            }
            case BuildingEffect.END_CHARACTER_MULTIPLIER -> {
                p.changePP(p.getCards().get(characterTypeForMultiplier).size());
            }
            case BuildingEffect.END_PP_BONUS -> {
                p.changePP(25);
            }

        }
    }
}

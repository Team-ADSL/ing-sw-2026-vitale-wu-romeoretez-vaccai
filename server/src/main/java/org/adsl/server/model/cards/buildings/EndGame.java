package org.adsl.server.model.cards.buildings;

import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.cards.buildings.utils.BuildingEffect;
import org.adsl.server.model.Player;
import org.adsl.shared.model.CardToken;

import java.util.Optional;
import java.util.Set;

public class EndGame extends Building {
    private final BuildingEffect buildingEffect;
    private final CardType characterTypeMultiplier;

    public EndGame(String id, int endGamePP, int cost, int era, Integer numPlayers, BuildingEffect buildingEffect, CardType characterTypeForMultiplier) {
        super(id, endGamePP, cost, era, numPlayers);
        this.buildingEffect = buildingEffect;
        this.characterTypeMultiplier = characterTypeForMultiplier;
    }

    @Override
    public void activeEffect(Set<Player> players, Trigger t) {
        if(t == Trigger.END_GAME){
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
                    p.changePP(p.getCards().get(characterTypeMultiplier).size());
                }
                case BuildingEffect.END_PP_BONUS -> {
                    p.changePP(25);
                }
            }
        }
    }



    @Override
    protected String getEffectsLabel() {
        String base = CardToken.ENDGAME;
        if (characterTypeMultiplier != null) {
            String charToken = switch (characterTypeMultiplier) {
                case HUNTER   -> CardToken.HUNTER;
                case GATHERER -> CardToken.GATHERER;
                case BUILDER  -> CardToken.BUILDER;
                case SHAMAN   -> CardToken.SHAMAN;
                case ARTIST   -> CardToken.ARTIST;
                case INVENTOR -> CardToken.INVENTOR;
                default       -> "";
            };
            base += " x" + charToken;
        }
        return base;
    }
}
//GESTIRE IL CAMBIO DI PP MULTIPLIER IN BASE AL PERSONAGGIO
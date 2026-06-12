package org.adsl.server.model.cards.buildings;

import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.cards.buildings.utils.BuildingEffect;
import org.adsl.server.model.Player;
import org.adsl.shared.model.CardToken;
import java.util.Optional;
import java.util.Set;

/**
 * Building whose effect fires once at the end of the game ({@link Trigger#END_GAME}).
 * The specific effect is determined by {@link BuildingEffect}:
 * <ul>
 *   <li>{@code END_BUILDER_MULTIPLIER} – doubles end-game PP from all buildings.</li>
 *   <li>{@code PP_COMPLETE_SET} – grants 6 PP per card in the player's smallest
 *       card-type group.</li>
 *   <li>{@code END_PP_BONUS} – grants a flat 25 PP.</li>
 * </ul>
 */
public class EndGame extends Building {
    private final BuildingEffect buildingEffect;

    /**
     * Creates an EndGame building card.
     *
     * @param id             unique card identifier
     * @param endGamePP      prestige points scored at end-game for owning this building
     * @param cost           food cost to acquire this building
     * @param era            the era this card belongs to (1-3)
     * @param numPlayers     minimum number of players required for this card to be in play, or {@code null} if always included
     * @param buildingEffect which end-game effect this card applies (see {@link BuildingEffect})
     */
    public EndGame(String id, int endGamePP, int cost, int era, Integer numPlayers, BuildingEffect buildingEffect) {
        super(id, endGamePP, cost, era, numPlayers);
        this.buildingEffect = buildingEffect;
    }

    /**
     * At end-game ({@link Trigger#END_GAME}), applies the configured {@link BuildingEffect}:
     * doubles building PP via {@code BuildingBonus}, grants 6 PP per card in the
     * owner's smallest non-building card-type group, or grants a flat 25 PP.
     *
     * @param players the affected players (expects a singleton containing the owner)
     * @param t       the trigger that fired
     */
    @Override
    public void activeEffect(Set<Player> players, Trigger t) {
        if(t == Trigger.END_GAME){
            Optional<Player> playerContainer = players.stream().findFirst();
            if (playerContainer.isEmpty()) {
                return;
            }
            Player p = playerContainer.get();
            switch (buildingEffect) {
                case BuildingEffect.END_BUILDER_MULTIPLIER -> {
                    p.getBuildingBonus().setBuilderMultiplierPP(2);
                }
                case BuildingEffect.PP_COMPLETE_SET -> {
                    int min = p.getCards().entrySet().stream()
                            .filter(e -> e.getKey() != CardType.BUILDINGS)
                            .mapToInt(e -> e.getValue().size())
                            .min()
                            .orElse(0);

                    p.changePP(min * 6);
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
        switch (buildingEffect) {
            case END_BUILDER_MULTIPLIER -> base += "  2x" + CardToken.BUILDER + CardToken.PP;
            case PP_COMPLETE_SET -> base += "  6" + CardToken.PP + "x" + CardToken.SET;
            case END_PP_BONUS -> base += "  +25" + CardToken.PP;
            default -> base += "";
        };
        return base;
    }
}
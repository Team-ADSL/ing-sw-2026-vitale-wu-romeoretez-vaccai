package org.adsl.server.model.cards.buildings;

import org.adsl.server.model.cards.Card;
import org.adsl.server.model.cards.characters.Inventor;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Icon;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.cards.buildings.utils.BuildingEffect;
import org.adsl.server.model.Player;
import org.adsl.shared.model.CardToken;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Building that reacts each time the owner draws a card ({@link Trigger#DRAWING}).
 * Tracks all character cards drawn since the building was acquired and triggers
 * a food bonus when a condition is met:
 * <ul>
 *   <li>{@code FOOD_COMPLETE_SET} – grants 5 food the moment the owner has at
 *       least one card of every character type; the set is then reset.</li>
 *   <li>{@code COUPLE_INVENTOR} – grants 3 food when the owner has drawn a
 *       second {@code Inventor}.</li>
 * </ul>
 */
public class SinceBuilt extends Building {
    private final BuildingEffect buildingEffect;
    private Map<CardType, Set<Card>> characterInUse;

    /**
     * Creates a SinceBuilt building card with a fresh, empty tracking map
     * (one empty set per character type) for cards drawn after acquisition.
     *
     * @param id             unique card identifier
     * @param endGamePP      prestige points scored at end-game for owning this building
     * @param cost           food cost to acquire this building
     * @param trigger        unused, reserved for future configuration of the activation trigger
     * @param era            the era this card belongs to (1-3)
     * @param numPlayers     minimum number of players required for this card to be in play, or {@code null} if always included
     * @param buildingEffect which tracking effect this card applies (see {@link BuildingEffect})
     */
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

    /**
     * Creates a SinceBuilt building card with a pre-populated tracking map,
     * e.g. when restoring state from a saved game.
     *
     * @param id             unique card identifier
     * @param endGamePP      prestige points scored at end-game for owning this building
     * @param cost           food cost to acquire this building
     * @param trigger        unused, reserved for future configuration of the activation trigger
     * @param characterInUse the existing map of character cards tracked since this building was built
     * @param era            the era this card belongs to (1-3)
     * @param numPlayers     minimum number of players required for this card to be in play, or {@code null} if always included
     * @param buildingEffect which tracking effect this card applies (see {@link BuildingEffect})
     */
    public SinceBuilt(String id, int endGamePP, int cost, Trigger trigger, Map<CardType, Set<Card>> characterInUse,
                      int era, Integer numPlayers, BuildingEffect buildingEffect) {
        super(id, endGamePP, cost, era, numPlayers);
        this.characterInUse = characterInUse;
        this.buildingEffect = buildingEffect;
    }

    /**
     * When the owner draws a card ({@link Trigger#DRAWING}), records the last
     * drawn character in {@code characterInUse} and checks the configured
     * {@link BuildingEffect}:
     * <ul>
     *   <li>{@code FOOD_COMPLETE_SET} – if the owner now has at least one card of
     *       every character type, grants 5 food and removes one card from each
     *       type's tracked set (resetting the "set" progress).</li>
     *   <li>{@code COUPLE_INVENTOR} – if two tracked Inventors share the same icon,
     *       grants 3 food and removes that pair from tracking.</li>
     * </ul>
     *
     * @param players the affected players (expects a singleton containing the owner)
     * @param t       the trigger that fired
     */
    @Override
    public void activeEffect(Set<Player> players, Trigger t) {
        if(t == Trigger.DRAWING){
            Optional<Player> playerContainer = players.stream().findFirst();
            if (playerContainer.isEmpty()) {
                return;
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
                    Map<Icon, List<Card>> grouping = characterInUse.get(CardType.INVENTOR).stream()
                            .collect(Collectors.groupingBy(c -> ((Inventor) c).getIcon()));
                    Optional<Icon> coupleWithSameIcon = grouping.entrySet().stream()
                            .filter(entry -> entry.getValue().size() >= 2)
                            .map(Map.Entry::getKey)
                            .findFirst();

                    coupleWithSameIcon.ifPresent(icon -> {
                        List<Card> couple = grouping.get(icon).subList(0, 2);
                        p.changeFood(3);

                        couple.forEach(characterInUse.get(CardType.INVENTOR)::remove);
                    });
                }
            }
        }
    }

    @Override
    protected String getEffectsLabel() {
        return switch (buildingEffect) {
            case FOOD_COMPLETE_SET -> "+5" + CardToken.FOOD + " x" + CardToken.SET;
            case COUPLE_INVENTOR   -> "+3" + CardToken.FOOD + " x2" + CardToken.INVENTOR;
            default                -> "";
        };
    }
}

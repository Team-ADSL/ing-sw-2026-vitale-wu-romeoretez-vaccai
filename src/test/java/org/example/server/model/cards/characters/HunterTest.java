package org.example.server.model.cards.characters;

import org.example.server.model.cards.Card;
import org.example.server.model.cards.characters.Hunter;
import org.example.shared.enums.CardType;
import org.example.shared.enums.Trigger;
import org.example.shared.enums.Color;
import org.example.server.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class HunterTest {

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player("Tester", 5, 0, Color.RED);
    }

    @Test
    void canBeDrawn_alwaysTrue() {
        Hunter h = new Hunter(true, 1, Optional.empty());
        assertTrue(h.canBeDrawn(player));
    }

    @Test
    void insert_addsToHunterSet() {
        Hunter h = new Hunter(false, 1, Optional.empty());
        Map<CardType, Set<Card>> cards = initCardsMap();
        h.insert(cards);
        assertTrue(cards.get(CardType.HUNTER).contains(h));
    }

    @Test
    void insert_doesNotAddToOtherSets() {
        Hunter h = new Hunter(false, 1, Optional.empty());
        Map<CardType, Set<Card>> cards = initCardsMap();
        h.insert(cards);
        for (CardType type : CardType.values()) {
            if (type != CardType.HUNTER) {
                assertFalse(cards.get(type).contains(h));
            }
        }
    }

    // --- activeEffect ---

    @Test
    void activeEffect_extraFoodFalse_noFoodChange() {
        Hunter h = new Hunter(false, 1, Optional.empty());
        player.getCards().get(CardType.HUNTER).add(h);
        int foodBefore = player.getFood();
        h.activeEffect(Set.of(player), Trigger.DRAWING);
        assertEquals(foodBefore, player.getFood());
    }

    @Test
    void activeEffect_extraFoodTrue_wrongTrigger_noFoodChange() {
        Hunter h = new Hunter(true, 1, Optional.empty());
        player.getCards().get(CardType.HUNTER).add(h);
        int foodBefore = player.getFood();
        h.activeEffect(Set.of(player), Trigger.END_ROUND);
        assertEquals(foodBefore, player.getFood());
    }

    @Test
    void activeEffect_extraFoodTrue_drawingTrigger_oneHunter_addsFoodByHunterCount() {
        Hunter h = new Hunter(true, 1, Optional.empty());
        player.getCards().get(CardType.HUNTER).add(h); // 1 hunter in deck
        h.activeEffect(Set.of(player), Trigger.DRAWING);
        assertEquals(6, player.getFood()); // 5 + 1
    }

    @Test
    void activeEffect_extraFoodTrue_threeHunters_addsFoodByHunterCount() {
        // Add 3 hunters to deck first, THEN call activeEffect
        Hunter h1 = new Hunter(true, 1, Optional.empty());
        Hunter h2 = new Hunter(true, 1, Optional.empty());
        Hunter h3 = new Hunter(true, 1, Optional.empty());
        player.getCards().get(CardType.HUNTER).add(h1);
        player.getCards().get(CardType.HUNTER).add(h2);
        player.getCards().get(CardType.HUNTER).add(h3); // 3 hunters in deck
        h1.activeEffect(Set.of(player), Trigger.DRAWING);
        assertEquals(8, player.getFood()); // 5 + 3
    }

    @Test
    void activeEffect_emptyPlayerSet_doesNotThrow() {
        Hunter h = new Hunter(true, 1, Optional.empty());
        assertDoesNotThrow(() -> h.activeEffect(Set.of(), Trigger.DRAWING));
    }

    private Map<CardType, Set<Card>> initCardsMap() {
        Map<CardType, Set<Card>> cards = new HashMap<>();
        for (CardType t : CardType.values()) cards.put(t, new HashSet<>());
        return cards;
    }
}

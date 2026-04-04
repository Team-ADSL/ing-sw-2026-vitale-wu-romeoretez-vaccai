package org.example.server.model.cards.characters;

import org.example.server.model.cards.Card;
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
        player = new Player("Tester");
        player.changeFood(5);
    }

    @Test
    void canBeDrawn_alwaysTrue() {
        Hunter h = new Hunter("h", true, 1, null);
        assertTrue(h.canBeDrawn(player));
    }

    @Test
    void insert_addsToHunterSet() {
        Hunter h = new Hunter("h", false, 1, null);
        Map<CardType, Set<Card>> cards = initCardsMap();
        h.insert(cards);
        assertTrue(cards.get(CardType.HUNTER).contains(h));
    }

    @Test
    void insert_doesNotAddToOtherSets() {
        Hunter h = new Hunter("h", false, 1, null);
        Map<CardType, Set<Card>> cards = initCardsMap();
        h.insert(cards);
        for (CardType type : CardType.values()) {
            if (type != CardType.HUNTER) {
                assertFalse(cards.get(type).contains(h));
            }
        }
    }

    @Test
    void activeEffect_extraFoodFalse_noFoodChange() {
        Hunter h = new Hunter("h", false, 1, null);
        player.getCards().get(CardType.HUNTER).add(h);
        int foodBefore = player.getFood();
        h.activeEffect(Set.of(player), Trigger.DRAWING);
        assertEquals(foodBefore, player.getFood());
    }

    @Test
    void activeEffect_extraFoodTrue_wrongTrigger_noFoodChange() {
        Hunter h = new Hunter("h", true, 1, null);
        player.getCards().get(CardType.HUNTER).add(h);
        int foodBefore = player.getFood();
        h.activeEffect(Set.of(player), Trigger.END_ROUND);
        assertEquals(foodBefore, player.getFood());
    }

    @Test
    void activeEffect_extraFoodTrue_drawingTrigger_oneHunter_addsFoodByHunterCount() {
        Hunter h = new Hunter("h", true, 1, null);
        player.getCards().get(CardType.HUNTER).add(h);
        h.activeEffect(Set.of(player), Trigger.DRAWING);
        assertEquals(6, player.getFood());
    }

    @Test
    void activeEffect_extraFoodTrue_threeHunters_addsFoodByHunterCount() {
        Hunter h1 = new Hunter("h1", true, 1, null);
        Hunter h2 = new Hunter("h2", true, 1, null);
        Hunter h3 = new Hunter("h3", true, 1, null);
        player.getCards().get(CardType.HUNTER).add(h1);
        player.getCards().get(CardType.HUNTER).add(h2);
        player.getCards().get(CardType.HUNTER).add(h3);
        h1.activeEffect(Set.of(player), Trigger.DRAWING);
        assertEquals(8, player.getFood());
    }

    @Test
    void activeEffect_emptyPlayerSet_doesNotThrow() {
        Hunter h = new Hunter("h", true, 1, null);
        assertDoesNotThrow(() -> h.activeEffect(Set.of(), Trigger.DRAWING));
    }

    @Test
    void getId_returnsCorrectId() {
        Hunter h = new Hunter("hunter_01", true, 1, null);
        assertEquals("hunter_01", h.getId());
    }

    private Map<CardType, Set<Card>> initCardsMap() {
        Map<CardType, Set<Card>> cards = new HashMap<>();
        for (CardType t : CardType.values()) cards.put(t, new HashSet<>());
        return cards;
    }
}

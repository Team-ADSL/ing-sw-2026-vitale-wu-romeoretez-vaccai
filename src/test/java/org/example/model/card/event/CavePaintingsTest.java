package org.example.model.card.event;

import org.example.model.card.CardType;
import org.example.model.card.Trigger;
import org.example.model.card.character.Artist;
import org.example.model.game.Color;
import org.example.model.game.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class CavePaintingsTest {

    private Player player;
    // minArtists=2, lostPP=5, multiplierPP=3
    private CavePaintings cavePaintings;

    @BeforeEach
    void setUp() {
        player = new Player("Tester", 10, 0, Color.RED);
        cavePaintings = new CavePaintings(2, 5, 3, false, 1, Optional.empty());
    }

    @Test
    void canBeDrawn_alwaysFalse() {
        assertFalse(cavePaintings.canBeDrawn(player));
    }

    @Test
    void activeEffect_wrongTrigger_noEffect() {
        player.getCards().get(CardType.ARTIST).add(new Artist(1, Optional.empty()));
        cavePaintings.activeEffect(Set.of(player), Trigger.END_ROUND);
        assertEquals(0, player.getPp());
    }

    @Test
    void activeEffect_artistsAboveMin_awardsPP() {
        // 3 artists >= minArtists(2): PP += 3 * 3 = 9
        player.getCards().get(CardType.ARTIST).add(new Artist(1, Optional.empty()));
        player.getCards().get(CardType.ARTIST).add(new Artist(1, Optional.empty()));
        player.getCards().get(CardType.ARTIST).add(new Artist(1, Optional.empty()));
        cavePaintings.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(9, player.getPp());
    }

    @Test
    void activeEffect_artistsBelowMin_losesPP() {
        // 1 artist < minArtists(2): PP -= 5
        player.getCards().get(CardType.ARTIST).add(new Artist(1, Optional.empty()));
        cavePaintings.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(-5, player.getPp());
    }

    @Test
    void activeEffect_noArtists_losesPP() {
        // 0 artists < minArtists(2): PP -= 5
        cavePaintings.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(-5, player.getPp());
    }

    @Test
    void activeEffect_exactlyMinArtists_awardsPP() {
        // 2 artists == minArtists(2): PP += 2 * 3 = 6
        player.getCards().get(CardType.ARTIST).add(new Artist(1, Optional.empty()));
        player.getCards().get(CardType.ARTIST).add(new Artist(1, Optional.empty()));
        cavePaintings.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(6, player.getPp());
    }

    @Test
    void activeEffect_withArtistFoodBonus_addsFood() {
        // 2 artists, artistFood bonus active: food += 2
        player.getCards().get(CardType.ARTIST).add(new Artist(1, Optional.empty()));
        player.getCards().get(CardType.ARTIST).add(new Artist(1, Optional.empty()));
        player.getBuildingBonus().setArtistFood(true);
        cavePaintings.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(12, player.getFood()); // 10 + 2
    }

    @Test
    void activeEffect_withoutArtistFoodBonus_noExtraFood() {
        player.getCards().get(CardType.ARTIST).add(new Artist(1, Optional.empty()));
        player.getCards().get(CardType.ARTIST).add(new Artist(1, Optional.empty()));
        cavePaintings.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(10, player.getFood()); // unchanged
    }

    @Test
    void activeEffect_resetsBonus() {
        player.getBuildingBonus().setArtistFood(true);
        cavePaintings.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertFalse(player.getBuildingBonus().isArtistFood());
    }
}

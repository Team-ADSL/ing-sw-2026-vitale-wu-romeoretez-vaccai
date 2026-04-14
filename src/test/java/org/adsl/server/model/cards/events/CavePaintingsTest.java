package org.adsl.server.model.cards.events;

import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.cards.characters.Artist;
import org.adsl.server.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class CavePaintingsTest {

    private Player player;
    private CavePaintings cavePaintings;

    @BeforeEach
    void setUp() {
        player = new Player("Tester");
        player.changeFood(10);
        cavePaintings = new CavePaintings("cp_01", 2, 5, 3, false, 1, null);
    }

    @Test
    void canBeDrawn_alwaysFalse() {
        assertFalse(cavePaintings.canBeDrawn(player));
    }

    @Test
    void getId_returnsCorrectId() {
        assertEquals("cp_01", cavePaintings.getId());
    }

    @Test
    void activeEffect_wrongTrigger_noEffect() {
        player.getCards().get(CardType.ARTIST).add(new Artist("a", 1, null));
        cavePaintings.activeEffect(Set.of(player), Trigger.END_ROUND);
        assertEquals(0, player.getPp());
    }

    @Test
    void activeEffect_artistsAboveMin_awardsPP() {
        player.getCards().get(CardType.ARTIST).add(new Artist("a1", 1, null));
        player.getCards().get(CardType.ARTIST).add(new Artist("a2", 1, null));
        player.getCards().get(CardType.ARTIST).add(new Artist("a3", 1, null));
        cavePaintings.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(9, player.getPp());
    }

    @Test
    void activeEffect_artistsBelowMin_losesPP() {
        player.getCards().get(CardType.ARTIST).add(new Artist("a1", 1, null));
        cavePaintings.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(-5, player.getPp());
    }

    @Test
    void activeEffect_noArtists_losesPP() {
        cavePaintings.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(-5, player.getPp());
    }

    @Test
    void activeEffect_exactlyMinArtists_awardsPP() {
        player.getCards().get(CardType.ARTIST).add(new Artist("a1", 1, null));
        player.getCards().get(CardType.ARTIST).add(new Artist("a2", 1, null));
        cavePaintings.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(6, player.getPp());
    }

    @Test
    void activeEffect_withArtistFoodBonus_addsFood() {
        player.getCards().get(CardType.ARTIST).add(new Artist("a1", 1, null));
        player.getCards().get(CardType.ARTIST).add(new Artist("a2", 1, null));
        player.getBuildingBonus().setArtistFood(true);
        cavePaintings.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(12, player.getFood());
    }

    @Test
    void activeEffect_withoutArtistFoodBonus_noExtraFood() {
        player.getCards().get(CardType.ARTIST).add(new Artist("a1", 1, null));
        player.getCards().get(CardType.ARTIST).add(new Artist("a2", 1, null));
        cavePaintings.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(10, player.getFood());
    }

    @Test
    void activeEffect_resetsBonus() {
        player.getBuildingBonus().setArtistFood(true);
        cavePaintings.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertFalse(player.getBuildingBonus().isArtistFood());
    }
}

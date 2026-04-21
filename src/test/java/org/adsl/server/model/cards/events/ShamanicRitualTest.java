package org.adsl.server.model.cards.events;

import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.cards.characters.Shaman;
import org.adsl.server.model.Player;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ShamanicRitualTest {

    private ShamanicRitual ritual() {
        return new ShamanicRitual("sr_01", 5, 10, false, 1, null);
    }

    @Test
    void testCanBeDrawn_alwaysFalse() {
        Player p = new Player("Test");
        assertFalse(ritual().canBeDrawn(p));
    }

    @Test
    void testActiveEffect_wrongTrigger_noEffect() {
        Player p = new Player("Test");
        p.changePP(10);
        p.getCards().get(CardType.SHAMAN).add(new Shaman("s", 3, 1, null));
        ritual().activeEffect(Set.of(p), Trigger.END_ROUND);
        assertEquals(10, p.getPp());
    }

    @Test
    void testActiveEffect_singlePlayer_sameStarsMaxMin_noPPChange() {
        Player p = new Player("Solo");
        p.changePP(10);
        p.getCards().get(CardType.SHAMAN).add(new Shaman("s", 3, 1, null));
        ritual().activeEffect(Set.of(p), Trigger.EVENT_EXECUTION);
        assertEquals(10, p.getPp());
    }

    @Test
    void testActiveEffect_twoPlayers_differentStars_maxGainsPP_minLosesPP() {
        Player winner = new Player("Winner");
        Player loser  = new Player("Loser");
        winner.getCards().get(CardType.SHAMAN).add(new Shaman("s1", 3, 1, null));
        loser.getCards().get(CardType.SHAMAN).add(new Shaman("s2", 1, 1, null));
        Set<Player> players = new HashSet<>();
        players.add(winner);
        players.add(loser);
        ritual().activeEffect(players, Trigger.EVENT_EXECUTION);
        assertEquals(10, winner.getPp());
        assertEquals(-5, loser.getPp());
    }

    @Test
    void testActiveEffect_loserWithImmunity_doesNotLosePP() {
        Player winner = new Player("Winner");
        Player loser  = new Player("Loser");
        winner.getCards().get(CardType.SHAMAN).add(new Shaman("s1", 3, 1, null));
        loser.getCards().get(CardType.SHAMAN).add(new Shaman("s2", 1, 1, null));
        loser.getBuildingBonus().setNoRitualLostPP(true);
        Set<Player> players = new HashSet<>();
        players.add(winner);
        players.add(loser);
        ritual().activeEffect(players, Trigger.EVENT_EXECUTION);
        assertEquals(0, loser.getPp());
    }

    @Test
    void testActiveEffect_winnerWithShamanMultiplier_gainsDoubledPP() {
        Player winner = new Player("Winner");
        Player loser  = new Player("Loser");
        winner.getCards().get(CardType.SHAMAN).add(new Shaman("s1", 3, 1, null));
        loser.getCards().get(CardType.SHAMAN).add(new Shaman("s2", 1, 1, null));
        winner.getBuildingBonus().setShamanMulitiplierPP(2);
        Set<Player> players = new HashSet<>();
        players.add(winner);
        players.add(loser);
        ritual().activeEffect(players, Trigger.EVENT_EXECUTION);
        assertEquals(20, winner.getPp());
    }

    @Test
    void testActiveEffect_extraStarsFromBonus_countedInTotal() {
        Player winner = new Player("Winner");
        Player loser  = new Player("Loser");
        winner.getCards().get(CardType.SHAMAN).add(new Shaman("s1", 1, 1, null));
        loser.getCards().get(CardType.SHAMAN).add(new Shaman("s2", 1, 1, null));
        winner.getBuildingBonus().setExtraStars(3);
        Set<Player> players = new HashSet<>();
        players.add(winner);
        players.add(loser);
        ritual().activeEffect(players, Trigger.EVENT_EXECUTION);
        assertEquals(10, winner.getPp());
        assertEquals(-5, loser.getPp());
    }

    @Test
    void testActiveEffect_twoPlayersNoShamans_sameStars_noPPChange() {
        Player p1 = new Player("P1");
        p1.changePP(5);
        Player p2 = new Player("P2");
        p2.changePP(5);
        Set<Player> players = new HashSet<>();
        players.add(p1);
        players.add(p2);
        ritual().activeEffect(players, Trigger.EVENT_EXECUTION);
        assertEquals(5, p1.getPp());
        assertEquals(5, p2.getPp());
    }

    @Test
    void testActiveEffect_resetsBonus() {
        Player winner = new Player("Winner");
        Player loser  = new Player("Loser");
        winner.getCards().get(CardType.SHAMAN).add(new Shaman("s1", 3, 1, null));
        loser.getCards().get(CardType.SHAMAN).add(new Shaman("s2", 1, 1, null));
        winner.getBuildingBonus().setShamanMulitiplierPP(2);
        loser.getBuildingBonus().setNoRitualLostPP(true);
        Set<Player> players = new HashSet<>();
        players.add(winner);
        players.add(loser);
        ritual().activeEffect(players, Trigger.EVENT_EXECUTION);
        assertEquals(1, winner.getBuildingBonus().getShamanMulitiplierPP());
        assertFalse(loser.getBuildingBonus().isNoRitualLostPP());
    }
}

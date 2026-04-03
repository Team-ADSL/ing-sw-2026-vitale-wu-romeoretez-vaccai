package org.example.server.model.cards.events;

import org.example.shared.enums.CardType;
import org.example.shared.enums.Trigger;
import org.example.server.model.cards.characters.Shaman;
import org.example.shared.enums.Color;
import org.example.server.model.Player;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ShamanicRitualTest {

    private ShamanicRitual ritual() {
        return new ShamanicRitual("sr_01", 5, 10, false, 1, Optional.empty());
    }

    @Test
    void canBeDrawn_alwaysFalse() {
        Player p = new Player("Test");
        assertFalse(ritual().canBeDrawn(p));
    }

    @Test
    void getId_returnsCorrectId() {
        assertEquals("sr_01", ritual().getId());
    }

    @Test
    void activeEffect_wrongTrigger_noEffect() {
        Player p = new Player("Test");
        p.changePP(10);
        p.getCards().get(CardType.SHAMAN).add(new Shaman("s", 3, 1, Optional.empty()));
        ritual().activeEffect(Set.of(p), Trigger.END_ROUND);
        assertEquals(10, p.getPp());
    }

    @Test
    void activeEffect_singlePlayer_sameStarsMaxMin_noPPChange() {
        Player p = new Player("Solo");
        p.changePP(10);
        p.getCards().get(CardType.SHAMAN).add(new Shaman("s", 3, 1, Optional.empty()));
        ritual().activeEffect(Set.of(p), Trigger.EVENT_EXECUTION);
        assertEquals(10, p.getPp());
    }

    @Test
    void activeEffect_twoPlayers_differentStars_maxGainsPP_minLosesPP() {
        Player winner = new Player("Winner");
        Player loser  = new Player("Loser");
        winner.getCards().get(CardType.SHAMAN).add(new Shaman("s1", 3, 1, Optional.empty()));
        loser.getCards().get(CardType.SHAMAN).add(new Shaman("s2", 1, 1, Optional.empty()));
        Set<Player> players = new HashSet<>();
        players.add(winner);
        players.add(loser);
        ritual().activeEffect(players, Trigger.EVENT_EXECUTION);
        assertEquals(10, winner.getPp());
        assertEquals(-5, loser.getPp());
    }

    @Test
    void activeEffect_loserWithImmunity_doesNotLosePP() {
        Player winner = new Player("Winner");
        Player loser  = new Player("Loser");
        winner.getCards().get(CardType.SHAMAN).add(new Shaman("s1", 3, 1, Optional.empty()));
        loser.getCards().get(CardType.SHAMAN).add(new Shaman("s2", 1, 1, Optional.empty()));
        loser.getBuildingBonus().setNoRitualLostPP(true);
        Set<Player> players = new HashSet<>();
        players.add(winner);
        players.add(loser);
        ritual().activeEffect(players, Trigger.EVENT_EXECUTION);
        assertEquals(0, loser.getPp());
    }

    @Test
    void activeEffect_winnerWithShamanMultiplier_gainsDoubledPP() {
        Player winner = new Player("Winner");
        Player loser  = new Player("Loser");
        winner.getCards().get(CardType.SHAMAN).add(new Shaman("s1", 3, 1, Optional.empty()));
        loser.getCards().get(CardType.SHAMAN).add(new Shaman("s2", 1, 1, Optional.empty()));
        winner.getBuildingBonus().setShamanMulitiplierPP(2);
        Set<Player> players = new HashSet<>();
        players.add(winner);
        players.add(loser);
        ritual().activeEffect(players, Trigger.EVENT_EXECUTION);
        assertEquals(20, winner.getPp());
    }

    @Test
    void activeEffect_extraStarsFromBonus_countedInTotal() {
        Player winner = new Player("Winner");
        Player loser  = new Player("Loser");
        winner.getCards().get(CardType.SHAMAN).add(new Shaman("s1", 1, 1, Optional.empty()));
        loser.getCards().get(CardType.SHAMAN).add(new Shaman("s2", 1, 1, Optional.empty()));
        winner.getBuildingBonus().setExtraStars(3);
        Set<Player> players = new HashSet<>();
        players.add(winner);
        players.add(loser);
        ritual().activeEffect(players, Trigger.EVENT_EXECUTION);
        assertEquals(10, winner.getPp());
        assertEquals(-5, loser.getPp());
    }

    @Test
    void activeEffect_twoPlayersNoShamans_sameStars_noPPChange() {
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
    void activeEffect_resetsBonus() {
        Player winner = new Player("Winner");
        Player loser  = new Player("Loser");
        winner.getCards().get(CardType.SHAMAN).add(new Shaman("s1", 3, 1, Optional.empty()));
        loser.getCards().get(CardType.SHAMAN).add(new Shaman("s2", 1, 1, Optional.empty()));
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

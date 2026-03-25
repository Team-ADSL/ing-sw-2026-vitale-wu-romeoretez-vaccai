package org.example.model.cards.events;

import org.example.shared.enums.CardType;
import org.example.shared.enums.Trigger;
import org.example.model.cards.characters.Shaman;
import org.example.shared.enums.Color;
import org.example.model.Player;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ShamanicRitualTest {

    // lostPP=5, gainedPP=10
    private ShamanicRitual ritual() {
        return new ShamanicRitual(5, 10, false, 1, Optional.empty());
    }

    @Test
    void canBeDrawn_alwaysFalse() {
        Player p = new Player("Test", 0, 0, Color.RED);
        assertFalse(ritual().canBeDrawn(p));
    }

    @Test
    void activeEffect_wrongTrigger_noEffect() {
        Player p = new Player("Test", 0, 10, Color.RED);
        p.getCards().get(CardType.SHAMAN).add(new Shaman(3, 1, Optional.empty()));
        ritual().activeEffect(Set.of(p), Trigger.END_ROUND);
        assertEquals(10, p.getPp());
    }

    @Test
    void activeEffect_singlePlayer_samStarsMaxMin_noPPChange() {
        // Only one player → max == min → no change
        Player p = new Player("Solo", 0, 10, Color.RED);
        p.getCards().get(CardType.SHAMAN).add(new Shaman(3, 1, Optional.empty()));
        ritual().activeEffect(Set.of(p), Trigger.EVENT_EXECUTION);
        assertEquals(10, p.getPp()); // unchanged
    }

    @Test
    void activeEffect_twoPlayers_differentStars_maxGainsPP_minLosesPP() {
        Player winner = new Player("Winner", 0, 0, Color.RED);
        Player loser  = new Player("Loser",  0, 0, Color.BLUE);
        winner.getCards().get(CardType.SHAMAN).add(new Shaman(3, 1, Optional.empty()));
        loser.getCards().get(CardType.SHAMAN).add(new Shaman(1,  1, Optional.empty()));
        Set<Player> players = new HashSet<>();
        players.add(winner);
        players.add(loser);
        ritual().activeEffect(players, Trigger.EVENT_EXECUTION);
        assertEquals(10, winner.getPp());  // gains gainedPP=10
        assertEquals(-5, loser.getPp());   // loses lostPP=5
    }

    @Test
    void activeEffect_loserWithImmunity_doesNotLosePP() {
        Player winner = new Player("Winner", 0, 0, Color.RED);
        Player loser  = new Player("Loser",  0, 0, Color.BLUE);
        winner.getCards().get(CardType.SHAMAN).add(new Shaman(3, 1, Optional.empty()));
        loser.getCards().get(CardType.SHAMAN).add(new Shaman(1, 1, Optional.empty()));
        loser.getBuildingBonus().setNoRitualLostPP(true);
        Set<Player> players = new HashSet<>();
        players.add(winner);
        players.add(loser);
        ritual().activeEffect(players, Trigger.EVENT_EXECUTION);
        assertEquals(0, loser.getPp()); // immunity: no PP lost
    }

    @Test
    void activeEffect_winnerWithShamanMultiplier_gainsDoubledPP() {
        Player winner = new Player("Winner", 0, 0, Color.RED);
        Player loser  = new Player("Loser",  0, 0, Color.BLUE);
        winner.getCards().get(CardType.SHAMAN).add(new Shaman(3, 1, Optional.empty()));
        loser.getCards().get(CardType.SHAMAN).add(new Shaman(1, 1, Optional.empty()));
        winner.getBuildingBonus().setShamanMulitiplierPP(2); // double PP
        Set<Player> players = new HashSet<>();
        players.add(winner);
        players.add(loser);
        ritual().activeEffect(players, Trigger.EVENT_EXECUTION);
        assertEquals(20, winner.getPp()); // 10 * 2
    }

    @Test
    void activeEffect_extraStarsFromBonus_countedInTotal() {
        // winner has 1 shaman(1 star) + 3 extraStars = 4 total
        // loser has 1 shaman(1 star) = 1 total
        Player winner = new Player("Winner", 0, 0, Color.RED);
        Player loser  = new Player("Loser",  0, 0, Color.BLUE);
        winner.getCards().get(CardType.SHAMAN).add(new Shaman(1, 1, Optional.empty()));
        loser.getCards().get(CardType.SHAMAN).add(new Shaman(1,  1, Optional.empty()));
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
        Player p1 = new Player("P1", 0, 5, Color.RED);
        Player p2 = new Player("P2", 0, 5, Color.BLUE);
        // No shamans → both have 0 stars → max == min
        Set<Player> players = new HashSet<>();
        players.add(p1);
        players.add(p2);
        ritual().activeEffect(players, Trigger.EVENT_EXECUTION);
        assertEquals(5, p1.getPp()); // unchanged
        assertEquals(5, p2.getPp()); // unchanged
    }

    @Test
    void activeEffect_resetsBonus() {
        Player winner = new Player("Winner", 0, 0, Color.RED);
        Player loser  = new Player("Loser",  0, 0, Color.BLUE);
        winner.getCards().get(CardType.SHAMAN).add(new Shaman(3, 1, Optional.empty()));
        loser.getCards().get(CardType.SHAMAN).add(new Shaman(1, 1, Optional.empty()));
        winner.getBuildingBonus().setShamanMulitiplierPP(2);
        loser.getBuildingBonus().setNoRitualLostPP(true);
        Set<Player> players = new HashSet<>();
        players.add(winner);
        players.add(loser);
        ritual().activeEffect(players, Trigger.EVENT_EXECUTION);
        // After reset, multiplier is back to 1 and immunity is false
        assertEquals(1, winner.getBuildingBonus().getShamanMulitiplierPP());
        assertFalse(loser.getBuildingBonus().isNoRitualLostPP());
    }
}

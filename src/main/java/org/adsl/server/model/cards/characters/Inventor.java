package org.adsl.server.model.cards.characters;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Icon;
import org.adsl.shared.model.CardToken;

import java.util.Map;
import java.util.Set;

public class Inventor extends Character {

    private final Icon icon;

    public Inventor (String id, Icon icon, int era, Integer numPlayers) {
        super(id, era, numPlayers);
        this.icon = icon;
    }

    @Override
    public void insert(Map<CardType, Set<Card>> cards) {
        if(cards.containsKey(CardType.INVENTOR)) cards.get(CardType.INVENTOR).add(this);
    }

    public Icon getIcon() {
        return icon;
    }

    @Override
    protected String getTypeLabel() {
        return CardToken.INVENTOR + " " + eraToRoman(getEra());
    }

    @Override
    protected String getEffectsLabel() {
        return switch (icon) {
            case BOAT     -> CardToken.ICON_BOAT;
            case SPEARHEAD -> CardToken.ICON_SPEAR;
            case HOOK     -> CardToken.ICON_HOOK;
            case NECKLACE -> CardToken.ICON_NECKLACE;
            case BOWL     -> CardToken.ICON_BOWL;
            case ROPE     -> CardToken.ICON_ROPE;
            case TOTEM    -> CardToken.TOTEM;
            case FLUTE    -> CardToken.ICON_FLUTE;
            case LEATHER  -> CardToken.ICON_LEATHER;
            case BREAD    -> CardToken.FOOD;
        };
    }
}

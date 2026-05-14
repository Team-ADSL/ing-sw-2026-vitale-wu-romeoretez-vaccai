package org.adsl.client.view.tui;

import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Totem;

/**
 * Utility class for resolving card IDs to display information.
 *
 * Card IDs encode their type via prefix (e.g., "hunter_01", "shamanic_ritual_02").
 * This class provides type inference from IDs and display helpers used when
 * the full card catalog is not loaded from JSON.
 */
public final class CardCatalog {

    private CardCatalog() {}

    /** Infers CardType from a card ID based on its prefix. */
    public static CardType typeFromId(String id) {
        if (id == null) return null;
        // Order matters: check longer prefixes first to avoid partial matches
        if (id.startsWith("hunter_"))          return CardType.HUNTER;
        if (id.startsWith("hunt_"))            return CardType.HUNT;
        if (id.startsWith("shamanic_ritual_")) return CardType.SHAMANIC_RITUAL;
        if (id.startsWith("shaman_"))          return CardType.SHAMAN;
        if (id.startsWith("builder_"))         return CardType.BUILDER;
        if (id.startsWith("cave_paintings_"))  return CardType.CAVE_PAINTINGS;
        if (id.startsWith("sustenance_"))      return CardType.SUSTENANCE;
        if (id.startsWith("gatherer_"))        return CardType.GATHERER;
        if (id.startsWith("inventor_"))        return CardType.INVENTOR;
        if (id.startsWith("artist_"))          return CardType.ARTIST;
        return CardType.BUILDINGS;
    }

    /** Short display label for a card type (fits in an 8-char card slot). */
    public static String typeLabel(CardType type) {
        if (type == null) return "UNKNOWN";
        return switch (type) {
            case HUNTER         -> "HUNTER";
            case GATHERER       -> "GATHERER";
            case SHAMAN         -> "SHAMAN";
            case BUILDER        -> "BUILDER";
            case INVENTOR       -> "INVENTOR";
            case ARTIST         -> "ARTIST";
            case BUILDINGS      -> "BUILDING";
            case HUNT           -> "HUNT";
            case SUSTENANCE     -> "SUSTAIN";
            case SHAMANIC_RITUAL -> "RITUAL";
            case CAVE_PAINTINGS -> "PAINTINGS";
        };
    }

    /** Whether a card type is an event card. */
    public static boolean isEvent(CardType type) {
        if (type == null) return false;
        return type == CardType.HUNT
                || type == CardType.SUSTENANCE
                || type == CardType.SHAMANIC_RITUAL
                || type == CardType.CAVE_PAINTINGS;
    }

    /** Whether a card type is a character card. */
    public static boolean isCharacter(CardType type) {
        if (type == null) return false;
        return !isEvent(type) && type != CardType.BUILDINGS;
    }

    /** Single-character symbol for a card type, used in compact tribe display. */
    public static String typeSymbol(CardType type) {
        if (type == null) return "?";
        return switch (type) {
            case HUNTER         -> "H";
            case GATHERER       -> "G";
            case SHAMAN         -> "S";
            case BUILDER        -> "B";
            case INVENTOR       -> "I";
            case ARTIST         -> "A";
            case BUILDINGS      -> "⌂";
            case SHAMANIC_RITUAL, HUNT, SUSTENANCE, CAVE_PAINTINGS -> null;
        };
    }

    /** ANSI color name for a totem (for display labels only). */
    public static String totemLabel(Totem totem) {
        if (totem == null) return "---";
        return switch (totem) {
            case RED    -> "RED";
            case BLUE   -> "BLUE";
            case WHITE  -> "WHITE";
            case BLACK  -> "BLACK";
            case YELLOW -> "YELLOW";
        };
    }
}

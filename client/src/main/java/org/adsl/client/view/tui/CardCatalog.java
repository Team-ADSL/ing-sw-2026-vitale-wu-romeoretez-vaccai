package org.adsl.client.view.tui;

import org.adsl.client.view.tui.render.TuiColor;
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

    /**
     * Infers the {@link CardType} from a card ID based on its prefix.
     *
     * @param id the card ID (e.g. "hunter_01"), may be {@code null}
     * @return the inferred card type, {@code CardType.BUILDINGS} as the
     *         default for unrecognized prefixes, or {@code null} if {@code id} is {@code null}
     */
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

    /**
     * Short display label for a card type (fits in an 8-char card slot).
     *
     * @param type the card type, may be {@code null}
     * @return a short label, or {@code "UNKNOWN"} if {@code type} is {@code null}
     */
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

    /**
     * @param type the card type, may be {@code null}
     * @return {@code true} if {@code type} is one of the event card types
     *         (hunt, sustenance, shamanic ritual, cave paintings)
     */
    public static boolean isEvent(CardType type) {
        if (type == null) return false;
        return type == CardType.HUNT
                || type == CardType.SUSTENANCE
                || type == CardType.SHAMANIC_RITUAL
                || type == CardType.CAVE_PAINTINGS;
    }

    /**
     * @param type the card type, may be {@code null}
     * @return {@code true} if {@code type} is a character card, i.e. neither
     *         an event card (see {@link #isEvent}) nor {@code CardType.BUILDINGS}
     */
    public static boolean isCharacter(CardType type) {
        if (type == null) return false;
        return !isEvent(type) && type != CardType.BUILDINGS;
    }

    /**
     * Single-character symbol for a card type, used in compact tribe display.
     *
     * @param type the card type, may be {@code null}
     * @return a one-character symbol, {@code "?"} if {@code type} is
     *         {@code null}, or {@code null} for event card types (which have no symbol)
     */
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

    /**
     * Display name for a totem color (for labels only).
     *
     * @param totem the totem, may be {@code null}
     * @return the totem's color name, or {@code "---"} if {@code totem} is {@code null}
     */
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

    /**
     * Maps a totem to the {@link TuiColor} used to render its player's name,
     * tiles and panel highlights.
     *
     * @param totem the totem, may be {@code null}
     * @return the matching color, or {@link TuiColor#WHITE} if {@code totem} is {@code null}
     */
    public static TuiColor totemColor(Totem totem) {
        if (totem == null) return TuiColor.WHITE;
        return switch (totem) {
            case RED    -> TuiColor.RED;
            case BLUE   -> TuiColor.CYAN;
            case WHITE  -> TuiColor.WHITE;
            case BLACK  -> TuiColor.DARK_PURPLE;
            case YELLOW -> TuiColor.YELLOW;
        };
    }
}

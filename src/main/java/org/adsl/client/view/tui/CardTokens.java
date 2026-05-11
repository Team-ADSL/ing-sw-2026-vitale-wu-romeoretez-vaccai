package org.adsl.client.view.tui;

import org.adsl.shared.model.CardToken;

import java.util.Map;

/**
 * Converts card token strings (produced server-side) to emoji for TUI display.
 * All emoji live here; server code stays ASCII-only.
 */
public final class CardTokens {
    private CardTokens() {}

    private static final Map<String, String> EMOJI_MAP = Map.ofEntries(
        Map.entry(CardToken.BUILDING,      "BUILDING"),
        Map.entry(CardToken.HUNTER,        "HUNTER"),
        Map.entry(CardToken.GATHERER,      "GATHERER"),
        Map.entry(CardToken.BUILDER,       "BUILDER"),
        Map.entry(CardToken.SHAMAN,        "SHAMAN"),
        Map.entry(CardToken.ARTIST,        "ARTIST"),
        Map.entry(CardToken.INVENTOR,      "INVENTOR"),
        Map.entry(CardToken.HUNT,          "HUNT"),
        Map.entry(CardToken.SUSTENANCE,    "SUSTENANCE"),
        Map.entry(CardToken.RITUAL,        "RITUAL"),
        Map.entry(CardToken.PAINTINGS,     "PAINTINGS"),
        Map.entry(CardToken.PP,            "🌟"),
        Map.entry(CardToken.SHAMAN_STAR,   "★"),
        Map.entry(CardToken.FOOD,          "🍖"),
        Map.entry(CardToken.SET,           "🌈"),
        Map.entry(CardToken.ENDGAME,       "🏁"),
        Map.entry(CardToken.EXTRA_MOVE,    "⏩"),
        Map.entry(CardToken.TOTEM,         "🗿"),
        Map.entry(CardToken.SHIELD,        "🛡️"),
        Map.entry(CardToken.ICON_BOAT,     "🚢"),
        Map.entry(CardToken.ICON_SPEAR,    "🗡️"),
        Map.entry(CardToken.ICON_HOOK,     "🪝"),
        Map.entry(CardToken.ICON_NECKLACE, "📿"),
        Map.entry(CardToken.ICON_BOWL,     "🥣"),
        Map.entry(CardToken.ICON_ROPE,     "🪢"),
        Map.entry(CardToken.ICON_FLUTE,    "🪈"),
        Map.entry(CardToken.ICON_LEATHER,  "🧥")
    );

    // Abbreviations for type tokens when they appear in effect labels (3 letters)
    private static final Map<String, String> TYPE_ABBR = Map.ofEntries(
        Map.entry(CardToken.BUILDING,   "BUI"),
        Map.entry(CardToken.HUNTER,     "HUN"),
        Map.entry(CardToken.GATHERER,   "GAT"),
        Map.entry(CardToken.BUILDER,    "BLD"),
        Map.entry(CardToken.SHAMAN,     "SHA"),
        Map.entry(CardToken.ARTIST,     "ART"),
        Map.entry(CardToken.INVENTOR,   "INV"),
        Map.entry(CardToken.HUNT,       "HNT"),
        Map.entry(CardToken.SUSTENANCE, "SUS"),
        Map.entry(CardToken.RITUAL,     "RIT"),
        Map.entry(CardToken.PAINTINGS,  "PAI")
    );

    public static String toEmoji(String s) {
        if (s == null) return null;
        String result = applyFoodCost(s);
        result = result.replace(CardToken.MEAT, "🍖");
        for (Map.Entry<String, String> entry : EMOJI_MAP.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static String toEffectLabel(String s) {
        if (s == null) return null;
        String result = s;
        for (Map.Entry<String, String> entry : TYPE_ABBR.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        result = applyFoodCost(result);
        result = result.replace(CardToken.MEAT, "🍖");
        for (Map.Entry<String, String> entry : EMOJI_MAP.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    // [FOOD_COST]-N (sconto) → +N🍖 ; [FOOD_COST]N (costo) → -N🍖
    private static String applyFoodCost(String s) {
        String result = s.replaceAll("\\[FOOD_COST\\]-(\\d+)", "-$1🍖");
        result = result.replaceAll("\\[FOOD_COST\\](\\d+)", "-$1🍖");
        return result;
    }
}

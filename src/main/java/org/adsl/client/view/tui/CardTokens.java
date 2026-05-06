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
        Map.entry(CardToken.BUILDING,      "🏛️"),
        Map.entry(CardToken.HUNTER,        "🏹"),
        Map.entry(CardToken.GATHERER,      "🧺"),
        Map.entry(CardToken.BUILDER,       "🔨"),
        Map.entry(CardToken.SHAMAN,        "🔮"),
        Map.entry(CardToken.ARTIST,        "🎨"),
        Map.entry(CardToken.INVENTOR,      "💡"),
        Map.entry(CardToken.HUNT,          "🐗"),
        Map.entry(CardToken.SUSTENANCE,    "🍲"),
        Map.entry(CardToken.RITUAL,        "🎭"),
        Map.entry(CardToken.PAINTINGS,     "🖌️"),
        Map.entry(CardToken.PP,            "🌟"),
        Map.entry(CardToken.SHAMAN_STAR,   "★"),
        Map.entry(CardToken.FOOD_COST,     "💰"),
        Map.entry(CardToken.FOOD,          "🍞"),
        Map.entry(CardToken.MEAT,          "🍖"),
        Map.entry(CardToken.MEAL,          "🍲"),
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

    public static String toEmoji(String s) {
        if (s == null) return null;
        String result = s;
        for (Map.Entry<String, String> entry : EMOJI_MAP.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }
}

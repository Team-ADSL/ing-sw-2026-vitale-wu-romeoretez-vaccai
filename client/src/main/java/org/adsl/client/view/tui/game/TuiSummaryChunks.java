package org.adsl.client.view.tui.game;

import org.adsl.client.view.tui.CardTokens;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.model.CardDTO;
import org.adsl.shared.model.PlayerDTO;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure builder of the compact per-player card-summary fragments shown in the TUI
 * game screen (the "N HUNTER", "N BUILDER: 🌟:x,🍖:-y", "│ BUILDINGS: …" pieces).
 * Free of any rendering: it turns a {@link PlayerDTO} into a list of tagged
 * {@link Chunk}s; the screen colours and places them. Extracted from
 * {@code GameScreen} so the parsing is in one testable place.
 */
public final class TuiSummaryChunks {

    private TuiSummaryChunks() {}

    /** Tagged text fragment: when {@code highlight} is true it renders in the player's totem color. */
    public record Chunk(String text, boolean highlight) {}

    /** Separator glyph used between a player's stats and their buildings section. */
    public static final String SEP = "│";
    private static final String STAR_EMOJI = "★";

    private static final Pattern PP_FROM_EFFECT  = Pattern.compile("(\\d+)\\[PP\\]");
    private static final Pattern DISC_FROM_EFFECT = Pattern.compile("-(\\d+)\\[FOOD\\]");
    private static final Pattern PP_FROM_TYPE    = Pattern.compile("(\\d+)\\[PP\\]");

    /** Builds character-section chunks: highlighted "N TYPE:" labels, plain detail strings. */
    public static void appendCharacterChunks(List<Chunk> out, PlayerDTO p) {
        int h = sizeOf(p, CardType.HUNTER);
        int g = sizeOf(p, CardType.GATHERER);
        int a = sizeOf(p, CardType.ARTIST);
        if (h > 0) out.add(new Chunk(h + " HUNTER ", false));
        if (g > 0) out.add(new Chunk(g + " GATHERER ", false));
        if (a > 0) out.add(new Chunk(a + " ARTIST ", false));

        Set<CardDTO> builders = cardsOf(p, CardType.BUILDER);
        if (!builders.isEmpty()) {
            int pp = 0, disc = 0;
            for (CardDTO c : builders) {
                pp   += firstInt(PP_FROM_EFFECT,  c.effectsLabel());
                disc += firstInt(DISC_FROM_EFFECT, c.effectsLabel());
            }
            out.add(new Chunk(builders.size() + " BUILDER: ", false));
            out.add(new Chunk("🌟:" + pp + ",🍖:-" + disc + " ", false));
        }

        Set<CardDTO> shamans = cardsOf(p, CardType.SHAMAN);
        if (!shamans.isEmpty()) {
            int stars = 0;
            for (CardDTO c : shamans) {
                stars += occurrences(c.effectsLabel(), "[SHAMAN_STAR]");
            }
            out.add(new Chunk(shamans.size() + " SHAMAN: ", false));
            out.add(new Chunk(STAR_EMOJI + ":" + stars + " ", false));
        }

        Set<CardDTO> inventors = cardsOf(p, CardType.INVENTOR);
        if (!inventors.isEmpty()) {
            Map<String, Integer> iconCount = new LinkedHashMap<>();
            for (CardDTO c : inventors) {
                String emoji = CardTokens.toEmoji(c.effectsLabel() == null ? "" : c.effectsLabel().trim());
                iconCount.merge(emoji, 1, Integer::sum);
            }
            out.add(new Chunk(inventors.size() + " INVENTOR: ", false));
            StringBuilder icons = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, Integer> e : iconCount.entrySet()) {
                if (!first) icons.append(", ");
                first = false;
                icons.append(e.getKey()).append(':').append(e.getValue());
            }
            icons.append(' ');
            out.add(new Chunk(icons.toString(), false));
        }
    }

    /** Buildings chunk: highlighted "│ BUILDINGS:" label, plain enumeration after. */
    public static void appendBuildingsChunks(List<Chunk> out, PlayerDTO p) {
        Set<CardDTO> buildings = cardsOf(p, CardType.BUILDINGS);
        if (buildings.isEmpty()) return;
        out.add(new Chunk(SEP + " BUILDINGS: ", false));
        StringBuilder body = new StringBuilder();
        boolean first = true;
        for (CardDTO c : buildings) {
            if (!first) body.append(" / ");
            first = false;
            int pp = firstInt(PP_FROM_TYPE, c.typeLabel());
            body.append("pp:").append(pp);
            String eff = CardTokens.toEffectLabel(c.effectsLabel());
            if (eff != null && !eff.isBlank()) {
                body.append('[').append(eff).append(']');
            }
        }
        out.add(new Chunk(body.toString(), false));
    }

    private static int sizeOf(PlayerDTO p, CardType t) {
        Set<CardDTO> s = p.cards().get(t);
        return s == null ? 0 : s.size();
    }

    private static Set<CardDTO> cardsOf(PlayerDTO p, CardType t) {
        Set<CardDTO> s = p.cards().get(t);
        return s == null ? Set.of() : s;
    }

    private static int firstInt(Pattern pattern, String s) {
        if (s == null) return 0;
        Matcher m = pattern.matcher(s);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private static int occurrences(String s, String needle) {
        if (s == null || needle.isEmpty()) return 0;
        int count = 0, idx = 0;
        while ((idx = s.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }
}

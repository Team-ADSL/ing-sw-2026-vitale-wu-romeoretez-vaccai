package org.adsl.client.view.gui;

import javafx.scene.image.Image;
import javafx.scene.text.Font;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Totem;

import java.util.HashMap;
import java.util.Map;

/**
 * Central lookup for every PNG asset and the bundled Christmas Chalk font used
 * by the GUI. Resolves IDs from server DTOs to the matching classpath resource
 * under {@code /assets/...}, caches the resulting {@link Image} instances so
 * repeated lookups don't re-decode from the jar.
 */
public final class ImageCatalog {

    private static final Map<String, Image> CACHE = new HashMap<>();
    private static Font CHRISTMAS_CHALK;
    private static Font ROBOTO;

    private ImageCatalog() {}

    public static void loadFonts() {
        if (CHRISTMAS_CHALK == null) {
            CHRISTMAS_CHALK = Font.loadFont(
                    ImageCatalog.class.getResourceAsStream("/assets/fonts/ChristmasChalk.ttf"), 24);
        }
        if (ROBOTO == null) {
            ROBOTO = Font.loadFont(
                    ImageCatalog.class.getResourceAsStream("/assets/fonts/Roboto/static/Roboto-Regular.ttf"), 14);
        }
    }

    public static Font chalkFont(double size) {
        if (CHRISTMAS_CHALK == null) loadFonts();
        return Font.font(CHRISTMAS_CHALK.getFamily(), size);
    }

    public static Font robotoFont(double size) {
        if (ROBOTO == null) loadFonts();
        return Font.font(ROBOTO.getFamily(), size);
    }

    public static Image load(String path) {
        return CACHE.computeIfAbsent(path, p -> {
            var stream = ImageCatalog.class.getResourceAsStream(p);
            if (stream == null) {
                System.err.println("[ImageCatalog] missing resource: " + p);
                throw new IllegalStateException("missing resource: " + p);
            }
            Image img = new Image(stream);
            if (img.isError()) {
                System.err.println("[ImageCatalog] decode error for " + p + ": " + img.getException());
            }
            return img;
        });
    }

    // ── Cards ────────────────────────────────────────────────────────────────

    /**
     * Front asset for a card. Filename must equal the JSON id (e.g. "hunter_01").
     * Reads from {@code cards_front_cropped/} — originals live in
     * {@code cards_front/} and are pre-processed by
     * {@code tools/preprocess_cards.sh}.
     */
    public static Image cardFront(String id) {
        return load("/assets/cards_front_cropped/" + id + ".png");
    }

    /**
     * Back asset matching a card's category and era. Buildings get the
     * buildings-era back, final events get the end-game back, everyone else
     * the normal era back. Cropped variants from
     * {@code cards_back_cropped/}.
     */
    public static Image cardBack(CardType type, int era, boolean isFinal) {
        String file;
        if (type == CardType.BUILDINGS) {
            file = "buildings_era_" + era;
        } else if (isEvent(type) && isFinal) {
            file = "cards_era_end";
        } else {
            file = "cards_era_" + era;
        }
        return load("/assets/cards_back_cropped/" + file + ".png");
    }

    private static boolean isEvent(CardType type) {
        return type == CardType.HUNT
                || type == CardType.SUSTENANCE
                || type == CardType.SHAMANIC_RITUAL
                || type == CardType.CAVE_PAINTINGS;
    }

    // ── Totems ──────────────────────────────────────────────────────────────

    /** 2D totem sprite for a given color. File: {@code 2d_totem_<color>.png}. */
    public static Image totem2D(Totem totem) {
        return load("/assets/totems/2d_totem_" + totem.name().toLowerCase() + ".png");
    }

    /** 3D standing-totem sprite for a given color. File: {@code 3d_totem_<color>.png}. */
    public static Image totem3D(Totem totem) {
        return load("/assets/totems/3d_totem_" + totem.name().toLowerCase() + ".png");
    }

    // ── Offer tiles ─────────────────────────────────────────────────────────

    /** Offer tile sprite. ID matches the JSON entry (e.g. "offer_tile_a"). */
    public static Image offerTile(String id) {
        return load("/assets/offer_tiles/" + id + ".png");
    }

    // ── Order (turn-order) tile ───────────────────────────────────────────────

    /** Turn-order tile sprite for the given player count. File: {@code order_tile_<n>p.png}. */
    public static Image orderTile(int numPlayers) {
        return load("/assets/order_tiles/order_tile_" + numPlayers + "p.png");
    }

    // ── Main deck card back ───────────────────────────────────────────────────

    /** Card back for the main draw pile of the given era (1–3). File: {@code cards_era_<era>.png}. */
    public static Image deckCardBack(int era) {
        return load("/assets/cards_back_cropped/cards_era_" + era + ".png");
    }

    // ── PP / Food chip backgrounds ──────────────────────────────────────────

    public static Image foodBack()   { return load("/assets/fustelle_back/food_back.png"); }
    public static Image ppEarnBack() { return load("/assets/fustelle_front/pp_earn_back.png"); }
    public static Image ppLossBack() { return load("/assets/fustelle_back/pp_loss_back.png"); }
}

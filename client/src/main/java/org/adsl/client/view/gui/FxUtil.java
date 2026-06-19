package org.adsl.client.view.gui;

import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

import java.util.function.Supplier;

/**
 * Small JavaFX helpers shared by the game-screen sub-views: the selection glow,
 * the disabled-card darken filter, the card-pile shadow, and image loaders that
 * swallow failures so a missing asset never crashes a render. Kept in one place
 * so {@code GameScreen} and its renderers ({@code BoardRenderer},
 * {@code PlayerPanelsRenderer}, …) all draw with the same effects.
 */
public final class FxUtil {

    private FxUtil() {}

    /** White glow marking a selected/hovered card (same look across board + decks). */
    public static DropShadow selectedGlow() {
        DropShadow glow = new DropShadow(10, Color.WHITE);
        glow.setSpread(0.30);
        return glow;
    }

    /**
     * Darkens a node while keeping it fully opaque. Used for non-interactable
     * cards/tiles so they read as disabled without going transparent over the
     * background image.
     */
    public static ColorAdjust darken() {
        ColorAdjust ca = new ColorAdjust();
        ca.setBrightness(-0.45);
        return ca;
    }

    /** Soft offset shadow that makes a single card-back read as a small pile. */
    public static DropShadow deckShadow() {
        return new DropShadow(6, 3, 3, Color.rgb(0, 0, 0, 0.55));
    }

    /** Loads an image from a classpath resource, or {@code null} if missing/broken. */
    public static Image image(String path) {
        try {
            var in = FxUtil.class.getResourceAsStream(path);
            return in != null ? new Image(in) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Runs an image supplier, swallowing failures (returns {@code null}). */
    public static Image image(Supplier<Image> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            System.err.println("[GUI] image load failed: " + e.getMessage());
            return null;
        }
    }

    /** Wraps a supplied image in an {@link ImageView}, or {@code null} on failure. */
    public static ImageView imageView(Supplier<Image> supplier) {
        try {
            return new ImageView(supplier.get());
        } catch (Exception e) {
            System.err.println("[GUI] image load failed: " + e.getMessage());
            return null;
        }
    }
}

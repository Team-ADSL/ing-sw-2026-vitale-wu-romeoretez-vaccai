package org.adsl.client.view.gui;

/**
 * Pure layout arithmetic shared by the responsive GUI. No JavaFX scene-graph
 * dependencies so it can be unit-tested in isolation.
 */
public final class LayoutMath {

    private LayoutMath() {}

    /**
     * The single card width shared by every card row. Sizes the most-constrained
     * single-line row ({@code maxCardsInARow}) to fit {@code availWidth} minus the
     * inter-card gaps, clamped to {@code [min, max]}.
     */
    public static double cardWidth(double availWidth, int maxCardsInARow,
                                   double gap, double min, double max) {
        if (maxCardsInARow <= 0) return max;
        double w = (availWidth - gap * (maxCardsInARow - 1)) / maxCardsInARow;
        return Math.max(min, Math.min(max, w));
    }

    /**
     * How many fixed-width cards fit in a hand row of {@code availWidth}. Always
     * at least 1 so a page is never empty.
     */
    public static int perPage(double availWidth, double cardWidth, double gap) {
        if (cardWidth <= 0) return 1;
        return Math.max(1, (int) Math.floor((availWidth + gap) / (cardWidth + gap)));
    }

    /**
     * Uniform shrink-to-fit scale for content of {@code contentW x contentH} inside
     * {@code availW x availH}. Clamped to {@code [minScale, 1.0]} — never enlarges,
     * never shrinks past the floor (so content stays legible rather than vanishing).
     */
    public static double fitScale(double contentW, double contentH,
                                  double availW, double availH, double minScale) {
        if (contentW <= 0 || contentH <= 0) return 1.0;
        double s = Math.min(availW / contentW, availH / contentH);
        if (s > 1.0) s = 1.0;
        if (s < minScale) s = minScale;
        return s;
    }
}

package org.adsl.client.view.gui;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * Wraps a screen root in a centering holder that uniformly scales the content
 * DOWN when the window is smaller than the content's laid-out size — the shared
 * "never clip" fallback for every screen. When the content fits, the scale is
 * 1.0 (no-op). It never enlarges content and never shrinks past
 * {@link #MIN_SCALE}, so menu screens stay natural-sized on big monitors and
 * legible on tiny ones.
 *
 * <p>Reads the content's actual laid-out size (not preferred): a StackPane keeps
 * its child at the child's minimum size when the holder is smaller, so the child
 * overflows and we scale by {@code holder/child}. Scaling is a visual transform
 * and does not feed back into layout, so this cannot loop.
 */
public final class ResponsiveScaler {

    private static final double MIN_SCALE = 0.4;

    private ResponsiveScaler() {}

    /** Returns a holder Region that contains and scales {@code content} to fit. */
    public static Region wrap(Parent content) {
        StackPane holder = new StackPane(content);
        holder.setMinSize(0, 0);
        StackPane.setAlignment(content, Pos.CENTER);

        Runnable apply = () -> {
            if (!(content instanceof Region r)) return;
            double s = LayoutMath.fitScale(
                    r.getWidth(), r.getHeight(),
                    holder.getWidth(), holder.getHeight(), MIN_SCALE);
            content.setScaleX(s);
            content.setScaleY(s);
        };

        holder.widthProperty().addListener((_, _, _) -> apply.run());
        holder.heightProperty().addListener((_, _, _) -> apply.run());
        if (content instanceof Region r) {
            r.widthProperty().addListener((_, _, _) -> apply.run());
            r.heightProperty().addListener((_, _, _) -> apply.run());
        }
        return holder;
    }
}

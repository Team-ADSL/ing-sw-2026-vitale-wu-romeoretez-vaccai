import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Generates pre-baked PNGs for every ChalkBadge text variant used in the game.
 * Eliminating runtime JavaFX snapshots removes all ChalkBadge-related lag.
 *
 * Three output directories under client/src/assets/badge_glyphs/:
 *
 *   stroke_29/ — "x0".."x30", "0".."20"      (14.5pt×2, WHITE fill + BLACK OUTSIDE stroke)
 *                Used for xN card-counts and inventor-icon counts.
 *
 *   plain_25/  — "0".."50"                    (12.5pt×2, WHITE fill, no stroke)
 *                Used for builder PP and shaman stars (tinted at runtime via Blend).
 *
 *   plain_20/  — "0".."15", "m0".."m15"       (10pt×2, WHITE fill, no stroke)
 *                Used for builder/gatherer discounts; "m" prefix = minus sign.
 *
 * Run (headless, JDK only — no JavaFX needed):
 *   javac -d tools/out tools/BadgeGlyphGenerator.java
 *   java  -Djava.awt.headless=true -cp tools/out BadgeGlyphGenerator
 */
public final class BadgeGlyphGenerator {

    private static final double SCALE = 2.0;  // matches ChalkBadge.SNAPSHOT_SCALE

    // Font sizes: logical pt × SCALE → AWT pt
    private static final float SIZE_STROKE = (float)(14.5 * SCALE);  // 29pt
    private static final float SIZE_PLAIN_25 = (float)(12.5 * SCALE); // 25pt
    private static final float SIZE_PLAIN_20 = (float)(10.0 * SCALE); // 20pt

    // stroke_29 uses WHITE fill + BLACK OUTSIDE stroke
    private static final Color WHITE   = Color.WHITE;
    private static final Color BLACK   = Color.BLACK;
    private static final float STROKE_W = (float)(1.1 * SCALE * 2); // DECK_COUNT_STROKE*SCALE*2

    public static void main(String[] args) throws Exception {
        String fontPath = "client/src/assets/fonts/ChristmasChalk.ttf";
        String outRoot  = "client/src/assets/badge_glyphs";

        Font base = Font.createFont(Font.TRUETYPE_FONT, new File(fontPath));

        Font stroke29  = base.deriveFont(SIZE_STROKE);
        Font plain25   = base.deriveFont(SIZE_PLAIN_25);
        Font plain20   = base.deriveFont(SIZE_PLAIN_20);

        // ── stroke_29 ──────────────────────────────────────────────────────────
        String dir29 = outRoot + "/stroke_29";
        new File(dir29).mkdirs();
        for (int n = 0; n <= 30; n++) {
            save(renderStroke(stroke29, "x" + n), dir29 + "/x" + n + ".png");
        }
        for (int n = 0; n <= 20; n++) {
            save(renderStroke(stroke29, String.valueOf(n)), dir29 + "/" + n + ".png");
        }

        // ── plain_25 ───────────────────────────────────────────────────────────
        String dir25 = outRoot + "/plain_25";
        new File(dir25).mkdirs();
        for (int n = 0; n <= 50; n++) {
            save(renderPlain(plain25, String.valueOf(n)), dir25 + "/" + n + ".png");
        }

        // ── plain_20 ───────────────────────────────────────────────────────────
        String dir20 = outRoot + "/plain_20";
        new File(dir20).mkdirs();
        for (int n = 0; n <= 15; n++) {
            save(renderPlain(plain20, String.valueOf(n)), dir20 + "/" + n + ".png");
            // "m" prefix = minus sign (avoids awkward leading-dash filenames)
            save(renderPlain(plain20, "-" + n), dir20 + "/m" + n + ".png");
        }

        System.out.println("Done: badge glyphs generated.");
    }

    // WHITE fill + BLACK centred-stroke (inner half hidden by fill = OUTSIDE effect)
    private static BufferedImage renderStroke(Font font, String text) {
        BasicStroke stroke = new BasicStroke(STROKE_W, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        FontRenderContext frc = new FontRenderContext(null, true, true);
        GlyphVector gv = font.createGlyphVector(frc, text);
        Rectangle2D b = gv.getOutline().getBounds2D();

        int pad = (int) Math.ceil(STROKE_W / 2.0) + 2;
        int w = (int) Math.ceil(b.getWidth())  + pad * 2;
        int h = (int) Math.ceil(b.getHeight()) + pad * 2;

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,RenderingHints.VALUE_STROKE_PURE);

        var outline = gv.getOutline((float)(-b.getMinX() + pad), (float)(-b.getMinY() + pad));
        g.setColor(BLACK);  g.setStroke(stroke); g.draw(outline);
        g.setColor(WHITE);  g.fill(outline);
        g.dispose();
        return img;
    }

    // WHITE fill only — tinted at runtime via Blend(MULTIPLY) to any target color
    private static BufferedImage renderPlain(Font font, String text) {
        FontRenderContext frc = new FontRenderContext(null, true, true);
        GlyphVector gv = font.createGlyphVector(frc, text);
        Rectangle2D b = gv.getOutline().getBounds2D();

        int pad = 2;
        int w = (int) Math.ceil(b.getWidth())  + pad * 2;
        int h = (int) Math.ceil(b.getHeight()) + pad * 2;

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,RenderingHints.VALUE_STROKE_PURE);

        var outline = gv.getOutline((float)(-b.getMinX() + pad), (float)(-b.getMinY() + pad));
        g.setColor(WHITE); g.fill(outline);
        g.dispose();
        return img;
    }

    private static void save(BufferedImage img, String path) throws Exception {
        ImageIO.write(img, "png", new File(path));
    }
}

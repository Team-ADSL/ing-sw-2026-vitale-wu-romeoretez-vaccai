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
 * One-off generator for the move-hint count glyphs (option 3: pre-baked PNGs so
 * there is zero runtime snapshot cost / lag spike). Renders 0..MAX in three
 * colour variants with a white outer outline, in the ChristmasChalk font, at 2x
 * for HiDPI crispness. Round join/cap avoids the miter spikes the chalk font's
 * sharp corners otherwise produce.
 *
 * Output: client/src/assets/move_counts/{up,down,grey}/<n>.png
 *
 * Run (headless, JDK only):
 *   javac -d tools/out tools/CountGlyphGenerator.java
 *   java  -Djava.awt.headless=true -cp tools/out CountGlyphGenerator
 */
public final class CountGlyphGenerator {

    private static final double SCALE      = 2.0;   // matches the old 2x snapshot
    private static final double FONT_SIZE  = 22.0 * SCALE;
    private static final double OUTSIDE_PX = 1.0 * SCALE;   // 1px logical outline

    // up: #1b7056   down: #9f3850
    private static final Color UP_FILL   = new Color(0x1b, 0x70, 0x56);
    private static final Color DOWN_FILL = new Color(0x9f, 0x38, 0x50);
    private static final Color OUTLINE   = Color.WHITE;

    public static void main(String[] args) throws Exception {
        String fontPath  = "client/src/assets/fonts/ChristmasChalk.ttf";
        String outRoot   = "client/src/assets/move_counts";

        Font base = Font.createFont(Font.TRUETYPE_FONT, new File(fontPath))
                        .deriveFont((float) FONT_SIZE);

        emit(base, UP_FILL,   outRoot + "/up");
        emit(base, DOWN_FILL, outRoot + "/down");
        System.out.println("Done: glyphs generated.");
    }

    private static void emit(Font font, Color fill, String dir) throws Exception {
        new File(dir).mkdirs();
        // Render 0, 1, 2
        for (int n = 0; n <= 2; n++) {
            BufferedImage img = render(font, fill, String.valueOf(n));
            ImageIO.write(img, "png", new File(dir + "/" + n + ".png"));
        }
        // Render -1 to -20
        for (int n = 1; n <= 20; n++) {
            BufferedImage img = render(font, fill, "-" + n);
            ImageIO.write(img, "png", new File(dir + "/-" + n + ".png"));
        }
    }

    private static BufferedImage render(Font font, Color fill, String text) {
        // Centred stroke of 2*OUTSIDE_PX gives OUTSIDE_PX outside the glyph; the
        // fill drawn on top hides the inner half, leaving a clean outer outline.
        float strokeW = (float) (OUTSIDE_PX * 2.0);
        BasicStroke stroke = new BasicStroke(strokeW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f);

        FontRenderContext frc = new FontRenderContext(null, true, true);
        GlyphVector gv = font.createGlyphVector(frc, text);
        Rectangle2D b = gv.getOutline().getBounds2D();

        int pad = (int) Math.ceil(OUTSIDE_PX) + 2;
        int w = (int) Math.ceil(b.getWidth())  + pad * 2;
        int h = (int) Math.ceil(b.getHeight()) + pad * 2;

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // Move glyph (origin at baseline) so its visual box sits inside the pad.
        var outline = gv.getOutline((float) (-b.getMinX() + pad), (float) (-b.getMinY() + pad));

        g.setColor(OUTLINE);
        g.setStroke(stroke);
        g.draw(outline);
        g.setColor(fill);
        g.fill(outline);
        g.dispose();
        return img;
    }
}

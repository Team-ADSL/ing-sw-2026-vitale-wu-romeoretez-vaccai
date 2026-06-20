package org.adsl.client.view.gui;

import javafx.scene.paint.Color;

/**
 * Central registry of GUI layout constants for the JavaFX client.
 *
 * <p>Groups constants by the component they belong to. All values are
 * {@code public static final} so any renderer or screen can import them
 * directly without holding an instance.
 *
 * <p>Not all magic numbers live here: purely internal implementation details
 * (e.g. snapshot-oversampling scales, single-method clamp floors) stay in
 * their originating class to keep context local. Only constants that are
 * either already cross-class or represent meaningful "layout parameters" that
 * a reader would expect to tweak in one place belong here.
 */
public final class GuiConstants {

    private GuiConstants() {}

    // ── Window (GUI) ──────────────────────────────────────────────────────────

    /** Application window title. */
    public static final String WINDOW_TITLE  = "MESOS";
    /** Default window width in pixels. */
    public static final double WINDOW_W      = 1280;
    /** Default window height in pixels. */
    public static final double WINDOW_H      = 800;
    /** Minimum window width in pixels. */
    public static final double MIN_W         = 900;
    /** Minimum window height in pixels. */
    public static final double MIN_H         = 620;

    // ── GameScreen layout ─────────────────────────────────────────────────────

    /** Margin (px) between the uniformly-scaled board group and the window edges. */
    public static final double BOARD_MARGIN          = 24.0;
    /** Fixed vertical space reserved for the TOP opponent panel row. */
    public static final double TOP_PANEL_H_RESERVE   = 132.0;
    /**
     * Top inset pushing LEFT/RIGHT panels down so their top aligns with
     * the central "MESOS — …" header.
     */
    public static final double SIDE_PANEL_TOP        = 178.0;
    /**
     * Debounce interval (ms) for resize-driven board re-renders.
     * The board rebuilds only once the window has been still for this long.
     */
    public static final double RESIZE_DEBOUNCE_MS    = 90;

    // ── BoardRenderer ─────────────────────────────────────────────────────────

    /** Card image aspect ratio (height / width). */
    public static final double CARD_ASPECT    = 1.484;
    /** Maximum card width (px) used as the fixed reference size for the board. */
    public static final double CARD_MAX_W     = 95.0;
    /** Horizontal gap between consecutive cards in a row. */
    public static final double CARD_GAP       = 6.0;
    /** Offer-tile image aspect ratio (height / width). */
    public static final double TILE_ASPECT    = 1.65;
    /** Maximum offer-tile width (px). */
    public static final double TILE_MAX_W     = 80.0;

    // Order-tile pixel geometry (native PNG dimensions)
    /** Native width of the order-tile PNG. */
    public static final double ORDER_TILE_W   = 624.0;
    /** Native height of the order-tile PNG. */
    public static final double ORDER_TILE_H   = 965.0;
    /** Native width of the 3-D totem PNG. */
    public static final double TOTEM3D_W      = 220.0;
    /** Native height of the 3-D totem PNG. */
    public static final double TOTEM3D_H      = 384.0;
    /** X anchor of the totem foot as a fraction of TOTEM3D_W. */
    public static final double TOTEM_ANCHOR_X = 113.0 / TOTEM3D_W;
    /** Y anchor of the totem foot as a fraction of TOTEM3D_H. */
    public static final double TOTEM_ANCHOR_Y = 339.0 / TOTEM3D_H;
    /** Horizontal center of an order-tile cell, as a fraction of tile width. */
    public static final double ORDER_CELL_X   = 0.5;
    /** Totem width on the order tile, as a fraction of ORDER_TILE_W. */
    public static final double ORDER_TOTEM_W_FRAC = 201.0 / ORDER_TILE_W;

    // Offer-tile pixel geometry
    /** Native width of the offer-tile PNG. */
    public static final double OFFER_TILE_W   = 602.0;
    /** Native height of the offer-tile PNG. */
    public static final double OFFER_TILE_H   = 1004.0;
    /** Totem width on an offer tile, as a fraction of tile width. */
    public static final double OFFER_TOTEM_W_FRAC =
            (201.0 / 965.0) * (OFFER_TILE_H / OFFER_TILE_W);

    // ── PlayerPanelsRenderer ──────────────────────────────────────────────────

    /**
     * Maximum width (px) of the narrow left/right opponent panels.
     * Read by GameScreen to reserve side space before scaling the board.
     */
    public static final double LR_PANEL_W         = 170;
    /** Width of the fixed identity column (totem + name + chips) in the self panel. */
    public static final double IDENTITY_COL_W     = 100;
    /** Chip width for the self panel. */
    public static final double CHIP_WIDTH         = 50;
    /** Chip width for opponent panels (smaller). */
    public static final double CHIP_WIDTH_SM      = 40;
    /** Totem 2-D icon size in the self panel. */
    public static final double SELF_TOTEM         = 36;
    /** Totem 2-D icon size in opponent panels. */
    public static final double OPP_TOTEM          = 24;

    // Deck-summary icons
    /** Width of a deck-summary icon for opponents. */
    public static final double DECK_ICON_W        = 65.0;
    /** Width of a deck-summary icon for the self panel (larger). */
    public static final double DECK_ICON_SELF_W   = 92.0;
    /** Aspect ratio of a deck-summary icon (native height / width). */
    public static final double DECK_ICON_AR       = 147.0 / 174.0;
    /** Gap between deck-summary icons. */
    public static final double DECK_ICON_GAP      = 6.0;
    /** Number of columns in the narrow LEFT/RIGHT panel icon grid. */
    public static final int    DECK_SIDE_COLS     = 2;
    /** Width of the extra-info badge shown in the icon corner. */
    public static final double BADGE_W            = 29.0;
    /** Font size for extra-info badge numbers (normal). */
    public static final double BADGE_FONT         = 12.5;
    /** Font size for discount badges (smaller, fits "-NN"). */
    public static final double DISCOUNT_FONT      = 10.0;
    /** Font size for xN count and inventor number badges. */
    public static final double DECK_COUNT_FONT    = 14.5;
    /** Stroke width for xN count badges. */
    public static final double DECK_COUNT_STROKE  = 1.1;
    /** Card width used in the drill-down view for the self player. */
    public static final double DECK_CARD_W_SELF   = 100.0;
    /** Card width used in the drill-down view for opponents. */
    public static final double DECK_CARD_W_OPP    = 62.0;

    // Extra-info badge native pixel spaces + centered coordinate of the dynamic number
    /** Native width of the PP-card badge image. */
    public static final double PP_W  = 161;
    /** Native height of the PP-card badge image. */
    public static final double PP_H  = 137;
    /** X center of the number inside the PP badge (native px). */
    public static final double PP_CX = 82;
    /** Y center of the number inside the PP badge (native px). */
    public static final double PP_CY = 50;
    /** Native width of the food-loss badge image. */
    public static final double FL_W  = 188;
    /** Native height of the food-loss badge image. */
    public static final double FL_H  = 127;
    /** X center of the number inside the food-loss badge (native px). */
    public static final double FL_CX = 41;
    /** Y center of the number inside the food-loss badge (native px). */
    public static final double FL_CY = 73;
    /** Native width of the shaman-stars badge image. */
    public static final double SS_W  = 188;
    /** Native height of the shaman-stars badge image. */
    public static final double SS_H  = 127;
    /** X center of the number inside the shaman-stars badge (native px). */
    public static final double SS_CX = 53;
    /** Y center of the number inside the shaman-stars badge (native px). */
    public static final double SS_CY = 62;

    // Card-type ink colours for extra-info badges
    /** Text colour for Builder deck badges. */
    public static final Color BUILDER_INK  = Color.web("#541620");
    /** Text colour for Gatherer deck badges. */
    public static final Color GATHERER_INK = Color.web("#f57a13");
    /** Text colour for Shaman deck badges. */
    public static final Color SHAMAN_INK   = Color.web("#9a445d");

    // ── Chip ─────────────────────────────────────────────────────────────────

    /** Fill colour for food-chip digits. */
    public static final Color  FOOD_FILL            = Color.WHITE;
    /** Outline colour for food-chip digits. */
    public static final Color  FOOD_STROKE          = Color.BLACK;
    /** Fill colour for positive PP chip digits. */
    public static final Color  PP_POS_FILL          = Color.web("#4A1F18");
    /** Fill colour for negative PP chip digits. */
    public static final Color  PP_NEG_FILL          = Color.web("#F2E2C6");
    /** Vertical offset of the food number as a fraction of chip width. */
    public static final double FOOD_OFFSET_Y_RATIO  = 0.18;
    /** Base font size as a fraction of chip width. */
    public static final double BASE_FONT_RATIO      = 0.5;
    /** Stroke width as a fraction of chip width. */
    public static final double STROKE_RATIO         = 0.025;
    /** PP font scale multiplier applied on top of BASE_FONT_RATIO. */
    public static final double PP_BASE_MUL          = 0.90;
    /** Additional shrink for negative PP chips (to fit the minus sign). */
    public static final double PP_NEG_SHRINK        = 0.90;
    /**
     * Per-length font shrink multipliers for chip numbers.
     * Index 0 unused; strings longer than the array clamp to the last value.
     */
    public static final double[] LENGTH_SHRINK = { 1.0, 1.0, 0.88, 0.65, 0.50, 0.40 };

    // ── FloatingLog ───────────────────────────────────────────────────────────

    /** Maximum width of the floating log panel. */
    public static final double LOG_MAX_W           = 420;
    /** Vertical margin subtracted from window height when capping the scroll viewport. */
    public static final double LOG_VERTICAL_MARGIN = 120;
    /** Maximum height of the floating log in collapsed state. */
    public static final double LOG_COLLAPSED_MAX_H = 300;
}

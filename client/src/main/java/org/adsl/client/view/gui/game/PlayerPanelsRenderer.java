package org.adsl.client.view.gui.game;

import static org.adsl.client.view.gui.GuiConstants.*;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.adsl.client.view.gui.ChalkBadge;
import org.adsl.client.view.gui.Chip;
import org.adsl.client.view.gui.FxUtil;
import org.adsl.client.view.gui.ImageCatalog;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.model.CardDTO;
import org.adsl.shared.model.GameDTO;
import org.adsl.shared.model.PlayerDTO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders the player panels around the board: the local player's panel at the
 * bottom and the opponents' panels along the top/left/right. Each panel shows
 * the seven deck-summary icons (with per-type extra-info badges and {@code xN}
 * counts) or, when an icon is clicked, drills down into that type's cards.
 *
 * <p>Owns the drill-down open state and the stable seat order; clicking an icon
 * or the back button updates that state and asks the screen to re-render via the
 * supplied callback.
 */
public final class PlayerPanelsRenderer {

    /**
     * Maximum width (px) of the narrow left/right opponent panels.
     * Alias of {@link org.adsl.client.view.gui.GuiConstants#LR_PANEL_W} kept
     * here so callers using {@code PlayerPanelsRenderer.LR_PANEL_W} still compile.
     */
    public static final double LR_PANEL_W = GuiConstants.LR_PANEL_W;

    // All other layout constants are defined in GuiConstants.

    /** The seven hand card types, in the order their icons are laid out. */
    private static final List<CardType> DECK_ORDER = List.of(
            CardType.HUNTER, CardType.GATHERER, CardType.BUILDER, CardType.SHAMAN,
            CardType.INVENTOR, CardType.ARTIST, CardType.BUILDINGS);

    private enum Slot { LEFT, TOP, RIGHT }

    private final HBox selfPanelBox;
    private final HBox topPlayersBox;
    private final VBox leftPlayersBox;
    private final VBox rightPlayersBox;
    private final String username;
    private final Runnable rerender;

    /** Player name -> the card type currently drilled into (icon toggle open). */
    private final Map<String, CardType> openDeck = new HashMap<>();
    /** Fixed seat order, captured from the first snapshot so panels never shuffle. */
    private List<String> playerOrder;
    private GameDTO game;

    public PlayerPanelsRenderer(HBox selfPanelBox, HBox topPlayersBox, VBox leftPlayersBox,
                                VBox rightPlayersBox, String username, Runnable rerender) {
        this.selfPanelBox = selfPanelBox;
        this.topPlayersBox = topPlayersBox;
        this.leftPlayersBox = leftPlayersBox;
        this.rightPlayersBox = rightPlayersBox;
        this.username = username;
        this.rerender = rerender;
    }

    public void render(GameDTO game) {
        this.game = game;
        renderSelfPanel();
        renderOpponentPanels();
    }

    private PlayerDTO findMe() {
        if (game == null || username == null || game.players() == null) return null;
        for (PlayerDTO p : game.players()) {
            if (username.equals(p.name())) return p;
        }
        return null;
    }

    // ── Self panel (bottom) ──────────────────────────────────────────────────

    private void renderSelfPanel() {
        selfPanelBox.getChildren().clear();
        PlayerDTO me = findMe();
        if (me == null) return;

        VBox identity = buildSelfIdentity(me);

        if (openDeck.containsKey(me.name())) {
            // Drill-down open: keep the self totem + name + chips unchanged; show the
            // cards in a row with the back button on the RIGHT.
            HBox cardsRow = new HBox(DECK_ICON_GAP);
            cardsRow.setAlignment(Pos.CENTER_LEFT);
            cardsRow.getChildren().addAll(deckCardNodes(me, openDeck.get(me.name()), DECK_CARD_W_SELF));
            selfPanelBox.getChildren().addAll(identity, cardsRow, buildDeckBackButton(me.name(), 18));
            return;
        }

        // Default: totem + name + chips on the left, the seven deck icons on the right.
        selfPanelBox.getChildren().addAll(identity, buildDeckIcons(me, false, DECK_ICON_SELF_W));
    }

    /** Self identity column: totem on top, name + food/pp chips below. */
    private VBox buildSelfIdentity(PlayerDTO me) {
        VBox left = new VBox(4);
        left.setAlignment(Pos.CENTER);
        left.setPrefWidth(IDENTITY_COL_W);
        left.setMinWidth(IDENTITY_COL_W);

        if (me.totem() != null) {
            ImageView totem = FxUtil.imageView(() -> ImageCatalog.totem2D(me.totem()));
            if (totem != null) {
                totem.setFitWidth(SELF_TOTEM);
                totem.setPreserveRatio(true);
                left.getChildren().add(totem);
            }
        }

        Label name = new Label("★ " + me.name());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #f5deb3;");
        HBox stats = new HBox(8, Chip.food(me.food(), CHIP_WIDTH), Chip.pp(me.pp(), CHIP_WIDTH));
        stats.setAlignment(Pos.CENTER);
        left.getChildren().addAll(name, stats);
        return left;
    }

    // ── Opponent panels (sides) ──────────────────────────────────────────────

    /**
     * Slot assignment for opponents based on total player count. Returned in
     * seat order starting from self+1 going clockwise.
     */
    private List<Slot> slotsFor(int playerCount) {
        return switch (playerCount) {
            case 2 -> List.of(Slot.TOP);
            case 3 -> List.of(Slot.TOP, Slot.TOP);
            case 4 -> List.of(Slot.LEFT, Slot.TOP, Slot.RIGHT);
            case 5 -> List.of(Slot.LEFT, Slot.TOP, Slot.TOP, Slot.RIGHT);
            default -> List.of();
        };
    }

    private void renderOpponentPanels() {
        topPlayersBox.getChildren().clear();
        leftPlayersBox.getChildren().clear();
        rightPlayersBox.getChildren().clear();

        if (game.players() == null || game.players().isEmpty()) return;

        // Capture the seat order once, then always render against it so panels
        // don't shuffle when the server reorders players() between phases.
        if (playerOrder == null) {
            playerOrder = new ArrayList<>();
            for (PlayerDTO p : game.players()) playerOrder.add(p.name());
        }
        List<PlayerDTO> all = new ArrayList<>(game.players());
        all.sort(Comparator.comparingInt(p -> {
            int i = playerOrder.indexOf(p.name());
            return i < 0 ? Integer.MAX_VALUE : i;
        }));

        int n = all.size();
        int selfIdx = findMyIndex(all);

        // Build clockwise seat order starting from self+1.
        List<PlayerDTO> opponents = new ArrayList<>();
        for (int k = 1; k < n; k++) {
            int idx = (selfIdx >= 0 ? (selfIdx + k) % n : k - 1);
            opponents.add(all.get(idx));
        }

        List<Slot> slots = slotsFor(n);
        int count = Math.min(slots.size(), opponents.size());
        for (int i = 0; i < count; i++) {
            Slot slot = slots.get(i);
            PlayerDTO opp = opponents.get(i);
            // When a side opponent's deck is open the panel mirrors the (wide) self
            // layout, so the narrow LR cap is lifted for that render to avoid clipping.
            boolean open = openDeck.containsKey(opp.name());
            Node panel = buildOpponentPanel(opp, slot);
            switch (slot) {
                case LEFT  -> { leftPlayersBox.setMaxWidth(open ? Region.USE_COMPUTED_SIZE : LR_PANEL_W + 40); leftPlayersBox.getChildren().add(panel); }
                case RIGHT -> { rightPlayersBox.setMaxWidth(open ? Region.USE_COMPUTED_SIZE : LR_PANEL_W + 40); rightPlayersBox.getChildren().add(panel); }
                case TOP   -> topPlayersBox.getChildren().add(panel);
            }
        }
    }

    private int findMyIndex(List<PlayerDTO> players) {
        for (int i = 0; i < players.size(); i++) {
            if (username != null && username.equals(players.get(i).name())) return i;
        }
        return -1;
    }

    /**
     * Header for the opponent panel.
     * <p>{@code horizontal=true} (TOP slot): row of [totem, name, spacer, chips].
     * Wide layout, fills horizontal space.
     * <p>{@code horizontal=false} (LEFT/RIGHT): column of [totem, name, chips]
     * stacked, like the self panel — keeps the panel narrow so the center
     * board has more room.
     */
    private Pane buildOpponentHeader(PlayerDTO p, boolean horizontal) {
        ImageView totem = null;
        if (p.totem() != null) {
            totem = FxUtil.imageView(() -> ImageCatalog.totem2D(p.totem()));
            if (totem != null) {
                totem.setFitWidth(OPP_TOTEM);
                totem.setPreserveRatio(true);
            }
        }
        Label name = new Label(p.name());
        name.setStyle("-fx-font-weight: bold; -fx-text-fill: #f5deb3; -fx-font-size: 13px;");

        if (horizontal) {
            HBox h = new HBox(8);
            h.setAlignment(Pos.CENTER_LEFT);
            if (totem != null) h.getChildren().add(totem);
            h.getChildren().add(name);
            Region grow = new Region();
            HBox.setHgrow(grow, Priority.ALWAYS);
            h.getChildren().add(grow);
            h.getChildren().addAll(
                Chip.food(p.food(), CHIP_WIDTH_SM),
                Chip.pp(p.pp(),     CHIP_WIDTH_SM));
            return h;
        } else {
            VBox v = new VBox(4);
            v.setAlignment(Pos.CENTER);
            if (totem != null) v.getChildren().add(totem);
            v.getChildren().add(name);
            HBox chips = new HBox(6,
                Chip.food(p.food(), CHIP_WIDTH_SM),
                Chip.pp(p.pp(),     CHIP_WIDTH_SM));
            chips.setAlignment(Pos.CENTER);
            v.getChildren().add(chips);
            return v;
        }
    }

    private Node buildOpponentPanel(PlayerDTO p, Slot slot) {
        boolean topSlot = (slot == Slot.TOP);
        boolean open = openDeck.containsKey(p.name());

        VBox panel = new VBox(6);
        panel.getStyleClass().add("panel");
        panel.setPadding(new Insets(8, 10, 8, 10));
        panel.setAlignment(Pos.TOP_CENTER);

        if (open) {
            // Drill-down: identity (totem + name + food/pp) on the LEFT in opponent
            // sizing, that type's cards (smaller than the self panel) in the middle,
            // back button on the right — all inside the usual panel background.
            HBox cardsRow = new HBox(DECK_ICON_GAP);
            cardsRow.setAlignment(Pos.CENTER_LEFT);
            cardsRow.getChildren().addAll(deckCardNodes(p, openDeck.get(p.name()), DECK_CARD_W_OPP));
            HBox content = new HBox(8, buildOpponentHeader(p, false), cardsRow, buildDeckBackButton(p.name(), 13));
            content.setAlignment(Pos.CENTER_LEFT);
            panel.getChildren().add(content);
        } else {
            // Default: full header (totem + chips) + the seven icons.
            panel.getChildren().addAll(buildOpponentHeader(p, topSlot),
                    buildDeckIcons(p, !topSlot, DECK_ICON_W));
        }

        // Narrow cap only for the closed icon view; the open row lays out
        // horizontally and would be clipped by LR_PANEL_W.
        if (!topSlot && !open) {
            panel.setPrefWidth(LR_PANEL_W);
            panel.setMaxWidth(LR_PANEL_W);
        }
        return panel;
    }

    // ── Deck-summary icons ────────────────────────────────────────────────────

    /**
     * Builds the seven deck-summary icons for a player. {@code grid=false} lays
     * them in a single row (wide self/TOP panels); {@code grid=true} uses a
     * {@link #DECK_SIDE_COLS}-column grid for the narrow LEFT/RIGHT panels.
     */
    private Pane buildDeckIcons(PlayerDTO p, boolean grid, double iconW) {
        if (!grid) {
            HBox row = new HBox(DECK_ICON_GAP);
            row.setAlignment(Pos.CENTER);
            for (CardType t : DECK_ORDER) row.getChildren().add(buildDeckIconCell(p, t, iconW));
            return row;
        }
        GridPane g = new GridPane();
        g.setHgap(DECK_ICON_GAP);
        g.setVgap(DECK_ICON_GAP);
        g.setAlignment(Pos.CENTER);
        for (int i = 0; i < DECK_ORDER.size(); i++) {
            g.add(buildDeckIconCell(p, DECK_ORDER.get(i), iconW), i % DECK_SIDE_COLS, i / DECK_SIDE_COLS);
        }
        return g;
    }

    /** One deck icon with its {@code xN} count and any type-specific extra-info badge(s). */
    private StackPane buildDeckIconCell(PlayerDTO p, CardType type, double iconW) {
        double w = iconW;
        double h = w * DECK_ICON_AR;
        StackPane cell = new StackPane();
        cell.setMinSize(w, h);
        cell.setPrefSize(w, h);
        cell.setMaxSize(w, h);

        ImageView icon = FxUtil.imageView(() -> ImageCatalog.deckIcon(type));
        if (icon != null) {
            icon.setFitWidth(w);
            icon.setPreserveRatio(true);
            cell.getChildren().add(icon);
        }

        switch (type) {
            case BUILDER -> {
                // Bottom-left: total builder PP on the laurel. Top-right: discount
                // (shown as a negative, with a smaller font so "-NN" fits).
                StackPane pp = buildBadge(FxUtil.image(ImageCatalog::ppCard), PP_W, PP_H, BADGE_W,
                        String.valueOf(p.builderPP()), BUILDER_INK, BADGE_FONT, PP_CX, PP_CY);
                StackPane.setAlignment(pp, Pos.BOTTOM_LEFT);
                StackPane disc = buildBadge(FxUtil.image(ImageCatalog::foodLoss), FL_W, FL_H, BADGE_W,
                        "-" + p.builderDiscount(), BUILDER_INK, DISCOUNT_FONT, FL_CX, FL_CY);
                StackPane.setAlignment(disc, Pos.TOP_RIGHT);
                cell.getChildren().addAll(pp, disc);
            }
            case GATHERER -> {
                // Top-right: discount (negative) on the drumstick — same drawing as
                // the builder but with the maroon strokes repainted orange.
                StackPane disc = buildBadge(FxUtil.image(ImageCatalog::foodLossOrange), FL_W, FL_H, BADGE_W,
                        "-" + p.gathererDiscount(), GATHERER_INK, DISCOUNT_FONT, FL_CX, FL_CY);
                StackPane.setAlignment(disc, Pos.TOP_RIGHT);
                cell.getChildren().add(disc);
            }
            case SHAMAN -> {
                // Top-right: total shaman stars.
                StackPane stars = buildBadge(FxUtil.image(ImageCatalog::shamanStars), SS_W, SS_H, BADGE_W,
                        String.valueOf(p.shamanStars()), SHAMAN_INK, BADGE_FONT, SS_CX, SS_CY);
                StackPane.setAlignment(stars, Pos.TOP_RIGHT);
                cell.getChildren().add(stars);
            }
            case INVENTOR -> {
                // Top-right: number of unique inventor icons (explained on hover).
                Node uniq = ChalkBadge.number(String.valueOf(p.inventorUniqueIcons()),
                        DECK_COUNT_FONT, Color.WHITE, Color.BLACK, DECK_COUNT_STROKE);
                StackPane.setAlignment(uniq, Pos.TOP_RIGHT);
                Tooltip.install(uniq, new Tooltip("Number of unique icons"));
                cell.getChildren().add(uniq);
            }
            default -> { }
        }

        // Bottom-right of every icon: how many cards of this type the player holds.
        int count = (p.cards() == null || p.cards().get(type) == null) ? 0 : p.cards().get(type).size();
        Node xn = ChalkBadge.number("x" + count, DECK_COUNT_FONT, Color.WHITE, Color.BLACK, DECK_COUNT_STROKE);
        StackPane.setAlignment(xn, Pos.BOTTOM_RIGHT);
        cell.getChildren().add(xn);

        // Clicking an icon drills into this player's cards of that type (toggle);
        // hovering lights it up white like a selected board card.
        cell.setCursor(Cursor.HAND);
        cell.setOnMouseEntered(_ -> cell.setEffect(FxUtil.selectedGlow()));
        cell.setOnMouseExited(_ -> cell.setEffect(null));
        cell.setOnMouseClicked(_ -> { openDeck.put(p.name(), type); rerender.run(); });

        return cell;
    }

    /**
     * A small corner extra-info badge: the {@code img} with {@code text} drawn
     * centred on the image's {@code (cx,cy)} reference pixel. Any per-type
     * recolouring is baked into {@code img} by the caller (see
     * {@link ImageCatalog#foodLossOrange()}).
     */
    private StackPane buildBadge(Image img, double nativeW, double nativeH, double dispW,
                                 String text, Color ink, double fontSize, double cx, double cy) {
        double dispH = dispW * nativeH / nativeW;
        StackPane sp = new StackPane();
        sp.setMinSize(dispW, dispH);
        sp.setPrefSize(dispW, dispH);
        sp.setMaxSize(dispW, dispH);
        sp.setMouseTransparent(true);
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(dispW);
            iv.setFitHeight(dispH);
            iv.setPreserveRatio(false);
            sp.getChildren().add(iv);
        }
        Node num = ChalkBadge.number(text, fontSize, ink, null, 0);
        StackPane.setAlignment(num, Pos.CENTER);
        num.setTranslateX((cx - nativeW / 2.0) * (dispW / nativeW));
        num.setTranslateY((cy - nativeH / 2.0) * (dispH / nativeH));
        sp.getChildren().add(num);
        return sp;
    }

    // ── Deck drill-down (icon toggle → that type's cards) ─────────────────────

    /** The card images for {@code p}'s cards of {@code type} (sorted), or a single
     *  "—" placeholder when the player holds none of that type. */
    private List<Node> deckCardNodes(PlayerDTO p, CardType type, double cw) {
        List<CardDTO> cards = new ArrayList<>();
        if (p.cards() != null && p.cards().get(type) != null) cards.addAll(p.cards().get(type));
        cards.sort(Comparator.comparing(CardDTO::id));
        List<Node> nodes = new ArrayList<>();
        if (cards.isEmpty()) {
            Label none = new Label("—");
            none.setFont(ImageCatalog.chalkFont(16));
            none.setStyle("-fx-text-fill: #f5deb3; -fx-padding: 6;");
            nodes.add(none);
        } else {
            for (CardDTO c : cards) nodes.add(buildDeckCard(c, cw));
        }
        return nodes;
    }

    /** Gold back button that closes the drill-down for {@code playerName}, at the
     *  given font size (the self panel uses a larger one). */
    private Button buildDeckBackButton(String playerName, int fontSize) {
        Button b = new Button("↩");
        b.setFocusTraversable(false);
        int padV = Math.round(fontSize * 0.3f);
        int padH = Math.round(fontSize * 0.7f);
        b.setStyle("-fx-font-size: " + fontSize + "px; -fx-padding: "
                + padV + " " + padH + " " + padV + " " + padH + "; -fx-background-radius: 8;");
        b.setOnAction(_ -> { openDeck.remove(playerName); rerender.run(); });
        return b;
    }

    /** A single rounded card image used in the drill-down view. */
    private Node buildDeckCard(CardDTO card, double w) {
        double h = w * BoardRenderer.CARD_ASPECT;
        StackPane cell = new StackPane();
        cell.setMinSize(w, h);
        cell.setPrefSize(w, h);
        cell.setMaxSize(w, h);
        ImageView img = FxUtil.imageView(() -> ImageCatalog.cardFront(card.id()));
        if (img != null) {
            img.setFitWidth(w);
            img.setFitHeight(h);
            img.setPreserveRatio(false);
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(w, h);
            clip.setArcWidth(w * 0.12);
            clip.setArcHeight(w * 0.12);
            img.setClip(clip);
            cell.getChildren().add(img);
        } else {
            Label fb = new Label(card.id());
            fb.setWrapText(true);
            fb.setStyle("-fx-text-fill: #f5deb3; -fx-background-color: #3a2410;"
                    + " -fx-background-radius: 6; -fx-padding: 4; -fx-font-size: 10px;");
            cell.getChildren().add(fb);
        }
        return cell;
    }
}

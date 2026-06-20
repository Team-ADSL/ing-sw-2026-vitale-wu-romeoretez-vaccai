package org.adsl.client.view.gui.game;

import static org.adsl.client.view.gui.GuiConstants.*;

import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.geometry.Insets;
import org.adsl.client.view.gui.FxUtil;
import org.adsl.client.view.gui.ImageCatalog;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Row;
import org.adsl.shared.enums.Totem;
import org.adsl.shared.model.CardDTO;
import org.adsl.shared.model.GameDTO;
import org.adsl.shared.model.OfferTileDTO;
import org.adsl.shared.model.OrderCellDTO;
import org.adsl.shared.model.OrderTileDTO;
import org.adsl.shared.utils.Move;

import java.util.List;
import java.util.Map;

/**
 * Renders the central board block of the game screen into the FXML containers it
 * is handed: the two card rows, the offer track, the turn-order tile, the
 * face-down deck and the future-era building decks. Owns all the board pixel
 * geometry (card/tile aspect ratios, order-tile cell centers, totem anchors).
 * Turn/selection queries and click handling are delegated to {@link Callbacks},
 * so the renderer stays free of game state and input policy.
 */
public final class BoardRenderer {

    // Layout constants are defined in GuiConstants.

    // Per-player-count cell Y fractions on the order tile (native-pixel / ORDER_TILE_H).
    private static final double[][] ORDER_CELL_Y = {
        { 298.0 / ORDER_TILE_H, 462.0 / ORDER_TILE_H },
        { 257.0 / ORDER_TILE_H, 422.0 / ORDER_TILE_H, 587.0 / ORDER_TILE_H },
        { 213.0 / ORDER_TILE_H, 379.0 / ORDER_TILE_H, 545.0 / ORDER_TILE_H, 711.0 / ORDER_TILE_H },
        { 148.0 / ORDER_TILE_H, 314.0 / ORDER_TILE_H, 479.0 / ORDER_TILE_H, 646.0 / ORDER_TILE_H, 811.0 / ORDER_TILE_H },
    };

    // Totem slot centers (fractional x,y of tile size) for each offer-tile sprite.
    private static final Map<String, double[]> OFFER_SLOT = Map.of(
        "offer_tile_a", new double[]{325.0 / OFFER_TILE_W, 280.0 / OFFER_TILE_H},
        "offer_tile_b", new double[]{325.0 / OFFER_TILE_W, 280.0 / OFFER_TILE_H},
        "offer_tile_c", new double[]{309.0 / OFFER_TILE_W, 281.0 / OFFER_TILE_H},
        "offer_tile_d", new double[]{302.0 / OFFER_TILE_W, 281.0 / OFFER_TILE_H},
        "offer_tile_e", new double[]{291.0 / OFFER_TILE_W, 280.0 / OFFER_TILE_H},
        "offer_tile_f", new double[]{281.0 / OFFER_TILE_W, 281.0 / OFFER_TILE_H},
        "offer_tile_g", new double[]{277.0 / OFFER_TILE_W, 281.0 / OFFER_TILE_H}
    );

    /** Game-state queries and input handling the renderer delegates back to the screen. */
    public interface Callbacks {
        boolean canPickRow(Row row);
        boolean canPlaceTotem();
        boolean isSelected(Move move);
        void onCardClicked(Row row, int idx, CardDTO card, StackPane pane);
        void onOfferTileClicked(int idx, OfferTileDTO tile);
    }

    private final HBox topRow;
    private final HBox bottomRow;
    private final HBox offerTrack;
    private final HBox buildingDecks;
    private final Pane orderTilePane;
    private final Pane deckPane;
    private final Callbacks cb;

    public BoardRenderer(HBox topRow, HBox bottomRow, HBox offerTrack, HBox buildingDecks,
                         Pane orderTilePane, Pane deckPane, Callbacks cb) {
        this.topRow = topRow;
        this.bottomRow = bottomRow;
        this.offerTrack = offerTrack;
        this.buildingDecks = buildingDecks;
        this.orderTilePane = orderTilePane;
        this.deckPane = deckPane;
        this.cb = cb;
    }

    /**
     * Renders the whole board for {@code game}. Every card/tile is drawn at a
     * fixed reference size; the screen then scales the whole block as one unit to
     * fit the window, so all elements resize together and nothing is clipped.
     */
    public void render(GameDTO game) {
        double cardW = CARD_MAX_W;
        double offW = TILE_MAX_W;

        renderRow(topRow, game.board().topRow(), Row.UPPER, cardW);
        renderRow(bottomRow, game.board().lowRow(), Row.LOWER, cardW);
        renderOfferTrack(game.board().offerTrack(), offW);
        renderDeckCard(game, offW * TILE_ASPECT);
        renderOrderTile(game, offW * TILE_ASPECT);
        renderBuildingDecks(game, offW * TILE_ASPECT);
    }

    private void renderRow(HBox container, List<CardDTO> cards, Row row, double cardW) {
        container.getChildren().clear();
        container.setSpacing(CARD_GAP);
        if (cards == null) return;
        boolean clickable = cb.canPickRow(row);
        int idx = 0;
        for (CardDTO c : cards) {
            if (c == null) { idx++; continue; }
            container.getChildren().add(buildCardCell(c, row, idx, clickable, cardW));
            idx++;
        }
    }

    private Node buildCardCell(CardDTO card, Row row, int idx, boolean clickable, double cardW) {
        double cardH = cardW * CARD_ASPECT;
        StackPane cell = new StackPane();
        cell.setPrefSize(cardW, cardH);
        cell.setMinSize(cardW, cardH);
        cell.setMaxSize(cardW, cardH);
        cell.setAlignment(javafx.geometry.Pos.CENTER);

        ImageView img = FxUtil.imageView(() -> ImageCatalog.cardFront(card.id()));
        if (img != null) {
            img.setFitWidth(cardW);
            img.setFitHeight(cardH);
            img.setPreserveRatio(false);
            Rectangle clip = new Rectangle(cardW, cardH);
            clip.setArcWidth(cardW * 0.12);
            clip.setArcHeight(cardW * 0.12);
            img.setClip(clip);
            cell.getChildren().add(img);
        } else {
            Label fallback = new Label(card.id());
            fallback.setWrapText(true);
            fallback.setStyle("-fx-text-fill: #f5deb3; -fx-background-color: #3a2410;"
                    + " -fx-background-radius: 8; -fx-padding: 6;");
            cell.getChildren().add(fallback);
        }

        boolean selected = cb.isSelected(new Move(idx, row));
        if (selected) {
            cell.setEffect(FxUtil.selectedGlow());
        }

        if (clickable) {
            cell.setCursor(Cursor.HAND);
            cell.setOnMouseEntered(_ -> {
                if (!cb.isSelected(new Move(idx, row))) cell.setEffect(FxUtil.selectedGlow());
            });
            cell.setOnMouseExited(_ -> {
                if (!cb.isSelected(new Move(idx, row))) cell.setEffect(null);
            });
            cell.setOnMouseClicked(_ -> cb.onCardClicked(row, idx, card, cell));
        } else {
            cell.setEffect(FxUtil.darken());
        }
        return cell;
    }

    private void renderOfferTrack(List<OfferTileDTO> tiles, double tileW) {
        offerTrack.getChildren().clear();
        if (tiles == null) return;
        boolean clickable = cb.canPlaceTotem();
        int idx = 0;
        for (OfferTileDTO t : tiles) {
            offerTrack.getChildren().add(buildOfferTileCell(t, idx, clickable, tileW));
            idx++;
        }
    }

    private Node buildOfferTileCell(OfferTileDTO tile, int idx, boolean clickable, double tileW) {
        double tileH = tileW * TILE_ASPECT;
        Pane cell = new Pane();
        cell.setPrefSize(tileW, tileH);
        cell.setMinSize(tileW, tileH);
        cell.setMaxSize(tileW, tileH);

        ImageView img = FxUtil.imageView(() -> ImageCatalog.offerTile(tile.id()));
        if (img != null) {
            img.setFitWidth(tileW);
            img.setFitHeight(tileH);
            img.setPreserveRatio(false);
            cell.getChildren().add(img);
        } else {
            Label fallback = new Label(tile.id());
            fallback.setStyle("-fx-text-fill: #f5deb3;");
            fallback.setLayoutX(4);
            fallback.setLayoutY(4);
            cell.getChildren().add(fallback);
        }

        if (tile.totem() != null) {
            ImageView totem = FxUtil.imageView(() -> ImageCatalog.totem3D(tile.totem()));
            if (totem != null) {
                double totemW = tileW * OFFER_TOTEM_W_FRAC;
                double totemH = totemW * (TOTEM3D_H / TOTEM3D_W);
                double[] slot = OFFER_SLOT.getOrDefault(tile.id(), new double[]{0.5, 0.28});
                double cx = slot[0] * tileW;
                double cy = slot[1] * tileH;
                totem.setFitWidth(totemW);
                totem.setPreserveRatio(true);
                totem.setLayoutX(cx - TOTEM_ANCHOR_X * totemW);
                totem.setLayoutY(cy - TOTEM_ANCHOR_Y * totemH);
                cell.getChildren().add(totem);
            }
        }

        boolean occupied = tile.totem() != null;
        if (clickable && !occupied) {
            double rectW = tileW * (217.0 / OFFER_TILE_W);
            double rectH = tileH * (121.0 / OFFER_TILE_H);
            double[] slot = OFFER_SLOT.getOrDefault(tile.id(), new double[]{0.5, 0.28});
            double cx = slot[0] * tileW;
            double cy = slot[1] * tileH;

            Rectangle hoverRect = new Rectangle(rectW, rectH);
            hoverRect.setX(cx - rectW / 2);
            hoverRect.setY(cy - rectH / 2);
            hoverRect.setFill(Color.web("#F2B035", 0.45));
            hoverRect.setStroke(Color.web("#F2B035"));
            hoverRect.setStrokeWidth(2.5);
            hoverRect.setArcWidth(8);
            hoverRect.setArcHeight(8);
            hoverRect.setVisible(false);
            hoverRect.setMouseTransparent(true);

            cell.getChildren().add(hoverRect);

            cell.setCursor(Cursor.HAND);
            cell.setOnMouseClicked(_ -> cb.onOfferTileClicked(idx, tile));
            cell.setOnMouseEntered(_ -> hoverRect.setVisible(true));
            cell.setOnMouseExited(_ -> hoverRect.setVisible(false));
        } else if (!clickable) {
            cell.setEffect(FxUtil.darken());
        }
        return cell;
    }

    /**
     * Renders the turn-order tile to the left of the offer track. Picks the
     * {@code order_tile_<n>p} sprite for the current player count, sizes it to
     * the offer-tile height ({@code tileH}) so it sits in line with the rest of
     * the board, then drops each occupied cell's 3D totem so its support point
     * rests on the cell center.
     */
    private void renderOrderTile(GameDTO game, double tileH) {
        if (orderTilePane == null) return;
        orderTilePane.getChildren().clear();

        OrderTileDTO ot = (game != null && game.board() != null) ? game.board().orderTile() : null;
        int n = (game != null && game.players() != null) ? game.players().size() : 0;
        if (ot == null || n < 2 || n > 5 || tileH <= 0) {
            sizeOrderTile(0, 0);
            return;
        }

        double tileW = tileH * (ORDER_TILE_W / ORDER_TILE_H);
        sizeOrderTile(tileW, tileH);

        ImageView img = FxUtil.imageView(() -> ImageCatalog.orderTile(n));
        if (img != null) {
            img.setFitWidth(tileW);
            img.setFitHeight(tileH);
            img.setPreserveRatio(false);
            orderTilePane.getChildren().add(img);
        }

        List<OrderCellDTO> cells = ot.cells();
        if (cells == null) return;
        double[] ys = ORDER_CELL_Y[n - 2];

        double rectW = tileW * (186.0 / ORDER_TILE_W);
        double rectH = tileH * (96.0 / ORDER_TILE_H);
        double arc   = rectW * 0.10;

        for (int i = 0; i < cells.size() && i < ys.length; i++) {
            Totem totem = cells.get(i).totem();
            if (totem == null) continue;
            double cx = ORDER_CELL_X * tileW;
            double cy = ys[i] * tileH;
            Rectangle rect = new Rectangle(rectW, rectH);
            rect.setArcWidth(arc);
            rect.setArcHeight(arc);
            rect.setFill(totemFill(totem));
            rect.setStroke(Color.web("#000000", 0.75));
            rect.setStrokeWidth(1.0);
            rect.setEffect(new DropShadow(5, 0, 2, Color.web("#000000", 0.55)));
            rect.setLayoutX(cx - rectW / 2.0);
            rect.setLayoutY(cy - rectH / 2.0);
            orderTilePane.getChildren().add(rect);
        }

        // Collect the colored rectangles (not the background ImageView) so hover
        // can fade them out to reveal the order-tile content underneath.
        List<Rectangle> totemRects = orderTilePane.getChildren().stream()
                .filter(c -> c instanceof Rectangle)
                .map(c -> (Rectangle) c)
                .toList();
        if (!totemRects.isEmpty()) {
            orderTilePane.setOnMouseEntered(_ -> totemRects.forEach(r -> r.setOpacity(0.15)));
            orderTilePane.setOnMouseExited(_  -> totemRects.forEach(r -> r.setOpacity(1.0)));
        }
    }

    private void sizeOrderTile(double w, double h) {
        orderTilePane.setMinSize(w, h);
        orderTilePane.setPrefSize(w, h);
        orderTilePane.setMaxSize(w, h);
    }

    /**
     * Renders the face-down future-era building decks to the right of the offer
     * track. {@code remainingBuildings} is a shrinking queue (index 0 is the next
     * era to enter), so the era for a slot is {@code (4 - size) + index}. Only
     * deck presence is known client-side, so a single card-back with a stacked
     * shadow stands in for the pile, sized to the offer-tile height.
     */
    private void renderBuildingDecks(GameDTO game, double tileH) {
        buildingDecks.getChildren().clear();
        if (game == null || game.board() == null || tileH <= 0) return;

        List<Boolean> remaining = game.board().remainingBuildings();
        if (remaining == null || remaining.isEmpty()) return;

        // Keep the first deck clearly detached from the offer track.
        HBox.setMargin(buildingDecks, new Insets(0, 0, 0, 18));

        int size = remaining.size();
        double deckW = tileH / CARD_ASPECT;
        for (int i = 0; i < size; i++) {
            if (!Boolean.TRUE.equals(remaining.get(i))) continue;
            final int era = (4 - size) + i;
            if (era < 2 || era > 3) continue;
            ImageView back = FxUtil.imageView(() -> ImageCatalog.cardBack(CardType.BUILDINGS, era, false));
            if (back == null) continue;
            back.setFitWidth(deckW);
            back.setFitHeight(tileH);
            back.setPreserveRatio(false);
            // Round the corners like the board cards (arc = 12% of width).
            Rectangle clip = new Rectangle(deckW, tileH);
            clip.setArcWidth(deckW * 0.12);
            clip.setArcHeight(deckW * 0.12);
            back.setClip(clip);
            // Wrap so the pile shadow survives the clip.
            StackPane deckCell = new StackPane(back);
            deckCell.setEffect(FxUtil.deckShadow());
            buildingDecks.getChildren().add(deckCell);
        }
    }

    private void renderDeckCard(GameDTO game, double tileH) {
        if (deckPane == null) return;
        deckPane.getChildren().clear();
        boolean empty = game == null || game.board() == null || game.board().isDeckEmpty();
        int era = (game != null) ? game.era() : 0;
        if (empty || era < 1 || era > 3 || tileH <= 0) {
            deckPane.setMinSize(0, 0);
            deckPane.setPrefSize(0, 0);
            deckPane.setMaxSize(0, 0);
            return;
        }
        double deckW = tileH / CARD_ASPECT;
        deckPane.setMinSize(deckW, tileH);
        deckPane.setPrefSize(deckW, tileH);
        deckPane.setMaxSize(deckW, tileH);
        ImageView back = FxUtil.imageView(() -> ImageCatalog.deckCardBack(era));
        if (back == null) return;
        back.setFitWidth(deckW);
        back.setFitHeight(tileH);
        back.setPreserveRatio(false);
        Rectangle clip = new Rectangle(deckW, tileH);
        clip.setArcWidth(deckW * 0.12);
        clip.setArcHeight(deckW * 0.12);
        back.setClip(clip);
        StackPane deckCell = new StackPane(back);
        deckCell.setEffect(FxUtil.deckShadow());
        deckPane.getChildren().add(deckCell);
    }

    private static Color totemFill(Totem totem) {
        return switch (totem) {
            case RED    -> Color.web("#ec6645");
            case WHITE  -> Color.web("#f7f1f0");
            case BLACK  -> Color.web("#421528");
            case BLUE   -> Color.web("#2391ae");
            case YELLOW -> Color.web("#f6c81f");
        };
    }
}

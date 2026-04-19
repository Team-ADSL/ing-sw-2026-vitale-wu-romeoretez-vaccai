package org.adsl.client.view.tui.screens;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import org.adsl.client.local.LocalGameCoordinator;
import org.adsl.client.view.tui.CardCatalog;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Phase;
import org.adsl.shared.enums.Row;
import org.adsl.shared.enums.Totem;
import org.adsl.shared.model.*;
import org.adsl.shared.utils.Move;

import java.io.IOException;
import java.util.*;

/**
 * Renders the game board and handles player input for the local hotseat mode.
 *
 * Visual layout (120+ columns, 35+ rows):
 *
 *   ══ HEADER: round/era/phase/current player ═══════════════════════════════
 *
 *   ── OFFER TRACK ──────────────────────────────────────────────────────────
 *   [ TileB  ]  [ TileC  ]  [ TileE  ]  [ TileF  ]  ...
 *   LOW:1       UP:2        UP:1 LO:1   UP:2
 *   (RED)       (---)       (BLUE)      (---)
 *
 *   ── TOP ROW ──────────────────────────────────────────────── BUILDINGS ───
 *   ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐     [I ▣] [II ▣] [III ▣]
 *   │HUNTER  │  │ARTIST  │  │SHAMAN  │  │HUNT evt│
 *   │hunter_1│  │artist_1│  │sham_02 │  │hunt_01 │
 *   └────────┘  └────────┘  └────────┘  └────────┘
 *
 *   ── BOTTOM ROW ───────────────────────────────────────────────────────────
 *   ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐
 *   │INVENTOR│  │GATHERER│  │BUILDER │  │SUSTAIN.│
 *   │inv_01  │  │gath_01 │  │build_01│  │sust_01 │
 *   └────────┘  └────────┘  └────────┘  └────────┘
 *
 *   ── YOUR TRIBE: ALICE [RED]  Food: 5  PP: 12 ─────────────────────────────
 *   H:2  G:1  S:1★★  Bu:1(-1F,3PP)  In:3(LEATHR,BOAT,SPEAR)  Ar:2  Bl:1
 *
 *   ── OTHERS ───────────────────────────────────────────────────────────────
 *   BOB   [BLUE]  F:3  PP:8  | H:1 G:2 S:0 Bu:0 In:1 Ar:2 Bl:0
 *
 *   ── TURN ORDER  ──  [↑↓/←→] Navigate  [SPACE] Select  [ENTER] Confirm ───
 */
public class GameScreen {
    private static final int CARD_W = 10;  // card box inner width
    private static final int CARD_H = 4;   // card box inner height (lines)
    private static final int CARD_GAP = 2; // horizontal gap between cards

    private final Screen screen;
    private final LocalGameCoordinator coordinator;

    // Selection state
    private int selectionIndex = 0;
    private final Set<Move> selectedMoves = new LinkedHashSet<>();
    private int requiredMoves = 0;         // how many cards/tiles must be picked

    public GameScreen(Screen screen, LocalGameCoordinator coordinator) {
        this.screen = screen;
        this.coordinator = coordinator;
    }

    // ── Public entry point ────────────────────────────────────────────────────

    /**
     * Shows a full-screen "PASS THE KEYBOARD" overlay and waits for ENTER.
     * Called each time the current player changes.
     */
    public void showPassScreen(String playerName, Totem totem) {
        try {
            screen.clear();
            TextGraphics tg = screen.newTextGraphics();
            TerminalSize sz = screen.getTerminalSize();
            int midRow = sz.getRows() / 2;
            int cols = sz.getColumns();

            tg.setForegroundColor(totemColor(totem));
            tg.setBackgroundColor(TextColor.ANSI.BLACK);
            putCentered(tg, midRow - 3, cols, "────────────────────────────────────");
            putCentered(tg, midRow - 2, cols, "  Pass the keyboard to:  ");
            putCentered(tg, midRow - 1, cols, "");
            putCentered(tg, midRow,     cols, "  " + playerName.toUpperCase() + "  (" + CardCatalog.totemLabel(totem) + " TRIBE)  ");
            putCentered(tg, midRow + 1, cols, "");
            tg.setForegroundColor(TextColor.ANSI.WHITE);
            putCentered(tg, midRow + 2, cols, "  Press ENTER to continue...  ");
            putCentered(tg, midRow + 3, cols, "────────────────────────────────────");
            screen.refresh();

            // Wait for ENTER
            KeyStroke key;
            do {
                key = screen.readInput();
            } while (key == null || key.getKeyType() != KeyType.Enter);

        } catch (IOException e) {
            System.err.println("Error rendering pass screen: " + e.getMessage());
        }
    }

    /**
     * Renders the static game state (non-interactive, for automatic phases).
     */
    public void renderStatic(GameDTO game) {
        try {
            screen.clear();
            TextGraphics tg = screen.newTextGraphics();
            drawBoard(tg, game, -1, Collections.emptySet(), screen.getTerminalSize());
            screen.refresh();
        } catch (IOException e) {
            System.err.println("Error rendering game: " + e.getMessage());
        }
    }

    /**
     * Interactive offer-tile selection for TOTEM_PLACEMENT.
     * Returns a singleton Set<Move> with the chosen tile index.
     */
    public Set<Move> collectTotemPlacement(GameDTO game) {
        selectionIndex = 0;
        List<OfferTileDTO> tiles = game.board().offerTrack();
        int tileCount = tiles.size();

        while (true) {
            try {
                screen.clear();
                TextGraphics tg = screen.newTextGraphics();
                TerminalSize sz = screen.getTerminalSize();
                drawBoard(tg, game, selectionIndex, Collections.emptySet(), sz);
                drawControls(tg, sz, "← → Navigate offer tiles   ENTER Confirm placement");
                screen.refresh();

                KeyStroke key = screen.readInput();
                if (key == null) continue;

                switch (key.getKeyType()) {
                    case ArrowLeft  -> selectionIndex = Math.max(0, selectionIndex - 1);
                    case ArrowRight -> selectionIndex = Math.min(tileCount - 1, selectionIndex + 1);
                    case Enter -> {
                        // Cannot place on a tile that already has a player
                        OfferTileDTO tile = tiles.get(selectionIndex);
                        if (tile.totem() != null) {
                            flashError(tg, sz, "That tile is already occupied!");
                            continue;
                        }
                        return Set.of(new Move(selectionIndex, Row.OFFER));
                    }
                    default -> {}
                }
            } catch (IOException e) {
                System.err.println("Input error: " + e.getMessage());
            }
        }
    }

    /**
     * Interactive card selection for ACTION_EXECUTION.
     * The player selects exactly {@code upperCount} cards from the top row
     * and {@code lowerCount} from the bottom row.
     */
    public Set<Move> collectCardSelection(GameDTO game, int upperCount, int lowerCount) {
        selectedMoves.clear();
        requiredMoves = upperCount + lowerCount;
        selectionIndex = 0;
        boolean onTopRow = true; // which row cursor is in

        List<CardDTO> topRow    = game.board().topRow();
        List<CardDTO> bottomRow = game.board().lowRow();

        while (true) {
            try {
                screen.clear();
                TextGraphics tg = screen.newTextGraphics();
                TerminalSize sz = screen.getTerminalSize();

                // Encode selection state for drawing
                int cursorTop    = onTopRow ? selectionIndex : -1;
                int cursorBottom = onTopRow ? -1 : selectionIndex;
                drawBoard(tg, game, cursorTop, selectedMoves, sz);
                // Highlight bottom row cursor separately
                if (!onTopRow) {
                    highlightCard(tg, selectionIndex, /* bottomRowStartCol */ 4,
                            /* bottomRowY */ getBottomRowY(sz), true);
                }
                drawControls(tg, sz, String.format(
                        "← → Navigate   ↑ ↓ Switch rows   SPACE Select (%d/%d)   ENTER Confirm",
                        selectedMoves.size(), requiredMoves));
                screen.refresh();

                KeyStroke key = screen.readInput();
                if (key == null) continue;

                List<CardDTO> activeRow = onTopRow ? topRow : bottomRow;
                int maxIndex = activeRow.size() - 1;

                switch (key.getKeyType()) {
                    case ArrowLeft  -> selectionIndex = Math.max(0, selectionIndex - 1);
                    case ArrowRight -> selectionIndex = Math.min(maxIndex, selectionIndex + 1);
                    case ArrowUp    -> { onTopRow = true;  selectionIndex = Math.min(selectionIndex, topRow.size() - 1); }
                    case ArrowDown  -> { onTopRow = false; selectionIndex = Math.min(selectionIndex, bottomRow.size() - 1); }
                    case Character -> {
                        if (key.getCharacter() == ' ') {
                            List<CardDTO> currentRow = onTopRow ? topRow : bottomRow;
                            if (selectionIndex < currentRow.size() && currentRow.get(selectionIndex) == null) {
                                flashError(tg, sz, "That slot is empty.");
                            } else {
                                toggleCardSelection(selectionIndex, onTopRow ? Row.UPPER : Row.LOWER,
                                        upperCount, lowerCount, tg, sz);
                            }
                        }
                    }
                    case Enter -> {
                        if (selectedMoves.size() != requiredMoves) {
                            flashError(tg, sz, String.format(
                                    "Select exactly %d card(s): %d from top, %d from bottom.",
                                    requiredMoves, upperCount, lowerCount));
                            continue;
                        }
                        // Validate row counts
                        long selTop = selectedMoves.stream().filter(m -> m.row() == Row.UPPER).count();
                        long selBot = selectedMoves.stream().filter(m -> m.row() == Row.LOWER).count();
                        if (selTop != upperCount || selBot != lowerCount) {
                            flashError(tg, sz, String.format(
                                    "Need %d from top row, %d from bottom row.", upperCount, lowerCount));
                            continue;
                        }
                        return Collections.unmodifiableSet(new LinkedHashSet<>(selectedMoves));
                    }
                    default -> {}
                }
            } catch (IOException e) {
                System.err.println("Input error: " + e.getMessage());
            }
        }
    }

    // ── Board rendering ───────────────────────────────────────────────────────

    private void drawBoard(TextGraphics tg, GameDTO game, int cursorOfferIndex,
                           Set<Move> highlights, TerminalSize sz) {
        int cols = sz.getColumns();
        tg.setForegroundColor(TextColor.ANSI.WHITE);
        tg.setBackgroundColor(TextColor.ANSI.BLACK);

        // Row 0: header
        drawHeader(tg, game, cols);

        // Row 2: offer track
        drawSectionLabel(tg, 2, cols, "OFFER TRACK");
        drawOfferTrack(tg, game.board().offerTrack(), 3, cursorOfferIndex, cols);

        // Row 8: top row
        int topRowY = 8;
        drawSectionLabel(tg, topRowY, cols, "TOP ROW");
        drawCardRow(tg, game.board().topRow(), topRowY + 1, highlights, Row.UPPER, cols);

        // Row 14: bottom row
        int botRowY = 14;
        drawSectionLabel(tg, botRowY, cols, "BOTTOM ROW");
        drawCardRow(tg, game.board().lowRow(), botRowY + 1, highlights, Row.LOWER, cols);

        // Building decks indicator (right side, next to top row)
        drawBuildingDecks(tg, game.board().remainingBuildings(), topRowY + 1, cols);

        // Row 20: current player's tribe
        int tribeY = 20;
        drawCurrentPlayerTribe(tg, game, tribeY, cols);

        // Row 24: other players
        drawOtherPlayers(tg, game, 24, cols);

        // Row 29: turn order
        drawTurnOrder(tg, game, 29, cols);
    }

    private void drawHeader(TextGraphics tg, GameDTO game, int cols) {
        tg.setForegroundColor(TextColor.ANSI.BLACK);
        tg.setBackgroundColor(TextColor.ANSI.YELLOW);
        String header = String.format("  MESOS  │  Round %d/10  │  Era %d  │  Phase: %-18s │  Current: %s  ",
                game.round(), game.era(),
                phaseLabel(game.phase()),
                currentPlayerLabel(game));
        tg.putString(0, 0, header);
        // fill rest of row
        if (header.length() < cols) {
            tg.putString(header.length(), 0, " ".repeat(cols - header.length()));
        }
        tg.setForegroundColor(TextColor.ANSI.WHITE);
        tg.setBackgroundColor(TextColor.ANSI.BLACK);
    }

    private void drawSectionLabel(TextGraphics tg, int row, int cols, String label) {
        tg.setForegroundColor(TextColor.ANSI.CYAN);
        String line = " ── " + label + " " + "─".repeat(Math.max(0, cols - label.length() - 6));
        tg.putString(0, row, line.substring(0, Math.min(line.length(), cols)));
        tg.setForegroundColor(TextColor.ANSI.WHITE);
    }

    private void drawOfferTrack(TextGraphics tg, List<OfferTileDTO> tiles, int startRow,
                                int cursorIndex, int cols) {
        int col = 2;
        for (int i = 0; i < tiles.size(); i++) {
            OfferTileDTO tile = tiles.get(i);
            boolean selected = (i == cursorIndex);

            TextColor boxColor = selected ? TextColor.ANSI.YELLOW : TextColor.ANSI.WHITE;
            tg.setForegroundColor(boxColor);

            // Tile header: [ID]
            String idLabel = tile.id() != null ? tile.id().replace("offer_tile_", "") : "?";
            tg.putString(col, startRow,     "┌──────────┐");
            tg.putString(col, startRow + 1, "│ [" + padRight(idLabel.toUpperCase(), 7) + "] │");
            // Player on tile
            String playerLabel = (tile.totem() != null) ? "(" + CardCatalog.totemLabel(tile.totem()) + ")" : "(empty)";
            tg.putString(col, startRow + 2, "│ " + padRight(playerLabel, 9) + " │");
            tg.putString(col, startRow + 3, "└──────────┘");
            tg.setForegroundColor(TextColor.ANSI.WHITE);

            col += 14;
            if (col + 14 > cols) break;
        }
    }

    private void drawCardRow(TextGraphics tg, List<CardDTO> cards, int startRow,
                             Set<Move> highlights, Row rowType, int cols) {
        int col = 2;
        for (int i = 0; i < cards.size(); i++) {
            CardDTO card = cards.get(i);

            if (card == null) {
                // Empty slot: draw a faint placeholder
                tg.setForegroundColor(TextColor.ANSI.BLACK);
                tg.putString(col, startRow,     "┌────────┐");
                tg.putString(col, startRow + 1, "│        │");
                tg.putString(col, startRow + 2, "│  empty │");
                tg.putString(col, startRow + 3, "└────────┘");
                tg.setForegroundColor(TextColor.ANSI.WHITE);
                col += 11;
                if (col + 11 > cols - 20) break;
                continue;
            }

            boolean highlighted = highlights.contains(new Move(i, rowType));
            TextColor boxColor = highlighted ? TextColor.ANSI.GREEN : TextColor.ANSI.WHITE;
            tg.setForegroundColor(boxColor);

            CardType type = CardCatalog.typeFromId(card.id());
            String typeStr  = padRight(CardCatalog.typeLabel(type), 8);
            String idStr    = padRight(card.id().length() > 8 ? card.id().substring(0, 8) : card.id(), 8);
            boolean isEvent = CardCatalog.isEvent(type);

            tg.putString(col, startRow,     "┌────────┐");
            if (isEvent) tg.setForegroundColor(TextColor.ANSI.MAGENTA);
            tg.putString(col, startRow + 1, "│" + typeStr + "│");
            tg.putString(col, startRow + 2, "│" + idStr   + "│");
            tg.setForegroundColor(boxColor);
            tg.putString(col, startRow + 3, "└────────┘");
            tg.setForegroundColor(TextColor.ANSI.WHITE);

            col += 11;
            if (col + 11 > cols - 20) break;
        }
    }

    private void drawBuildingDecks(TextGraphics tg, List<Boolean> decks, int startRow, int cols) {
        int col = cols - 18;
        tg.setForegroundColor(TextColor.ANSI.YELLOW);
        tg.putString(col, startRow, "BUILDINGS:");
        String[] eras = {"Era I", "Era II", "Era III"};
        for (int i = 0; i < Math.min(decks.size(), 3); i++) {
            String label = eras[i] + (decks.get(i) ? " [▣]" : " [ ]");
            tg.putString(col, startRow + 1 + i, label);
        }
        tg.setForegroundColor(TextColor.ANSI.WHITE);
    }

    private void drawCurrentPlayerTribe(TextGraphics tg, GameDTO game, int startRow, int cols) {
        // Find the current player's DTO (match by totem)
        Totem currentTotem = game.currentPlayerTotem();
        PlayerDTO me = game.players().stream()
                .filter(p -> p.totem() == currentTotem)
                .findFirst()
                .orElse(null);

        if (me == null && !game.players().isEmpty()) {
            me = game.players().iterator().next();
        }
        if (me == null) return;

        tg.setForegroundColor(totemColor(me.totem()));
        String header = String.format(" ── YOUR TRIBE: %s [%s]  Food: %d  PP: %d %s",
                me.name(), CardCatalog.totemLabel(me.totem()), me.food(), me.pp(),
                "─".repeat(Math.max(0, cols - 60)));
        tg.putString(0, startRow, header.substring(0, Math.min(header.length(), cols)));
        tg.setForegroundColor(TextColor.ANSI.WHITE);

        // Compact card counts
        StringBuilder sb = new StringBuilder("  ");
        for (CardType type : CardType.values()) {
            Set<CardDTO> cardSet = me.cards().get(type);
            if (cardSet != null && !cardSet.isEmpty()) {
                sb.append(CardCatalog.typeSymbol(type)).append(":").append(cardSet.size()).append("  ");
            }
        }
        tg.putString(0, startRow + 1, sb.toString());
    }

    private void drawOtherPlayers(TextGraphics tg, GameDTO game, int startRow, int cols) {
        tg.setForegroundColor(TextColor.ANSI.CYAN);
        tg.putString(0, startRow - 1, " ── OTHERS " + "─".repeat(Math.max(0, cols - 10)));
        tg.setForegroundColor(TextColor.ANSI.WHITE);

        Totem currentTotem = game.currentPlayerTotem();
        int row = startRow;
        for (PlayerDTO p : game.players()) {
            if (p.totem() == currentTotem) continue; // skip current player
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("  %-12s [%-6s] F:%-3d PP:%-4d | ",
                    p.name(), CardCatalog.totemLabel(p.totem()), p.food(), p.pp()));
            for (CardType type : CardType.values()) {
                Set<CardDTO> cards = p.cards().get(type);
                if (cards != null && !cards.isEmpty()) {
                    sb.append(CardCatalog.typeSymbol(type)).append(":").append(cards.size()).append(" ");
                }
            }
            tg.setForegroundColor(totemColor(p.totem()));
            tg.putString(0, row, sb.toString().substring(0, Math.min(sb.length(), cols)));
            tg.setForegroundColor(TextColor.ANSI.WHITE);
            row++;
        }
    }

    private void drawTurnOrder(TextGraphics tg, GameDTO game, int startRow, int cols) {
        tg.setForegroundColor(TextColor.ANSI.CYAN);
        StringBuilder sb = new StringBuilder(" ── Turn Order: ");
        List<Totem> order = (game.board().orderTile() != null)
                ? game.board().orderTile().totems()
                : Collections.emptyList();
        for (int i = 0; i < order.size(); i++) {
            sb.append(CardCatalog.totemLabel(order.get(i)));
            if (i < order.size() - 1) sb.append(" → ");
        }
        tg.putString(0, startRow, sb.toString().substring(0, Math.min(sb.length(), cols)));
        tg.setForegroundColor(TextColor.ANSI.WHITE);
    }

    private void drawControls(TextGraphics tg, TerminalSize sz, String hint) {
        int row = sz.getRows() - 1;
        tg.setForegroundColor(TextColor.ANSI.BLACK);
        tg.setBackgroundColor(TextColor.ANSI.WHITE);
        String line = "  " + hint;
        tg.putString(0, row, line + " ".repeat(Math.max(0, sz.getColumns() - line.length())));
        tg.setForegroundColor(TextColor.ANSI.WHITE);
        tg.setBackgroundColor(TextColor.ANSI.BLACK);
    }

    private void flashError(TextGraphics tg, TerminalSize sz, String msg) {
        tg.setForegroundColor(TextColor.ANSI.RED);
        tg.putString(2, sz.getRows() - 2, "! " + msg + " ".repeat(Math.max(0, sz.getColumns() - msg.length() - 4)));
        tg.setForegroundColor(TextColor.ANSI.WHITE);
        try {
            screen.refresh();
            Thread.sleep(1800);
        } catch (IOException | InterruptedException ignored) {}
    }

    // ── Selection logic ───────────────────────────────────────────────────────

    private void toggleCardSelection(int index, Row row, int maxUpper, int maxLower,
                                     TextGraphics tg, TerminalSize sz) {
        Move move = new Move(index, row);
        if (selectedMoves.contains(move)) {
            selectedMoves.remove(move);
        } else {
            long countRow = selectedMoves.stream().filter(m -> m.row() == row).count();
            int limit = (row == Row.UPPER) ? maxUpper : maxLower;
            if (countRow >= limit) {
                flashError(tg, sz, "You can only pick " + limit + " card(s) from this row.");
                return;
            }
            selectedMoves.add(move);
        }
    }

    private void highlightCard(TextGraphics tg, int index, int startCol, int rowY, boolean active) {
        int col = startCol + index * 11;
        tg.setForegroundColor(active ? TextColor.ANSI.YELLOW : TextColor.ANSI.WHITE);
        tg.putString(col, rowY, "▲");
        tg.setForegroundColor(TextColor.ANSI.WHITE);
    }

    // ── Coordinate helpers ────────────────────────────────────────────────────

    private int getBottomRowY(TerminalSize sz) {
        return 15; // matches drawBoard layout
    }

    // ── Display helpers ───────────────────────────────────────────────────────

    private String phaseLabel(Phase phase) {
        if (phase == null) return "---";
        return switch (phase) {
            case TOTEM_PLACEMENT   -> "PLACE TOTEM";
            case ACTION_EXECUTION  -> "PICK CARDS";
            case EXTRA_MOVE        -> "EXTRA PICK";
            case EVENTS_EXECUTION  -> "EVENTS";
            case END_ROUND         -> "END OF ROUND";
            case END_GAME          -> "GAME OVER";
        };
    }

    private String currentPlayerLabel(GameDTO game) {
        Totem t = game.currentPlayerTotem();
        if (t == null) return "---";
        String name = coordinator.getTotemToName().getOrDefault(t, "?");
        return name + " [" + CardCatalog.totemLabel(t) + "]";
    }

    private TextColor totemColor(Totem totem) {
        if (totem == null) return TextColor.ANSI.WHITE;
        return switch (totem) {
            case RED    -> TextColor.ANSI.RED;
            case BLUE   -> TextColor.ANSI.CYAN;
            case WHITE  -> TextColor.ANSI.WHITE;
            case BLACK  -> TextColor.ANSI.WHITE;
            case YELLOW -> TextColor.ANSI.YELLOW;
        };
    }

    private String padRight(String s, int len) {
        if (s == null) s = "";
        if (s.length() >= len) return s.substring(0, len);
        return s + " ".repeat(len - s.length());
    }

    private void putCentered(TextGraphics tg, int row, int cols, String text) {
        int col = Math.max(0, (cols - text.length()) / 2);
        tg.putString(col, row, text);
    }
}

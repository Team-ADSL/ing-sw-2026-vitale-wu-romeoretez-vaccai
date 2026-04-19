package org.adsl.client.view.tui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.adsl.shared.enums.Row;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads offer_tiles.json once at startup and provides move-count lookups by tile ID.
 * Tile IDs are unique across the file, so no player-count filtering is needed.
 */
public final class OfferTileCatalog {
    private static final Map<String, int[]> MOVES = new HashMap<>(); // id -> [upper, lower]

    static {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = OfferTileCatalog.class.getClassLoader()
                    .getResourceAsStream("offer_tiles.json");
            if (is != null) {
                JsonNode root = mapper.readTree(is);
                for (JsonNode node : root) {
                    String id = node.get("id").asText();
                    JsonNode movesNode = node.get("moves");
                    int upper = movesNode.has(Row.UPPER.name()) ? movesNode.get(Row.UPPER.name()).asInt() : 0;
                    int lower = movesNode.has(Row.LOWER.name()) ? movesNode.get(Row.LOWER.name()).asInt() : 0;
                    MOVES.put(id, new int[]{upper, lower});
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: could not load offer_tiles.json – " + e.getMessage());
        }
    }

    private OfferTileCatalog() {}

    /** Total number of card draws allowed by this offer tile. */
    public static int totalMoves(String tileId) {
        int[] m = MOVES.get(tileId);
        return (m != null) ? m[0] + m[1] : 1;
    }

    /** Number of cards drawn from the top row. */
    public static int upperMoves(String tileId) {
        int[] m = MOVES.get(tileId);
        return (m != null) ? m[0] : 0;
    }

    /** Number of cards drawn from the bottom row. */
    public static int lowerMoves(String tileId) {
        int[] m = MOVES.get(tileId);
        return (m != null) ? m[1] : 1;
    }
}

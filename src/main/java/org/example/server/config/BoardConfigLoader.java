package org.example.server.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.server.model.cards.Card;
import org.example.server.model.cards.buildings.*;
import org.example.server.model.cards.buildings.forEvent.*;
import org.example.server.model.cards.buildings.utils.BuildingEffect;
import org.example.server.model.cards.characters.*;
import org.example.server.model.cards.events.*;
import org.example.server.model.board.OfferTile;
import org.example.server.model.board.OfferTrack;
import org.example.server.model.board.OrderCell;
import org.example.server.model.board.OrderTile;
import org.example.shared.enums.CardType;
import org.example.shared.enums.Icon;
import org.example.shared.enums.Row;

import java.io.InputStream;
import java.util.*;

public class BoardConfigLoader {

    @FunctionalInterface
    interface CardParser {
        Card parse(JsonNode node, int era, Optional<Integer> numPlayers);
    }

    @FunctionalInterface
    interface BuildingParser {
        Building parse(JsonNode node, int era);
    }

    private static final Map<String, CardParser> CARD_PARSERS = Map.of(
        "HUNTER",          (n, era, np) -> new Hunter(n.get("id").asText(), n.get("extraFood").asBoolean(), era, np),
        "BUILDER",         (n, era, np) -> new Builder(n.get("id").asText(), n.get("discount").asInt(), n.get("pp").asInt(), era, np),
        "GATHERER",        (n, era, np) -> new Gatherer(n.get("id").asText(), n.get("discount").asInt(), era, np),
        "ARTIST",          (n, era, np) -> new Artist(n.get("id").asText(), era, np),
        "INVENTOR",        (n, era, np) -> new Inventor(n.get("id").asText(), Icon.valueOf(n.get("icon").asText()), era, np),
        "SHAMAN",          (n, era, np) -> new Shaman(n.get("id").asText(), n.get("starNum").asInt(), era, np),
        "HUNT",            (n, era, np) -> new Hunt(n.get("id").asText(), n.get("multiplierPP").asInt(), n.get("isFinal").asBoolean(), era, np),
        "SUSTENANCE",      (n, era, np) -> new Sustenance(n.get("id").asText(), n.get("lostPP").asInt(), n.get("isFinal").asBoolean(), era, np),
        "SHAMANIC_RITUAL", (n, era, np) -> new ShamanicRitual(n.get("id").asText(), n.get("lostPP").asInt(), n.get("gainedPP").asInt(), n.get("isFinal").asBoolean(), era, np),
        "CAVE_PAINTINGS", (n, era, np) -> new CavePaintings(n.get("id").asText(), n.get("minArtists").asInt(), n.get("lostPP").asInt(), n.get("multiplierPP").asInt(), n.get("isFinal").asBoolean(), era, np)
    );

    private static final Map<String, BuildingParser> BUILDING_PARSERS = Map.of(
        "SINCE_BUILT",       (n, era) -> new SinceBuilt(n.get("id").asText(), n.get("endGamePP").asInt(), n.get("cost").asInt(), null, era, Optional.empty(), BuildingEffect.valueOf(n.get("buildingEffect").asText())),
        "DURING_SUSTENANCE", (n, era) -> new DuringSustenance(n.get("id").asText(), n.get("endGamePP").asInt(), n.get("cost").asInt(), era, Optional.empty(), CardType.valueOf(n.get("typeMultiplier").asText())),
        "DURING_RITUAL",     (n, era) -> new DuringRitual(n.get("id").asText(), n.get("endGamePP").asInt(), n.get("cost").asInt(), era, Optional.empty(), BuildingEffect.valueOf(n.get("buildingEffect").asText())),
        "DURING_HUNT",       (n, era) -> new DuringHunt(n.get("id").asText(), n.get("endGamePP").asInt(), n.get("cost").asInt(), era, Optional.empty()),
        "DURING_PAINTINGS",  (n, era) -> new DuringPaintings(n.get("id").asText(), n.get("endGamePP").asInt(), n.get("cost").asInt(), era, Optional.empty()),
        "BONUS_TOTEM",       (n, era) -> new BonusTotem(n.get("id").asText(), n.get("endGamePP").asInt(), n.get("cost").asInt(), era, Optional.empty()),
        "EXTRA_MOVE",        (n, era) -> new ExtraMove(n.get("id").asText(), n.get("endGamePP").asInt(), n.get("cost").asInt(), null, null, era, Optional.empty()),
        "END_GAME",          (n, era) -> {
            JsonNode charType = n.get("characterTypeMultiplier");
            CardType ct = (charType != null && !charType.isNull()) ? CardType.valueOf(charType.asText()) : null;
            return new EndGame(n.get("id").asText(), n.get("endGamePP").asInt(), n.get("cost").asInt(), era, Optional.empty(), BuildingEffect.valueOf(n.get("buildingEffect").asText()), ct);
        }
    );

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Returns cards grouped by era (index 0 = era1, 1 = era2, 2 = era3).
     * Loads cards_2p.json as base, then incrementally adds cards from 3p, 4p, 5p files.
     * Suitable to pass directly to Deck.createDeck().
     */
    public ArrayList<Set<Card>> getCards(int numPlayers) {
        // 3 sets, one per era
        ArrayList<Set<Card>> result = new ArrayList<>();
        for (int i = 0; i < 3; i++) result.add(new HashSet<>());

        // Base file (2p): contains characters AND events
        loadCardsFromFile("/cards/cards_2p.json", "characters and events", 2, result);

        // Incremental files (3p, 4p, 5p): contain only additional characters
        for (int n = 3; n <= numPlayers; n++) {
            loadCardsFromFile("/cards/cards_" + n + "p.json", "characters", n, result);
        }

        return result;
    }

    private void loadCardsFromFile(String filename, String arrayKey, int fileNumPlayers, ArrayList<Set<Card>> result) {
        try (InputStream is = getClass().getResourceAsStream(filename)) {
            if (is == null) throw new IllegalArgumentException("Resource not found: " + filename);
            JsonNode root = mapper.readTree(is);
            Optional<Integer> np = Optional.of(fileNumPlayers);

            for (int era = 1; era <= 3; era++) {
                JsonNode eraNode = root.get("era" + era);
                if (eraNode == null) continue;
                JsonNode cardArray = eraNode.get(arrayKey);
                if (cardArray == null) continue;
                for (JsonNode node : cardArray) {
                    String type = node.get("type").asText();
                    CardParser parser = CARD_PARSERS.get(type);
                    if (parser != null) result.get(era - 1).add(parser.parse(node, era, np));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load cards from " + filename, e);
        }
    }

    /**
     * Returns the OfferTrack for the given number of players.
     * Includes all tiles with numPlayers <= the given value.
     */
    public OfferTrack getOfferTrack(int numPlayers) {
        try (InputStream is = getClass().getResourceAsStream("/offer_tiles.json")) {
            if (is == null) throw new IllegalArgumentException("Resource not found: /offer_tiles.json");
            JsonNode root = mapper.readTree(is);

            ArrayList<OfferTile> tiles = new ArrayList<>();
            for (JsonNode node : root) {
                if (node.get("numPlayers").asInt() <= numPlayers) {
                    Map<Row, Integer> moves = new EnumMap<>(Row.class);
                    JsonNode movesNode = node.get("moves");
                    movesNode.fields().forEachRemaining(e -> moves.put(Row.valueOf(e.getKey()), e.getValue().asInt()));
                    boolean givesFood = node.get("givesFood").asBoolean();
                    tiles.add(new OfferTile(Optional.empty(), moves, givesFood));
                }
            }
            return new OfferTrack(tiles);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load offer tiles", e);
        }
    }

    /**
     * Returns the OrderTile (turn order track) for the given number of players.
     */
    public OrderTile getOrderTile(int numPlayers) {
        try (InputStream is = getClass().getResourceAsStream("/order_tiles.json")) {
            if (is == null) throw new IllegalArgumentException("Resource not found: /order_tiles.json");
            JsonNode root = mapper.readTree(is);

            for (JsonNode node : root) {
                if (node.get("numPlayers").asInt() == numPlayers) {
                    ArrayList<OrderCell> cells = new ArrayList<>();
                    for (JsonNode cellNode : node.get("cells")) {
                        cells.add(new OrderCell(Optional.empty(), cellNode.get("bonus").asInt(), cellNode.get("isMalus").asBoolean()));
                    }
                    return new OrderTile(cells);
                }
            }
            throw new IllegalArgumentException("No order tile found for " + numPlayers + " players");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load order tiles", e);
        }
    }

    /**
     * Returns all buildings (all eras) as a flat set.
     * makeBuildingDecks() in InitGameState handles era separation and selection.
     */
    public Set<Building> getBuildings() {
        String filename = "/cards/buildings.json";
        try (InputStream is = getClass().getResourceAsStream(filename)) {
            if (is == null) throw new IllegalArgumentException("Resource not found: " + filename);
            JsonNode root = mapper.readTree(is);

            Set<Building> result = new HashSet<>();
            for (int era = 1; era <= 3; era++) {
                for (JsonNode node : root.get("era" + era)) {
                    String type = node.get("type").asText();
                    BuildingParser parser = BUILDING_PARSERS.get(type);
                    if (parser != null) result.add(parser.parse(node, era));
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load buildings", e);
        }
    }
}

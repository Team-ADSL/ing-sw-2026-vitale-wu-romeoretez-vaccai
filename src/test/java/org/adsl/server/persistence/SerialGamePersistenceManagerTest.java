package org.adsl.server.persistence;

import org.adsl.server.model.Game;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SerialGamePersistenceManagerTest {

    @TempDir
    Path tempDir;

    private SerialGamePersistenceManager buildManager() {
        return new SerialGamePersistenceManager(tempDir.toString());
    }

    // ──────────────────────────────────────────────
    // updateGame
    // ──────────────────────────────────────────────

    @Test
    void testUpdateGame_savesSerializedFileWithCorrectName() {
        SerialGamePersistenceManager manager = buildManager();
        Game game = new Game(42, 2);

        manager.updateGame(game);

        File expected = tempDir.resolve("game_42.ser").toFile();
        assertTrue(expected.exists(), "Serialized file must be created with name game_<id>.ser");
    }

    @Test
    void testUpdateGame_calledTwice_overwritesExistingFile() {
        SerialGamePersistenceManager manager = buildManager();
        Game game = new Game(7, 2);

        manager.updateGame(game);
        long firstSize = tempDir.resolve("game_7.ser").toFile().length();
        manager.updateGame(game);
        long secondSize = tempDir.resolve("game_7.ser").toFile().length();

        assertEquals(firstSize, secondSize, "Overwriting the same game must produce the same file size");
    }

    // ──────────────────────────────────────────────
    // recoverGames
    // ──────────────────────────────────────────────

    @Test
    void testRecoverGames_emptyDirectory_returnsEmptyList() throws Exception {
        SerialGamePersistenceManager manager = buildManager();

        List<Game> recovered = manager.recoverGames();

        assertTrue(recovered.isEmpty());
    }

    @Test
    void testRecoverGames_savedGame_returnsGameWithSameId() throws Exception {
        SerialGamePersistenceManager manager = buildManager();
        Game game = new Game(99, 4);
        manager.updateGame(game);

        List<Game> recovered = manager.recoverGames();

        assertEquals(1, recovered.size());
        assertEquals(99, recovered.getFirst().getGameId());
    }

    @Test
    void testRecoverGames_multipleSavedGames_returnsAll() throws Exception {
        SerialGamePersistenceManager manager = buildManager();
        manager.updateGame(new Game(1, 2));
        manager.updateGame(new Game(2, 3));
        manager.updateGame(new Game(3, 4));

        List<Game> recovered = manager.recoverGames();

        assertEquals(3, recovered.size());
    }

    @Test
    void testRecoverGames_recoveredGame_hasNullTransientObservers() throws Exception {
        SerialGamePersistenceManager manager = buildManager();
        manager.updateGame(new Game(1, 2));

        List<Game> games = manager.recoverGames();
        Game recovered = games.getFirst();

        assertThrows(NullPointerException.class,
                () -> recovered.addVirtualClient(null),
                "Deserialization does not run field initializers: transient observer lists are null after recovery");
    }

    // ──────────────────────────────────────────────
    // removeGame
    // ──────────────────────────────────────────────

    @Test
    void testRemoveGame_existingFile_deletesIt() throws Exception {
        SerialGamePersistenceManager manager = buildManager();
        Game game = new Game(5, 2);
        manager.updateGame(game);

        manager.removeGame(5);

        assertFalse(tempDir.resolve("game_5.ser").toFile().exists(),
                "removeGame must delete the serialized file");
    }

    @Test
    void testRemoveGame_nonExistentGame_doesNotThrow() {
        SerialGamePersistenceManager manager = buildManager();

        assertDoesNotThrow(() -> manager.removeGame(999));
    }
}

package org.adsl.server.persistence;

import org.adsl.server.model.Game;
import org.adsl.shared.enums.Totem;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link GamePersistenceManager} that serialises {@link Game} objects to
 * {@code .ser} files in a configurable directory.
 * <p>
 * Each call to {@link #updateGame} overwrites the file for that game ID
 * ({@code game_<id>.ser}). {@link #recoverGames} reads all {@code .ser} files
 * in the directory on startup. {@link #removeGame} deletes the file when the
 * game ends.
 * </p>
 */
public class SerialGamePersistenceManager implements GamePersistenceManager{

    private final String saveDirectory;

    /**
     * Creates a manager that stores game snapshots under {@code saveDirectory},
     * creating the directory if it does not already exist.
     *
     * @param saveDirectory path of the directory used to store {@code .ser} files
     */
    public SerialGamePersistenceManager(String saveDirectory) {
        this.saveDirectory = saveDirectory;
        ensureDirectoryExists();
    }

    private void ensureDirectoryExists() {
        try {
            Files.createDirectories(Paths.get(saveDirectory));
        } catch (IOException e) {
            System.err.println("Critical error: backup repository " + saveDirectory + " does not exists.");
        }
    }

    /**
     * Serialises the entire {@link Game} object to {@code game_<id>.ser},
     * overwriting any previous snapshot for that game. Called as a
     * {@link org.adsl.server.model.GameObserver} callback every time the game
     * model changes (i.e. on each move), so the on-disk state always reflects
     * the latest game state and can be used to recover after a crash.
     *
     * @param game the updated game model to persist
     */
    @Override
    public void updateGame(Game game) {
        String fileName = saveDirectory + File.separator + "game_" + game.getGameId() + ".ser";

        try (FileOutputStream fileOut = new FileOutputStream(fileName);
             ObjectOutputStream out = new ObjectOutputStream(fileOut)) {

            out.writeObject(game);
            System.out.println("Game " + game.getGameId() + " saved correctly.");

        } catch (IOException e) {
            System.err.println("Error: game " + game.getGameId() + " was not saved correctly: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Reads every {@code .ser} file in {@code saveDirectory} and deserialises it
     * back into a {@link Game}. Files that fail to deserialise (e.g. corrupted)
     * are skipped and logged, not thrown.
     *
     * @return list of recovered games (empty if the directory has no {@code .ser} files)
     * @throws Exception if reading the directory itself fails
     */
    @Override
    public List<Game> recoverGames() throws Exception {
        List<Game> recoveredGames = new ArrayList<>();
        File folder = new File(saveDirectory);
        File[] listOfFiles = folder.listFiles((dir, name) -> name.endsWith(".ser"));

        if (listOfFiles == null || listOfFiles.length == 0) {
            System.out.println("No backup found in directory " + saveDirectory + ".");
            return recoveredGames;
        }

        for (File file : listOfFiles) {
            try (FileInputStream fileIn = new FileInputStream(file);
                 ObjectInputStream in = new ObjectInputStream(fileIn)) {

                Game game = (Game) in.readObject();
                recoveredGames.add(game);
                System.out.println("Game " + game.getGameId() + " loaded ");

            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error during reading operation: " + file.getName() +
                        " may be corrupted. \n" + e.getMessage());
            }
        }

        return recoveredGames;
    }

    /**
     * Deletes the {@code game_<id>.ser} snapshot file, if it exists. Called once
     * the game ends or is cancelled, since it no longer needs to be recoverable.
     *
     * @param gameId the game whose snapshot should be removed
     * @throws Exception never thrown directly; I/O errors are caught and logged
     */
    @Override
    public void removeGame(int gameId) throws Exception {
        String fileName = saveDirectory + File.separator + "game_" + gameId + ".ser";
        try {
            Files.deleteIfExists(Paths.get(fileName));
        } catch (IOException e) {
            System.err.println("Error: backup for " + gameId + " was not removed.");
        }
    }

    // Need to save only game status update
    /**
     * No-op: lobby roster changes (before the game starts) are not part of the
     * recoverable game state and are not persisted.
     *
     * @param gameId            the game identifier (unused)
     * @param players           current lobby players (unused)
     * @param numPlayersAllowed maximum players for this game (unused)
     */
    @Override
    public void updateLobby(int gameId, List<String> players, int numPlayersAllowed) {}

    /**
     * No-op: totem-selection progress is not part of the recoverable game state
     * and is not persisted.
     *
     * @param totemsAvailable totems not yet chosen (unused)
     * @param message         log message (unused)
     */
    @Override
    public void updateTotemAvailable(List<Totem> totemsAvailable, String message) {}
}

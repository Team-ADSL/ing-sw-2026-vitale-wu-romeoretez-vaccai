package org.adsl.server.persistence;

import org.adsl.server.model.Game;
import org.adsl.shared.model.GameDTO;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SerialGamePersistenceManager implements GamePersistenceManager{

    private final String saveDirectory;

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
    @Override
    public void updateLobby(List<String> players) {}
}

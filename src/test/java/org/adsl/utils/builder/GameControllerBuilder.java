package org.adsl.utils.builder;

import org.adsl.server.config.BoardConfigLoader;
import org.adsl.server.controller.GameController;
import org.adsl.server.persistence.GameDAO;
import org.adsl.server.persistence.GamePersistenceManager;
import org.adsl.utils.TestDummies;
import org.adsl.utils.fakes.FakeGameDAO;
import org.adsl.utils.fakes.FakeGamePersistenceManager;

public class GameControllerBuilder {
    private BoardConfigLoader loader = new TestDummies.DummyBoardConfigLoader();
    private GamePersistenceManager persistence = new FakeGamePersistenceManager();
    private GameDAO dao = new FakeGameDAO();

    public GameController build() {
        return new GameController(loader, persistence, dao);
    }

    public GameControllerBuilder withGameDAO(GameDAO dao) {
        this.dao = dao;
        return this;
    }
}

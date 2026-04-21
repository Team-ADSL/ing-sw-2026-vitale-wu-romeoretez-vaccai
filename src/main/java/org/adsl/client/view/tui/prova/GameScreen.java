package org.adsl.client.view.tui.prova;

import org.adsl.client.AppCoordinator;

public class GameScreen implements Screen{
    private TUI
    private AppCoordinator
    

    public Screen handleEvent(){
        if(GameEvent){
            ....
            tui.toUpdate = true;
        }else {
            return new EndGameScreen();
        }
    }

    public void handleInput(){
        switch (input){
            s -> startGame();
        }
    }

    public startGame(){
        appcoordinator.createStartRequest
    }
}

package org.adsl.client.view.tui.prova;

public class TUIprova {
    private Screen currentScreen;

    public void handleEvent(Event event){
        Screen newScreen = currentScreen.handleEvent(event);
        if(currentScreen != newScreen ){
            changeScreen();
        }
    }

    public void loop(){
        while(){
            if(toUpdate){
                screenn.render();
            }

            try{
                input;
                screen.handleKeyPressed(input);
            }
        }
    }

    public void handleDisconnection(){

    }
}

package org.adsl.client.view.tui.screens;

import org.adsl.client.AppCoordinator;
import org.adsl.client.serverEvents.TotemAvailableEvent;
import org.adsl.client.view.tui.render.TuiTerminal;
import org.adsl.shared.enums.Totem;
import org.adsl.shared.network.responses.TotemAvailableUpdate;

import java.io.IOException;
import java.util.List;

public class TotemPickingScreen extends TUIScreen{
    private List<Totem> totemList;

    public TotemPickingScreen(TuiTerminal terminal, AppCoordinator appCoordinator,
                              String username, List<Totem> totemList){
        this.totemList = totemList;
        super(terminal, appCoordinator, username);
    }

    @Override
    public void render() throws IOException {

    }

    @Override
    public TUIScreen visit(TotemAvailableEvent event) {
        return null;
    }
}

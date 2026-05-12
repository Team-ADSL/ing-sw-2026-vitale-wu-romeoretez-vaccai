package org.adsl.client.view.gui.screens;

import org.adsl.client.AppCoordinator;
import org.adsl.client.serverEvents.TotemAvailableEvent;
import org.adsl.shared.enums.Totem;

import java.util.List;

public class TotemPickingScreen extends GUIScreen {
    private List<Totem> totemList;

    protected TotemPickingScreen(AppCoordinator coordinator, String username, List<Totem> totemList) {
        this.totemList = totemList;
        super(coordinator, username);
    }

    @Override
    public GUIScreen visit(TotemAvailableEvent event) {
        return null;
    }
}

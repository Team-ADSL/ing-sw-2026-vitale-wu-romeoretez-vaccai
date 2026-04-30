package org.adsl.server.controller;

import org.adsl.shared.network.requests.LoginRequest;
import org.adsl.shared.network.requests.LogoutRequest;
import org.adsl.utils.builder.ServerControllerBuilder;
import org.adsl.utils.fakes.FakeVirtualClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class LogoutReloginTest {
    @Test
    void canLoginAfterLogoutWithSameUsername() {
        ServerController sc = new ServerControllerBuilder().build();
        FakeVirtualClient client = new FakeVirtualClient();

        sc.visit(new LoginRequest("alice"), client);
        sc.visit(new LogoutRequest(), client);
        assertDoesNotThrow(() -> sc.visit(new LoginRequest("alice"), client),
                "Re-login with same username after logout must succeed");
    }
}

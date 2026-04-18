package org.adsl.fakes;

import java.rmi.Remote;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;


public class FakeRegistry implements Registry {
    public final List<String> unboundNames = new ArrayList<>();

    @Override public void unbind(String name) { unboundNames.add(name); }

    @Override public Remote lookup(String name) { return null; }
    @Override public void bind(String name, Remote obj) {}
    @Override public void rebind(String name, Remote obj) {}
    @Override public String[] list() { return new String[0]; }
}
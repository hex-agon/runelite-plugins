package com.example;

import net.runelite.api.Client;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;

@PluginDescriptor(
        name = "Example"
)
public class ExamplePlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private DebugOverlay overlay;

    @Override
    protected void startUp() {
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(overlay);
    }
}

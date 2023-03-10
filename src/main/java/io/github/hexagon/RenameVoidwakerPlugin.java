package io.github.hexagon;

import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemID;
import net.runelite.api.events.PostItemComposition;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;

@PluginDescriptor(name = "Rename Voidwaker")
public class RenameVoidwakerPlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Override
    protected void startUp() {
        clientThread.invokeLater(() -> client.getItemCompositionCache().reset());
    }

    @Override
    protected void shutDown() {
        clientThread.invokeLater(() -> client.getItemCompositionCache().reset());
    }

    @Subscribe
    public void onPostItemComposition(PostItemComposition event) {
        ItemComposition composition = event.getItemComposition();

        if (composition.getId() == ItemID.VOIDWAKER_27690) {
            composition.setName("Korasi's Sword");
        }
    }
}

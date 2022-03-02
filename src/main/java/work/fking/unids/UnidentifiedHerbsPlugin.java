package work.fking.unids;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.PostItemComposition;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.regex.Pattern;

@Slf4j
@PluginDescriptor(
        name = "Unidentified Herbs"
)
public class UnidentifiedHerbsPlugin extends Plugin {

    private static final int[] GRIMY_HERBS = {199, 201, 203, 205, 207, 209, 211, 213, 215, 217, 219, 2485, 3049, 3051};
    private static final int ORIGINAL_UNID_MODEL = 2364;
    private static final Pattern CLEAN_MESSAGE_PATTERN = Pattern.compile("You need level \\d{1,2} Herblore to clean the .+");
    private static final String CLEAN_MESSAGE_REPLACEMENT = "Your Herblore level is not high enough to identify this herb.";

    @Inject
    Client client;

    @Inject
    private ClientThread clientThread;

    @Override
    protected void startUp() {
        resetCaches();
    }

    @Override
    protected void shutDown() throws Exception {
        resetCaches();
    }

    @Subscribe
    public void onPostItemComposition(PostItemComposition event) {
        ItemComposition itemComposition = event.getItemComposition();
        int itemId = itemComposition.getId();

        if (isGrimyHerb(itemId)) {
            try {
                itemComposition.setName("Herb");
                itemComposition.setInventoryModel(ORIGINAL_UNID_MODEL);
                itemComposition.setColorToReplace(null);
                itemComposition.setColorToReplaceWith(null);
                itemComposition.getInventoryActions()[0] = "Identify";
            } catch (Exception e) {
                log.error("Could not modify the item composition", e);
            }
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage chatMessage) {
        if (CLEAN_MESSAGE_PATTERN.matcher(chatMessage.getMessage()).matches()) {
            chatMessage.getMessageNode().setValue(CLEAN_MESSAGE_REPLACEMENT);
        }
    }

    private void resetCaches() {
        clientThread.invokeLater(() -> {
            client.getItemCompositionCache().reset();
            client.getItemModelCache().reset();
            client.getItemSpriteCache().reset();
        });
    }

    private boolean isGrimyHerb(int itemId) {
        return Arrays.binarySearch(GRIMY_HERBS, itemId) >= 0;
    }
}

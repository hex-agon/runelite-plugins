package work.fking.nexnostalgia;

import com.google.inject.Provides;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.CommandExecuted;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

import javax.inject.Inject;

@PluginDescriptor(name = "Nex Nostalgia")
public class NexNostalgiaPlugin extends Plugin {

    private NexClipPlayer clipPlayer;

    @Inject
    private Client client;

    @Inject
    private NexNostalgiaConfig config;

    @Override
    protected void startUp() {
        if (clipPlayer == null || clipPlayer.isShutdown()) {
            clipPlayer = new NexClipPlayer(2);
        }
    }

    @Override
    protected void shutDown() {
        clipPlayer.shutdown();
    }

    @Provides
    NexNostalgiaConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(NexNostalgiaConfig.class);
    }

    @Subscribe
    public void onCommandExecuted(CommandExecuted command) {
        String[] arguments = command.getArguments();

        if (!command.getCommand().equals("vo")) {
            return;
        }
        if (arguments.length < 1) {
            return;
        }
        String voiceOverName = arguments[0].toUpperCase();

        try {
            VoiceOver voiceOver = VoiceOver.valueOf(voiceOverName);
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Playing voiceover " + voiceOver, null);

            playVoiceOver(voiceOver);
        } catch (IllegalArgumentException e) {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Unknown voiceover: " + voiceOverName, null);
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (event.getType() != ChatMessageType.NPC_SAY) {
            return;
        }
        String text = Text.removeTags(event.getMessage());

        if (!text.startsWith("Nex|")) {
            return;
        }
        VoiceOver voiceOver = VoiceOver.forTriggerLine(text.substring(4));

        if (voiceOver != null) {
            playVoiceOver(voiceOver);
        }
    }

    private void playVoiceOver(VoiceOver voiceOver) {
        clipPlayer.play(voiceOver, config.volumeGain());
    }
}

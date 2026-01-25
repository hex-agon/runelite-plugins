package work.fking.nexnostalgia;

import com.google.inject.Provides;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.gameval.AnimationID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.animsmoothing.AnimationSmoothingPlugin;
import net.runelite.client.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@PluginDescriptor(name = "Nex Nostalgia")
public class NexNostalgiaPlugin extends Plugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(NexNostalgiaPlugin.class);

    // during phase transitions there can be up to 3 VOs playing at the same time
    private static final int MAX_CONCURRENT_SOUNDS = 3;

    private NexClipPlayer clipPlayer;

    @Inject
    private Client client;

    @Inject
    private NexNostalgiaConfig config;

    @Inject
    private PluginManager pluginManager;

    @Inject
    private AnimationSmoothingPlugin animationSmoothingPlugin;

    @Override
    protected void startUp() {
        if (clipPlayer == null || clipPlayer.isShutdown()) {
            clipPlayer = new NexClipPlayer(MAX_CONCURRENT_SOUNDS);
        }
        setupAnimSmoothingFilter();
    }

    @Override
    protected void shutDown() {
        clipPlayer.shutdown();
        tearDownAnimSmoothingFilter();
    }

    @Provides
    NexNostalgiaConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(NexNostalgiaConfig.class);
    }

    @Subscribe
    public void onCommandExecuted(CommandExecuted command) {
        var arguments = command.getArguments();

        if (!command.getCommand().equals("vo")) {
            return;
        }
        if (arguments.length < 1) {
            return;
        }
        var voiceOverName = arguments[0].toUpperCase();

        try {
            var voiceOver = VoiceOver.valueOf(voiceOverName);
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
        var text = Text.removeTags(event.getMessage());

        if (!text.startsWith("Nex|")) {
            return;
        }
        var voiceOver = VoiceOver.forTriggerLine(text.substring(4));

        if (voiceOver != null) {
            playVoiceOver(voiceOver);
        }
    }

    private void playVoiceOver(VoiceOver voiceOver) {
        clipPlayer.play(voiceOver, config.volumeGain());
    }

    private boolean isAnimSmoothingPluginEnabled() {
        return pluginManager.isPluginEnabled(animationSmoothingPlugin);
    }

    private void tearDownAnimSmoothingFilter() {
        if (!config.enableAnimSmoothing() || isAnimSmoothingPluginEnabled()) {
            return;
        }
        client.setAnimationInterpolationFilter(null);
    }

    private void setupAnimSmoothingFilter() {
        if (!config.enableAnimSmoothing()) {
            return;
        }

        if (isAnimSmoothingPluginEnabled()) {
            LOGGER.debug("Skipping setting up the animation filter as the Animation Smoothing plugin is enabled");
            return;
        }
        client.setAnimationInterpolationFilter(animId -> {
            switch (animId) {
                case AnimationID.NEX_RUN:
                case AnimationID.NEX_PET_RUN:
                case AnimationID.NEX_READY:
                case AnimationID.NEX_DASH_ATTACK:
                case AnimationID.NEX_TURMOIL:
                case AnimationID.NEX_ATTACK:
                case AnimationID.NEX_PET_ATTACK:
                case AnimationID.NEX_BLAST_AWAY:
                case AnimationID.NEX_BLOOD_SIPHON:
                case AnimationID.NEX_DEATH:
                case AnimationID.NEX_DEFEND:
                case AnimationID.NEX_SMASH_ATTACK:
                case AnimationID.NEX_SPIN_OUT:
                case AnimationID.NEX_ALTERNATE_CAST_ATTACK:
                case AnimationID.NEX_CAST_ATTACK:

                case AnimationID.BLOOD_REAVER_DEATH:
                case AnimationID.BLOOD_REAVER_WALK:
                case AnimationID.BLOOD_REAVER_ATTACK:
                case AnimationID.BLOOD_REAVER_READY:
                case AnimationID.BLOOD_REAVER_DEFEND:

                case AnimationID.NEX_MUSHROOM_CLOUD_PROJANIM:
                case AnimationID.NEX_MUSHROOM_CLOUD_SPOTANIM:

                case AnimationID.NEX_SUMMON:
                    return true;
                default:
                    return false;
            }
        });
    }
}

package work.fking.nexnostalgia;

import com.google.inject.Provides;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.animsmoothing.AnimationSmoothingPlugin;
import net.runelite.client.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.sound.sampled.LineUnavailableException;

@PluginDescriptor(name = "Nex Nostalgia")
public class NexNostalgiaPlugin extends Plugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(NexNostalgiaPlugin.class);
    private static final int MAX_CONCURRENT_SOUNDS = 5;

    private SoundPlayer soundPlayer;

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private NexNostalgiaConfig config;

    @Inject
    private PluginManager pluginManager;

    @Inject
    private AnimationSmoothingPlugin animationSmoothingPlugin;

    @Inject
    private ChatMessageManager chatMessageManager;

    @Inject
    @Named("developerMode")
    private boolean developerMode;

    @Override
    protected void startUp() {
        if (soundPlayer == null || soundPlayer.isShutdown()) {
            try {
                soundPlayer = new SoundPlayer(MAX_CONCURRENT_SOUNDS);
                clientThread.invokeLater(this::updateSoundPlayerVolume);
            } catch (LineUnavailableException e) {
                LOGGER.warn("Could not open audio output line, SoundPlayer will be disabled", e);
            }
        }

        if (config.enableAnimSmoothing()) {
            setupAnimSmoothingFilter();
        }
    }

    @Override
    protected void shutDown() {
        if (soundPlayer != null) {
            soundPlayer.shutdown();
        }

        if (config.enableAnimSmoothing()) {
            tearDownAnimSmoothingFilter();
        }
    }

    @Provides
    NexNostalgiaConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(NexNostalgiaConfig.class);
    }

    @Subscribe
    public void onCommandExecuted(CommandExecuted command) {
        var arguments = command.getArguments();

        if (!developerMode || !command.getCommand().equals("vo")) {
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
    public void onConfigChanged(ConfigChanged event) {
        if (!NexNostalgiaConfig.GROUP.equals(event.getGroup())) {
            return;
        }

        switch (event.getKey()) {
            case NexNostalgiaConfig.KEY_EXTRA_VO_VOLUME:
                clientThread.invokeLater(this::updateSoundPlayerVolume);
                break;
            case NexNostalgiaConfig.KEY_ANIM_SMOOTHING:
                if (config.enableAnimSmoothing()) {
                    setupAnimSmoothingFilter();
                } else {
                    tearDownAnimSmoothingFilter();
                }
                break;
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

    @Subscribe
    public void onVarbitChanged(VarbitChanged event) {
        if (event.getVarpId() != VarPlayerID.OPTION_SOUNDS) {
            return;
        }
        updateSoundPlayerVolume();
    }

    // Must be called from within client thread
    private void updateSoundPlayerVolume() {
        if (soundPlayer == null) {
            return;
        }
        var soundVolume = client.getVarpValue(VarPlayerID.OPTION_SOUNDS);
        soundPlayer.setMasterVolume(soundVolume + config.extraVoVolume());
    }

    private void playVoiceOver(VoiceOver voiceOver) {
        if (soundPlayer != null) {
            soundPlayer.play(voiceOver);
        }
    }

    private boolean isAnimSmoothingPluginEnabled() {
        return pluginManager.isPluginEnabled(animationSmoothingPlugin);
    }

    private void tearDownAnimSmoothingFilter() {
        if (isAnimSmoothingPluginEnabled()) {
            return;
        }
        client.setAnimationInterpolationFilter(null);
    }

    private void setupAnimSmoothingFilter() {
        if (isAnimSmoothingPluginEnabled()) {
            var formattedMessage = new ChatMessageBuilder()
                    .append(ChatColorType.HIGHLIGHT)
                    .append("[Nex Nostalgia] Selective animation was not enabled, Animation Smoothing plugin is active.")
                    .build();

            chatMessageManager.queue(QueuedMessage.builder()
                                                  .type(ChatMessageType.CONSOLE)
                                                  .runeLiteFormattedMessage(formattedMessage)
                                                  .build());
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

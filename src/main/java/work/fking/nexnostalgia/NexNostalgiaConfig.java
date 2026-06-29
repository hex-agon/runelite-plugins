package work.fking.nexnostalgia;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(NexNostalgiaConfig.GROUP)
public interface NexNostalgiaConfig extends Config {

    String GROUP = "nexNostalgia";
    String KEY_EXTRA_VO_VOLUME = "extraVoVolume";
    String KEY_ANIM_SMOOTHING = "enableAnimSmoothing";

    @ConfigItem(
            keyName = KEY_EXTRA_VO_VOLUME,
            name = "Extra Volume",
            description = "The extra volume to be applied to Voice Overs."
    )
    @Range(min = -25, max = 75)
    default int extraVoVolume() {
        return 10;
    }

    @ConfigItem(
            keyName = KEY_ANIM_SMOOTHING,
            name = "Selective Anim Smoothing",
            description = "Selectively enables animation smoothing for Nex & Blood Reavers animations"
    )
    default boolean enableAnimSmoothing() {
        return false;
    }
}

package work.fking.nexnostalgia;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(NexNostalgiaConfig.GROUP)
public interface NexNostalgiaConfig extends Config {

    String GROUP = "nexNostalgia";

    String ANIM_SMOOTHING = "Animation Smoothing";

    @ConfigItem(
            keyName = "volumeGain",
            name = "Volume Gain",
            description = "The volume gain used for the voice over audios."
    )
    @Range(min = -25, max = 6)
    default int volumeGain() {
        return 0;
    }

    @ConfigItem(
            keyName = "enableAnimSmoothing",
            name = "Selective Anim Smoothing",
            description = "Selectively enables animation smoothing for Nex & Blood Reavers animations"
    )
    default boolean enableAnimSmoothing() {
        return false;
    }
}

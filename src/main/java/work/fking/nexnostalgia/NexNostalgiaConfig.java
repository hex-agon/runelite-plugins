package work.fking.nexnostalgia;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(NexNostalgiaConfig.GROUP)
public interface NexNostalgiaConfig extends Config {

    String GROUP = "nexNostalgia";

    @ConfigItem(
            keyName = "enableAnimSmoothing",
            name = "Selective Anim Smoothing",
            description = "Selectively enables animation smoothing for Nex & Blood Reavers animations"
    )
    default boolean enableAnimSmoothing() {
        return false;
    }
}

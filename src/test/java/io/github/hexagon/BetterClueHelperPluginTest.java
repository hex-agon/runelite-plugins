package io.github.hexagon;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BetterClueHelperPluginTest {

    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(BetterClueHelperPlugin.class);
        RuneLite.main(args);
    }
}
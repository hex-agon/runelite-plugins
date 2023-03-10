package io.github.hexagon;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class RenameVoidwakerPluginTest {

    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(RenameVoidwakerPlugin.class);
        RuneLite.main(args);
    }
}
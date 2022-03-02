package work.fking.unids;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PluginTest {

    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(UnidentifiedHerbsPlugin.class);
        RuneLite.main(args);
    }
}
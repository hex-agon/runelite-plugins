package work.fking.rl.stars;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ShootingStarsPluginTest {

    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(ShootingStars.class);
        RuneLite.main(args);
    }
}
package work.fking.rl.stars;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

public class ShootingStarsOverlay extends Overlay {

    private final Client client;
    private final ShootingStars shootingStars;

    @Inject
    public ShootingStarsOverlay(Client client, ShootingStars shootingStars) {
        this.client = client;
        this.shootingStars = shootingStars;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.UNDER_WIDGETS);
        setPriority(OverlayPriority.MED);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        CrashedStar crashedStar = shootingStars.crashedStar();
        if (crashedStar == null || !crashedStar.worldPoint().isInScene(client)) {
            return null;
        }
        graphics.drawString(crashedStar.toString(), 30, 40);
        LocalPoint location = LocalPoint.fromWorld(client, crashedStar.worldPoint());

        if (location == null) {
            return null;
        }
        graphics.setFont(FontManager.getRunescapeBoldFont());
        String text = "T" + crashedStar.tier();
        location = new LocalPoint(location.getX() + 64, location.getY() + 64);
        Point point = Perspective.getCanvasTextLocation(client, graphics, location, text, 60);

        if (point != null) {
            graphics.setColor(Color.ORANGE);
            OverlayUtil.renderTextLocation(graphics, point, text, Color.ORANGE);
        }
        return null;
    }
}

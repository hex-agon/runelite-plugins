package com.example;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

public class DebugOverlay extends Overlay {

    private static final int HEAT_HOT = 0;
    private static final int HEAT_MEDIUM = 1;
    private static final int HEAT_COLD = 2;

    private final Client client;

    @Inject
    public DebugOverlay(Client client) {
        this.client = client;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.MED);
    }

    @Override
    public Dimension render(Graphics2D g) {
        int metalQuality = client.getVarbitValue(13938);
        int currentQuality = client.getVarbitValue(13939);
        int startingQuality = client.getVarbitValue(13950);
        int heat = client.getVarbitValue(13948);
        int progress = client.getVarbitValue(13949);
        int y = 140;

        g.drawString("Quality: " + currentQuality + "/" + startingQuality, 30, y += 14);
        g.drawString("Metal quality: " + metalQuality, 30, y += 14);
        g.drawString("Progress: " + progress, 30, y += 14);

        var currentHeatType = currentHeatType();
        g.setColor(heatColor(currentHeatType));
        g.drawString("Heat: " + heat, 30, y += 14);
        g.setColor(Color.WHITE);

        var segmentCount = segmentCount(metalQuality);
        g.drawString("Segments: " + segmentCount, 30, y += 14);

        var currentSegment = currentSegment();
        g.drawString("Current segment: " + currentSegment, 30, y += 14);

        y += 14;
        var segmentOffset = 16;
        for (int segment = 0; segment < segmentCount; segment++) {
            g.setColor(heatColor(segmentHeatType(segment)));
            g.drawString(String.valueOf(segment), segmentOffset += 14, y);
        }

        var currentSegmentHeatType = segmentHeatType(currentSegment);

        if (currentSegmentHeatType != currentHeatType) {
            g.setColor(Color.MAGENTA);
            g.drawString("Current heat is outside of the sweet spot!", 250, 150);
        }
        return null;
    }

    private int segmentHeatType(int segment) {
        return switch (segment) {
            case 0 -> HEAT_HOT;
            case 1 -> client.getVarbitValue(13940);
            case 2 -> client.getVarbitValue(13941);
            case 3 -> client.getVarbitValue(13942);
            case 4 -> client.getVarbitValue(13943);
            case 5 -> client.getVarbitValue(13944);
            case 6 -> client.getVarbitValue(13945);
            default -> -1;
        };
    }

    private int currentHeatType() {
        int heat = client.getVarbitValue(13948);
        int metalQuality = client.getVarbitValue(13938);

        int sweetSpotWidth = heatSweetSpotWidth(metalQuality);
        int hotOffset = heatOffset(HEAT_HOT, metalQuality);
        int mediumOffset = heatOffset(HEAT_MEDIUM, metalQuality);
        int coldOffset = heatOffset(HEAT_COLD, metalQuality);

        if (heat >= coldOffset && heat <= coldOffset + sweetSpotWidth) {
            return HEAT_COLD;
        } else if (heat >= mediumOffset && heat <= mediumOffset + sweetSpotWidth) {
            return HEAT_MEDIUM;
        } else if (heat >= hotOffset && heat <= hotOffset + sweetSpotWidth) {
            return HEAT_HOT;
        } else {
            return -1;
        }
    }

    private Color heatColor(int heatType) {
        return switch (heatType) {
            case HEAT_HOT -> Color.RED;
            case HEAT_MEDIUM -> Color.YELLOW;
            case HEAT_COLD -> Color.GREEN;
            default -> Color.GRAY;
        };
    }

    private int currentSegment() {
        int progress = client.getVarbitValue(13949);
        int metalQuality = client.getVarbitValue(13938);
        return scale(progress, 1000, segmentCount(metalQuality));
    }

    private int segmentCount(int metalQuality) {
        if (metalQuality >= 0 & metalQuality < 20) {
            return 3;
        } else if (metalQuality >= 20 & metalQuality < 60) {
            return 4;
        } else if (metalQuality >= 60 & metalQuality < 90) {
            return 5;
        } else if (metalQuality >= 90 & metalQuality < 120) {
            return 6;
        } else if (metalQuality >= 120) {
            return 7;
        } else {
            return 3;
        }
    }

    private int heatOffset(int heatType, int metalQuality) {
        var a = 1000 / 3;
        var b = a - a / 2;
        var width = a - scale(metalQuality, 130, b);
        return switch (heatType) {
            case HEAT_HOT -> a / 2 + a * 2 - width / 2;
            case HEAT_MEDIUM -> a / 2 + a - width / 2;
            case HEAT_COLD -> a / 2 - width / 2;
            default -> -1;
        };
    }

    private int heatSweetSpotWidth(int metalQuality) {
        var a = 1000 / 3;
        var b = a - a / 2;
        return a - scale(metalQuality, 130, b);
    }

    private int scale(int value, int divisor, int multiplier) {
        return (multiplier * value) / divisor;
    }
}

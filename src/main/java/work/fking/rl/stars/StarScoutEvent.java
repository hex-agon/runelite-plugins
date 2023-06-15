package work.fking.rl.stars;

import java.time.Duration;

/**
 * An event that is fired when a shooting star is scouted by looking at a telescope.
 */
public class StarScoutEvent {

    private final int world;
    private final StarRegion region;
    private final Duration earliest;
    private final Duration latest;

    private StarScoutEvent(int world, StarRegion region, Duration earliest, Duration latest) {
        this.world = world;
        this.region = region;
        this.earliest = earliest;
        this.latest = latest;
    }

    public static StarScoutEvent of(int world, StarRegion region, Duration earliest, Duration latest) {
        return new StarScoutEvent(world, region, earliest, latest);
    }

    public int world() {
        return world;
    }

    public StarRegion region() {
        return region;
    }

    public Duration earliest() {
        return earliest;
    }

    public Duration latest() {
        return latest;
    }
}

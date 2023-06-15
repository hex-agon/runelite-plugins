package work.fking.rl.stars;

import java.time.Duration;
import java.time.LocalDateTime;

public class ScoutedStar {

    private final int world;
    private final StarRegion region;
    private LocalDateTime earliestTime;
    private LocalDateTime latestTime;

    private ScoutedStar(int world, StarRegion region, LocalDateTime earliestTime, LocalDateTime latestTime) {
        this.world = world;
        this.region = region;
        this.earliestTime = earliestTime;
        this.latestTime = latestTime;
    }

    public static ScoutedStar from(StarScoutEvent event) {
        LocalDateTime now = LocalDateTime.now();

        return new ScoutedStar(event.world(), event.region(), now.plus(event.earliest()), now.plus(event.latest()));
    }

    public int world() {
        return world;
    }

    public StarRegion region() {
        return region;
    }

    public LocalDateTime earliestTime() {
        return earliestTime;
    }

    public LocalDateTime latestTime() {
        return latestTime;
    }

    public void updateTimes(LocalDateTime earliest, LocalDateTime latest) {
        this.earliestTime = earliest;
        this.latestTime = latest;
    }

    public boolean hasLanded() {
        return LocalDateTime.now().isAfter(latestTime);
    }

    public boolean expired() {
        var now = LocalDateTime.now();
        return now.isAfter(latestTime) && Math.abs(Duration.between(now, latestTime).toMinutesPart()) > 30;
    }
}

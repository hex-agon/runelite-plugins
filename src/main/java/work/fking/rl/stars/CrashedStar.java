package work.fking.rl.stars;

import net.runelite.api.coords.WorldPoint;
import work.fking.rl.stars.StarRegion.LandingSite;

import java.util.Objects;

/**
 * Represents a shooting star that has landed
 */
public class CrashedStar {

    public static final int MIN_TIER = 1;

    private final int world;
    private final WorldPoint worldPoint;
    private final LandingSite landingSite;
    private int tier;

    private CrashedStar(int world, WorldPoint worldPoint, LandingSite landingSite, int tier) {
        this.world = world;
        this.worldPoint = worldPoint;
        this.landingSite = landingSite;
        this.tier = tier;
    }

    public static CrashedStar of(int world, WorldPoint worldPoint, LandingSite landingSite, int tier) {
        return new CrashedStar(world, worldPoint, landingSite, tier);
    }

    public int world() {
        return world;
    }

    public WorldPoint worldPoint() {
        return worldPoint;
    }

    public int tier() {
        return tier;
    }

    public void updateTier(int tier) {
        this.tier = tier;
    }

    public LandingSite landingSite() {
        return landingSite;
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, worldPoint);
    }

    @Override
    public String toString() {
        return "CrashedStar{" +
                "world=" + world +
                ", worldPoint=" + worldPoint +
                ", landingSite=" + landingSite +
                ", tier=" + tier +
                '}';
    }
}

package work.fking.rl.stars;

import com.google.common.primitives.Ints;
import com.google.inject.name.Named;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemID;
import net.runelite.api.ObjectID;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import work.fking.rl.stars.StarRegion.LandingSite;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.runelite.client.util.ColorUtil.wrapWithColorTag;

@PluginDescriptor(
        name = "Shooting Stars",
        description = "Helps you track shooting stars",
        enabledByDefault = false
)
public class ShootingStars extends Plugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShootingStars.class);

    private static final int MINIMUM_EVICTION_DISTANCE = 28;
    private static final int HINT_ARROW_CLEAR_DISTANCE = 6;

    private static final int CHATBOX_MESSAGE_INTERFACE = 229;
    private static final int CHATBOX_MESSAGE_COMPONENT = 1;

    private static final int[] CRASHED_STARS = {
            ObjectID.CRASHED_STAR_41229,
            ObjectID.CRASHED_STAR_41228,
            ObjectID.CRASHED_STAR_41227,
            ObjectID.CRASHED_STAR_41226,
            ObjectID.CRASHED_STAR_41225,
            ObjectID.CRASHED_STAR_41224,
            ObjectID.CRASHED_STAR_41223,
            ObjectID.CRASHED_STAR_41021,
            ObjectID.CRASHED_STAR
    };

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private WorldMapPointManager worldMapPointManager;

    @Inject
    private ItemManager itemManager;

    @Inject
    private ShootingStarsOverlay overlay;

    @Inject
    private ChatMessageManager chatMessageManager;

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private EventBus eventBus;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    @Named("developerMode")
    private boolean developerMode;

    private ScoutedStar currentScout;
    private CrashedStar currentStar;

    private final List<PossibleLandingSite> possibleSites = new ArrayList<>();
    private final List<ScoutedStar> scouts = new ArrayList<>();

    private BufferedImage worldMapImage;
    private ShootingStarsPanel starsPanel;
    private NavigationButton navigationButton;

    private int previousWorld;

    public CrashedStar crashedStar() {
        return currentStar;
    }

    @Override
    protected void startUp() {
        overlayManager.add(overlay);
        starsPanel = new ShootingStarsPanel();

        BufferedImage icon = ImageUtil.loadImageResource(ShootingStars.class, "/panel_icon.png");

        navigationButton = NavigationButton.builder()
                                           .tooltip("Shooting Stars")
                                           .icon(icon)
                                           .priority(2)
                                           .panel(starsPanel)
                                           .build();

        clientToolbar.addNavigation(navigationButton);
        starsPanel.buildScoutedStarComponents(scouts);
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(overlay);
        clientToolbar.removeNavigation(navigationButton);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        var gameState = event.getGameState();
        if (gameState == GameState.LOGGING_IN || (gameState == GameState.LOGGED_IN && previousWorld != client.getWorld())) {
            previousWorld = client.getWorld();
            resetStarTrackingState();

            scouts.stream()
                  .filter(scout -> scout.world() == client.getWorld())
                  .findFirst()
                  .ifPresent(this::setupScoutedStar);
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        var localPlayer = client.getLocalPlayer();

        if (localPlayer == null) {
            return;
        }

        if (currentStar == null) {
            evaluatePossibleCrashSites(localPlayer);
            return;
        }
        if (client.hasHintArrow() && localPlayer.getWorldLocation().distanceTo(currentStar.worldPoint()) < HINT_ARROW_CLEAR_DISTANCE) {
            client.clearHintArrow();
        }
        if (!possibleSites.isEmpty()) {
            LOGGER.info("Clearing possible landing sites as we've found the star already.");
            clearPossibleSites();
        }
    }

    @Subscribe
    public void onClientTick(ClientTick event) {
        if (client.getGameCycle() % 250 == 0) {
            // update panel every 500th client cycle (10 seconds)
            scouts.removeIf(ScoutedStar::expired);
            starsPanel.buildScoutedStarComponents(scouts);
        }
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event) {
        if (event.getGroupId() != CHATBOX_MESSAGE_INTERFACE) {
            return;
        }
        clientThread.invokeLater(this::parseTelescopeWidget);
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event) {
        int starTier = getStarTier(event.getGameObject().getId());

        if (starTier <= 0) {
            return;
        }
        var worldPoint = event.getTile().getWorldLocation();

        if (currentStar == null || currentStar.world() != client.getWorld() || !Objects.equals(currentStar.worldPoint(), worldPoint)) {
            var landingSite = StarRegion.LANDING_SITES.stream()
                                                      .filter(l -> l.location().equals(worldPoint))
                                                      .findFirst()
                                                      .orElse(null);

            var crashedStar = CrashedStar.of(client.getWorld(), worldPoint, landingSite, starTier);
            handleNewSpottedStar(crashedStar);
        } else if (starTier < currentStar.tier()) {
            currentStar.updateTier(starTier);
            handleDowngradedStar(currentStar);
        } else {
            LOGGER.warn("New shooting star loc spawned but not handled");
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event) {
        var starTier = getStarTier(event.getGameObject().getId());

        if (starTier <= 0) {
            return;
        }

        if (currentStar != null && currentStar.tier() == CrashedStar.MIN_TIER) {
            handleDepletedStar(currentStar);
        }
    }

    @Subscribe
    public void onCommandExecuted(CommandExecuted commandExecuted) {
        if (!developerMode) {
            return;
        }
        switch (commandExecuted.getCommand()) {
            case "ssregion":
                handleRegionCommand(commandExecuted.getArguments());
                break;
            case "ssclear":
                handleClearCommand(commandExecuted.getArguments());
                break;
            case "sscouts":
                handleSetupScoutsCommand(commandExecuted.getArguments());
                break;
        }
    }

    private void handleRegionCommand(String[] args) {
        if (args.length < 1) {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Insufficient parameters, usage: ssregion <regionname>", null);
            return;
        }
        var region = args[0].toUpperCase();
        try {
            var starRegion = StarRegion.valueOf(region);
            resetStarTrackingState();
            setupPossibleCrashSites(starRegion);
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Star region set to: " + starRegion, null);
        } catch (IllegalArgumentException e) {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "No star region could be found with name: " + region, null);
        }
    }

    private void handleClearCommand(String[] args) {
        resetStarTrackingState();
        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Cleared current crashed star", null);
    }

    private void handleSetupScoutsCommand(String[] args) {
        if (scouts.isEmpty()) {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Cleared current crashed star", null);
        } else {
            for (var scout : scouts) {
                LocalTime earliestTime = scout.earliestTime().toLocalTime().truncatedTo(ChronoUnit.SECONDS);
                LocalTime latestTime = scout.latestTime().toLocalTime().truncatedTo(ChronoUnit.SECONDS);
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", scout.world() + " - " + scout.region() + "(" + earliestTime + " <-> " + latestTime + ")", null);
            }
        }
    }

    private void evaluatePossibleCrashSites(Player localPlayer) {
        var iterator = possibleSites.iterator();
        while (iterator.hasNext()) {
            var possibleSite = iterator.next();
            var worldPoint = possibleSite.landingSite.location();

            if (!worldPoint.isInScene(client) || worldPoint.distanceTo(localPlayer.getWorldLocation()) >= MINIMUM_EVICTION_DISTANCE || checkForShootingStar(worldPoint)) {
                continue;
            }
            QueuedMessage queuedMessage = QueuedMessage.builder()
                                                       .type(ChatMessageType.GAMEMESSAGE)
                                                       .runeLiteFormattedMessage(ColorUtil.wrapWithColorTag("No shooting star was found, ignoring nearby crash site.", Color.ORANGE))
                                                       .build();

            chatMessageManager.queue(queuedMessage);
            LOGGER.info("Removing possible crash site as no star was found at {}", worldPoint);
            worldMapPointManager.remove(possibleSite.worldMapPoint);
            iterator.remove();
        }
    }

    private boolean checkForShootingStar(WorldPoint worldPoint) {
        var localPoint = LocalPoint.fromWorld(client, worldPoint);

        if (localPoint == null) {
            return false;
        }
        var tile = client.getScene().getTiles()[client.getPlane()][localPoint.getSceneX()][localPoint.getSceneY()];

        for (var gameObject : tile.getGameObjects()) {
            if (gameObject != null && isShootingStar(gameObject.getId())) {
                return true;
            }
        }
        return false;
    }

    private boolean isShootingStar(int locId) {
        return getStarTier(locId) > 0;
    }

    @Subscribe
    public void onStarScoutEvent(StarScoutEvent event) {
        var scoutedStar = ScoutedStar.from(event);

        scouts.removeIf(scout -> scout.world() == event.world());
        scouts.add(scoutedStar);
        starsPanel.buildScoutedStarComponents(scouts);

        if (scoutedStar.world() == client.getWorld()) {
            setupScoutedStar(scoutedStar);
        }
    }

    private void setupScoutedStar(ScoutedStar scoutedStar) {
        currentScout = scoutedStar;
        var region = scoutedStar.region();
        setupPossibleCrashSites(region);

        var now = LocalDateTime.now();
        var color = client.getTopLevelInterfaceId() == WidgetID.FIXED_VIEWPORT_GROUP_ID ? Color.BLUE : Color.CYAN;

        var messageBuilder = new ChatMessageBuilder()
                .append("A shooting star has been scouted in this world. ");

        if (now.isBefore(scoutedStar.earliestTime())) {
            var earliest = Duration.between(now, scoutedStar.earliestTime());
            var latest = Duration.between(now, scoutedStar.latestTime());

            messageBuilder
                    .append("It'll land at ")
                    .append(color, region.shortName())
                    .append(" in approximately ")
                    .append(color, formatDuration(earliest))
                    .append(" to ")
                    .append(color, formatDuration(latest))
                    .append(". ");
        } else if (now.isBefore(scoutedStar.latestTime())) {
            var latest = Duration.between(now, scoutedStar.latestTime());
            messageBuilder.append(" it'll at ")
                          .append(color, region.shortName())
                          .append(" in at most ")
                          .append(color, formatDuration(latest))
                          .append(". ");
        } else {
            messageBuilder.append(" it has landed at ")
                          .append(color, region.shortName())
                          .append(" by now. ");
        }

        String chatMessage = messageBuilder.append("Open your")
                                           .append(color, " world map ")
                                           .append("to see possible landing sites.")
                                           .build();

        var queuedMessage = QueuedMessage.builder()
                                         .type(ChatMessageType.GAMEMESSAGE)
                                         .runeLiteFormattedMessage(chatMessage)
                                         .build();

        chatMessageManager.queue(queuedMessage);
    }

    private String formatDuration(Duration duration) {
        var builder = new StringBuilder();
        var seconds = duration.getSeconds();
        var minutes = ((seconds % 3600) / 60);
        var hours = seconds / 3600;

        if (hours > 0) {
            builder.append(hours).append(" hour");
            if (hours > 1) {
                builder.append('s');
            }
            builder.append(' ');
        }
        builder.append(minutes).append(" minutes");
        return builder.toString();
    }

    private void parseTelescopeWidget() {
        int world = client.getWorld();
        var widget = client.getWidget(CHATBOX_MESSAGE_INTERFACE, CHATBOX_MESSAGE_COMPONENT);

        if (widget == null) {
            return;
        }
        var region = TelescopeParser.extractStarRegion(widget.getText());
        if (region == null) {
            return;
        }
        var earliest = TelescopeParser.extractEarliestDuration(widget.getText());
        var latest = TelescopeParser.extractLatestDuration(widget.getText());
        eventBus.post(StarScoutEvent.of(world, region, earliest, latest));
    }

    /**
     * Returns the shooting star tier for a given locId (gameObjectId). Returns -1 if the loc isn't a shooting star.
     *
     * @param locId The locId (gameObjectId).
     * @return The star tier or -1 if the loc isn't a shooting star.
     */
    private int getStarTier(int locId) {
        return Ints.indexOf(CRASHED_STARS, locId) + 1;
    }

    /**
     * Resets all the plugin state to the default values.
     */
    private void resetStarTrackingState() {
        currentStar = null;
        clearPossibleSites();
    }

    private void clearPossibleSites() {
        possibleSites.stream()
                     .map(PossibleLandingSite::worldMapPoint)
                     .forEach(worldMapPointManager::remove);
        possibleSites.clear();
    }

    /**
     * Sets up the world map with the possible crash sites for the given region.
     *
     * @param region The region where the star will land in.
     */
    private void setupPossibleCrashSites(StarRegion region) {
        clearPossibleSites();
        var landingSites = region.landingSites();

        if (worldMapImage == null) {
            worldMapImage = createWorldMapImage();
        }

        for (var landingSite : landingSites) {
            var mapPoint = WorldMapPoint.builder()
                                        .worldPoint(landingSite.location())
                                        .name("Landing site")
                                        .tooltip(landingSite.name())
                                        .image(worldMapImage)
                                        .jumpOnClick(true)
                                        .snapToEdge(true)
                                        .build();

            worldMapPointManager.add(mapPoint);
            possibleSites.add(PossibleLandingSite.of(landingSite, mapPoint));
        }
    }

    private void handleNewSpottedStar(CrashedStar star) {
        LOGGER.info("New shooting star spotted {}", star);
        var queuedMessage = QueuedMessage.builder()
                                         .type(ChatMessageType.GAMEMESSAGE)
                                         .runeLiteFormattedMessage(wrapWithColorTag("A shooting star has been spotted nearby!", Color.CYAN))
                                         .build();

        chatMessageManager.queue(queuedMessage);
        client.setHintArrow(star.worldPoint());
        currentStar = star;
    }

    private void handleDowngradedStar(CrashedStar star) {
        LOGGER.info("Shooting star degraded to tier {}, world={}, worldPoint={}", star.tier(), star.world(), star.worldPoint());
    }

    private void handleDepletedStar(CrashedStar star) {
        LOGGER.info("Shooting star depleted, world={}, worldPoint={}", star.world(), star.worldPoint());
        resetStarTrackingState();
    }

    private BufferedImage createWorldMapImage() {
        var background = ImageUtil.loadImageResource(ShootingStars.class, "/util/clue_arrow.png");
        var starFragment = itemManager.getImage(ItemID.STAR_FRAGMENT);

        var image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
        var graphics = image.getGraphics();

        graphics.drawImage(background, 0, 0, null);
        graphics.drawImage(starFragment, 0, 0, null);
        return image;
    }

    private static class PossibleLandingSite {

        private final LandingSite landingSite;
        private final WorldMapPoint worldMapPoint;

        public PossibleLandingSite(LandingSite landingSite, WorldMapPoint worldMapPoint) {
            this.landingSite = landingSite;
            this.worldMapPoint = worldMapPoint;
        }

        public static PossibleLandingSite of(LandingSite landingSite, WorldMapPoint worldMapPoint) {
            return new PossibleLandingSite(landingSite, worldMapPoint);
        }

        public LandingSite landingSite() {
            return landingSite;
        }

        public WorldMapPoint worldMapPoint() {
            return worldMapPoint;
        }
    }
}

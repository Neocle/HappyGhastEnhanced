package fr.neocle.happyghastenhanced.manager;

import fr.neocle.happyghastenhanced.HappyGhastEnhanced;
import fr.neocle.happyghastenhanced.util.Logger;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.HappyGhast;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CameraManager {
    private final HappyGhastEnhanced plugin;
    private final Logger logger;

    private final Map<UUID, Double> originalCameraDistances = new ConcurrentHashMap<>();

    public CameraManager(HappyGhastEnhanced plugin) {
        this.plugin = plugin;
        this.logger = plugin.getCustomLogger();
    }

    public void onPlayerMount(Player player, HappyGhast ghast) {
        if (!ghast.isValid() || ghast.isDead()) {
            return;
        }

        UUID ghastId = ghast.getUniqueId();

        if (ghast.getAttribute(Attribute.CAMERA_DISTANCE) != null) {
            double originalDistance = Objects.requireNonNull(ghast.getAttribute(Attribute.CAMERA_DISTANCE)).getBaseValue();
            originalCameraDistances.put(ghastId, originalDistance);

            double configuredDistance = plugin.getConfigManager().getCameraDistance();
            Objects.requireNonNull(ghast.getAttribute(Attribute.CAMERA_DISTANCE)).setBaseValue(configuredDistance);

            logger.debug("Set camera distance for ghast " + ghastId + " from " + originalDistance + " to " + configuredDistance);
        } else {
            logger.warning("HappyGhast missing CAMERA_DISTANCE attribute!");
        }
    }

    public void onPlayerDismount(Player player, HappyGhast ghast) {
        if (!ghast.isValid() || ghast.isDead()) {
            cleanup(ghast);
            return;
        }

        UUID ghastId = ghast.getUniqueId();
        Double originalDistance = originalCameraDistances.get(ghastId);

        if (originalDistance != null && ghast.getAttribute(Attribute.CAMERA_DISTANCE) != null) {
            Objects.requireNonNull(ghast.getAttribute(Attribute.CAMERA_DISTANCE)).setBaseValue(originalDistance);
            originalCameraDistances.remove(ghastId);

            logger.debug("Restored camera distance for ghast " + ghastId + " to " + originalDistance);
        }
    }

    public void onGhastRemove(HappyGhast ghast) {
        cleanup(ghast);
    }

    private void cleanup(HappyGhast ghast) {
        UUID ghastId = ghast.getUniqueId();
        originalCameraDistances.remove(ghastId);
        logger.debug("Cleaned up camera data for ghast " + ghastId);
    }

    public void shutdown() {
        originalCameraDistances.clear();
        logger.debug("CameraManager shutdown completed");
    }
}
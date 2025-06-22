package fr.neocle.happyghastenhanced.manager;

import fr.neocle.happyghastenhanced.HappyGhastEnhanced;
import fr.neocle.happyghastenhanced.util.Logger;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.HappyGhast;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SpeedManager {
    private final HappyGhastEnhanced plugin;
    private final Logger logger;

    private final Map<UUID, HappyGhast> playerToGhast = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> ghastToPlayers = new ConcurrentHashMap<>();

    private BukkitRunnable speedCheckTask;

    public SpeedManager(HappyGhastEnhanced plugin) {
        this.plugin = plugin;
        this.logger = plugin.getCustomLogger();
    }

    public void addRider(Player player, HappyGhast ghast) {
        UUID playerId = player.getUniqueId();
        UUID ghastId = ghast.getUniqueId();

        playerToGhast.put(playerId, ghast);
        ghastToPlayers.computeIfAbsent(ghastId, k -> ConcurrentHashMap.newKeySet()).add(playerId);

        updateGhastSpeed(ghast);

        logger.debug("Added rider " + player.getName() + " to ghast " + ghastId +
                ". Total riders: " + getRiderCount(ghast));
    }

    public void removeRider(Player player, HappyGhast ghast) {
        UUID playerId = player.getUniqueId();
        UUID ghastId = ghast.getUniqueId();

        playerToGhast.remove(playerId);
        Set<UUID> riders = ghastToPlayers.get(ghastId);
        if (riders != null) {
            riders.remove(playerId);
            if (riders.isEmpty()) {
                ghastToPlayers.remove(ghastId);
                resetGhastSpeed(ghast);
            }
        }

        updateGhastSpeed(ghast);

        logger.debug("Removed rider " + player.getName() + " from ghast " + ghastId +
                ". Remaining riders: " + getRiderCount(ghast));
    }

    public void removePlayerFromAllGhasts(Player player) {
        UUID playerId = player.getUniqueId();
        HappyGhast ghast = playerToGhast.remove(playerId);

        if (ghast != null) {
            UUID ghastId = ghast.getUniqueId();
            Set<UUID> riders = ghastToPlayers.get(ghastId);
            if (riders != null) {
                riders.remove(playerId);
                if (riders.isEmpty()) {
                    ghastToPlayers.remove(ghastId);
                    resetGhastSpeed(ghast);
                } else {
                    updateGhastSpeed(ghast);
                }
            }
        }
    }

    public HappyGhast getPlayerGhast(Player player) {
        return playerToGhast.get(player.getUniqueId());
    }

    public int getRiderCount(HappyGhast ghast) {
        Set<UUID> riders = ghastToPlayers.get(ghast.getUniqueId());
        return riders != null ? riders.size() : 0;
    }

    public void updateGhastSpeed(HappyGhast ghast) {
        if (!ghast.isValid() || ghast.isDead()) {
            cleanup(ghast);
            return;
        }

        Set<UUID> riderIds = ghastToPlayers.get(ghast.getUniqueId());
        if (riderIds == null || riderIds.isEmpty()) {
            resetGhastSpeed(ghast);
            return;
        }

        boolean anyPlayerHasTriggerItem = false;
        Material triggerItem = plugin.getConfigManager().getTriggerItem();

        for (UUID riderId : riderIds) {
            Player rider = plugin.getServer().getPlayer(riderId);
            if (rider != null && rider.isOnline()) {
                if (isPlayerHoldingTriggerItem(rider, triggerItem)) {
                    anyPlayerHasTriggerItem = true;
                    break;
                }
            }
        }

        double targetSpeed = anyPlayerHasTriggerItem ?
                plugin.getConfigManager().getBoostedSpeed() :
                plugin.getConfigManager().getDefaultSpeed();

        setGhastSpeed(ghast, targetSpeed);

        logger.debug("Updated ghast speed to " + targetSpeed + " (boost: " + anyPlayerHasTriggerItem +
                ") for " + riderIds.size() + " riders");
    }

    private boolean isPlayerHoldingTriggerItem(Player player, Material triggerItem) {
        return player.getInventory().getItemInMainHand().getType() == triggerItem ||
                player.getInventory().getItemInOffHand().getType() == triggerItem;
    }

    private void setGhastSpeed(HappyGhast ghast, double targetSpeed) {
        if (ghast.getAttribute(Attribute.FLYING_SPEED) != null) {
            double currentSpeed = Objects.requireNonNull(ghast.getAttribute(Attribute.FLYING_SPEED)).getBaseValue();

            if (Math.abs(currentSpeed - targetSpeed) > 0.001) {
                Objects.requireNonNull(ghast.getAttribute(Attribute.FLYING_SPEED)).setBaseValue(targetSpeed);
                logger.debug("Speed updated: " + currentSpeed + " -> " + targetSpeed);
            }
        } else {
            logger.warning("HappyGhast missing FLYING_SPEED attribute!");
        }
    }

    private void resetGhastSpeed(HappyGhast ghast) {
        if (ghast != null && ghast.isValid() && !ghast.isDead()) {
            double defaultSpeed = plugin.getConfigManager().getDefaultSpeed();
            setGhastSpeed(ghast, defaultSpeed);
            logger.debug("Reset HappyGhast speed to " + defaultSpeed);
        }
    }

    private void cleanup(HappyGhast ghast) {
        UUID ghastId = ghast.getUniqueId();
        Set<UUID> riders = ghastToPlayers.remove(ghastId);

        if (riders != null) {
            for (UUID riderId : riders) {
                playerToGhast.remove(riderId);
            }
            logger.debug("Cleaned up invalid ghast " + ghastId + " with " + riders.size() + " riders");
        }
    }

    public void startSpeedCheckTask() {
        speedCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!playerToGhast.isEmpty()) {
                    logger.performance("Speed check task running for " + playerToGhast.size() + " players on " +
                            ghastToPlayers.size() + " ghasts");
                }

                Iterator<Map.Entry<UUID, HappyGhast>> playerIterator = playerToGhast.entrySet().iterator();
                while (playerIterator.hasNext()) {
                    Map.Entry<UUID, HappyGhast> entry = playerIterator.next();
                    Player player = plugin.getServer().getPlayer(entry.getKey());
                    HappyGhast ghast = entry.getValue();

                    if (player == null || !player.isOnline()) {
                        logger.debug("Removing offline player from tracking: " + entry.getKey());
                        UUID playerId = entry.getKey();
                        HappyGhast playerGhast = entry.getValue();
                        playerIterator.remove();

                        Set<UUID> riders = ghastToPlayers.get(playerGhast.getUniqueId());
                        if (riders != null) {
                            riders.remove(playerId);
                            if (riders.isEmpty()) {
                                ghastToPlayers.remove(playerGhast.getUniqueId());
                                resetGhastSpeed(playerGhast);
                            } else {
                                updateGhastSpeed(playerGhast);
                            }
                        }
                    } else if (!ghast.isValid() || ghast.isDead()) {
                        logger.debug("Removing invalid ghast from tracking");
                        cleanup(ghast);
                    } else {
                        updateGhastSpeed(ghast);
                    }
                }
            }
        };
        speedCheckTask.runTaskTimer(plugin, 20L, 20L);
        logger.debug("Speed check task started");
    }

    public void shutdown() {
        if (speedCheckTask != null) {
            speedCheckTask.cancel();
        }

        for (HappyGhast ghast : new HashSet<>(playerToGhast.values())) {
            resetGhastSpeed(ghast);
        }

        playerToGhast.clear();
        ghastToPlayers.clear();
        logger.debug("GhastSpeedManager shutdown completed");
    }
}
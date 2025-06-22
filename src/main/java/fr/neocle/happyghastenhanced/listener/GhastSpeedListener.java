package fr.neocle.happyghastenhanced.listener;

import fr.neocle.happyghastenhanced.HappyGhastEnhanced;
import fr.neocle.happyghastenhanced.util.Logger;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.HappyGhast;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class GhastSpeedListener implements Listener {

    private final HappyGhastEnhanced plugin;
    private final Logger logger;
    private final Map<UUID, HappyGhast> ridingGhasts = new HashMap<>();
    private BukkitRunnable speedCheckTask;

    public GhastSpeedListener(HappyGhastEnhanced plugin) {
        this.plugin = plugin;
        this.logger = plugin.getCustomLogger();
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent event) {
        logger.debug("VehicleEnterEvent: " + event.getVehicle().getType() + " entered by " + event.getEntered().getType());

        if (event.getEntered() instanceof Player player && event.getVehicle() instanceof HappyGhast ghast) {
            ridingGhasts.put(player.getUniqueId(), ghast);
            logger.debug("Player " + player.getName() + " mounted HappyGhast. Total riding: " + ridingGhasts.size());
        }
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        logger.debug("VehicleExitEvent: " + event.getVehicle().getType() + " exited by " + event.getExited().getType());

        if (event.getExited() instanceof Player player) {
            HappyGhast ghast = ridingGhasts.remove(player.getUniqueId());
            if (ghast != null) {
                logger.debug("Player " + player.getName() + " dismounted HappyGhast. Resetting speed.");
                resetGhastSpeed(ghast);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        HappyGhast ghast = ridingGhasts.remove(event.getPlayer().getUniqueId());
        if (ghast != null) {
            logger.debug("Player " + event.getPlayer().getName() + " quit while riding HappyGhast. Resetting speed.");
            resetGhastSpeed(ghast);
        }
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        HappyGhast ghast = ridingGhasts.get(player.getUniqueId());

        if (ghast != null) {
            Material newItem = player.getInventory().getItem(event.getNewSlot()) != null ?
                    player.getInventory().getItem(event.getNewSlot()).getType() : Material.AIR;
            logger.debug("Player " + player.getName() + " changed held item to: " + newItem);

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                updateGhastSpeed(player, ghast);
            }, 1L);
        }
    }

    public void startSpeedCheckTask() {
        speedCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!ridingGhasts.isEmpty()) {
                    logger.performance("Speed check task running for " + ridingGhasts.size() + " players");
                }

                ridingGhasts.entrySet().removeIf(entry -> {
                    Player player = plugin.getServer().getPlayer(entry.getKey());
                    if (player != null && player.isOnline()) {
                        updateGhastSpeed(player, entry.getValue());
                        return false;
                    } else {
                        logger.debug("Removing offline player from tracking: " + entry.getKey());
                        return true;
                    }
                });
            }
        };
        speedCheckTask.runTaskTimer(plugin, 20L, 20L);
        logger.debug("Speed check task started");
    }

    private void updateGhastSpeed(Player player, HappyGhast ghast) {
        if (!ghast.isValid() || ghast.isDead()) {
            logger.debug("HappyGhast invalid/dead, removing from tracking: " + player.getName());
            ridingGhasts.remove(player.getUniqueId());
            return;
        }

        Material triggerItem = plugin.getConfigManager().getTriggerItem();
        boolean holdingTriggerItem = player.getInventory().getItemInMainHand().getType() == triggerItem ||
                player.getInventory().getItemInOffHand().getType() == triggerItem;

        logger.debug("Player " + player.getName() + " holding " + triggerItem + ": " + holdingTriggerItem);

        double targetSpeed = holdingTriggerItem ?
                plugin.getConfigManager().getBoostedSpeed() :
                plugin.getConfigManager().getDefaultSpeed();

        if (ghast.getAttribute(Attribute.FLYING_SPEED) != null) {
            double currentSpeed = Objects.requireNonNull(ghast.getAttribute(Attribute.FLYING_SPEED)).getBaseValue();

            if (Math.abs(currentSpeed - targetSpeed) > 0.001) {
                Objects.requireNonNull(ghast.getAttribute(Attribute.FLYING_SPEED)).setBaseValue(targetSpeed);
                logger.debug("Speed updated: " + currentSpeed + " -> " + targetSpeed + " for " + player.getName());
            }
        } else {
            logger.warning("HappyGhast missing FLYING_SPEED attribute!");
        }
    }

    private void resetGhastSpeed(HappyGhast ghast) {
        if (ghast != null && ghast.isValid() && !ghast.isDead() &&
                ghast.getAttribute(Attribute.FLYING_SPEED) != null) {
            double defaultSpeed = plugin.getConfigManager().getDefaultSpeed();
            Objects.requireNonNull(ghast.getAttribute(Attribute.FLYING_SPEED)).setBaseValue(defaultSpeed);
            logger.debug("Reset HappyGhast speed to " + defaultSpeed);
        }
    }

    public void cleanup() {
        if (speedCheckTask != null) {
            speedCheckTask.cancel();
        }

        for (Map.Entry<UUID, HappyGhast> entry : ridingGhasts.entrySet()) {
            resetGhastSpeed(entry.getValue());
        }
        ridingGhasts.clear();
        logger.debug("Cleaned up all tracked ghasts");
    }
}
package fr.neocle.happyghastenhanced.listener;

import fr.neocle.happyghastenhanced.HappyGhastEnhanced;
import fr.neocle.happyghastenhanced.manager.CameraManager;
import fr.neocle.happyghastenhanced.manager.SpeedManager;
import fr.neocle.happyghastenhanced.util.Logger;
import org.bukkit.entity.HappyGhast;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

public class GhastMountListener implements Listener {

    private final HappyGhastEnhanced plugin;
    private final Logger logger;
    private final SpeedManager speedManager;
    private final CameraManager cameraManager;

    public GhastMountListener(HappyGhastEnhanced plugin, SpeedManager speedManager, CameraManager cameraManager) {
        this.plugin = plugin;
        this.logger = plugin.getCustomLogger();
        this.speedManager = speedManager;
        this.cameraManager = cameraManager;
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent event) {
        logger.debug("VehicleEnterEvent: " + event.getVehicle().getType() + " entered by " + event.getEntered().getType());

        if (event.getEntered() instanceof Player player && event.getVehicle() instanceof HappyGhast ghast) {
            speedManager.addRider(player, ghast);
            cameraManager.onPlayerMount(player, ghast);

            logger.debug("Player " + player.getName() + " mounted HappyGhast. Total riders on this ghast: " +
                    speedManager.getRiderCount(ghast));
        }
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        logger.debug("VehicleExitEvent: " + event.getVehicle().getType() + " exited by " + event.getExited().getType());

        if (event.getExited() instanceof Player player && event.getVehicle() instanceof HappyGhast ghast) {
            speedManager.removeRider(player, ghast);

            if (speedManager.getRiderCount(ghast) == 0) {
                cameraManager.onPlayerDismount(player, ghast);
            }

            logger.debug("Player " + player.getName() + " dismounted HappyGhast. Remaining riders: " +
                    speedManager.getRiderCount(ghast));
        }
    }
}
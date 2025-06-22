package fr.neocle.happyghastenhanced.listener;

import fr.neocle.happyghastenhanced.HappyGhastEnhanced;
import fr.neocle.happyghastenhanced.manager.SpeedManager;
import fr.neocle.happyghastenhanced.util.Logger;
import org.bukkit.Material;
import org.bukkit.entity.HappyGhast;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;

public class ItemHeldListener implements Listener {

    private final HappyGhastEnhanced plugin;
    private final Logger logger;
    private final SpeedManager speedManager;

    public ItemHeldListener(HappyGhastEnhanced plugin, SpeedManager speedManager) {
        this.plugin = plugin;
        this.logger = plugin.getCustomLogger();
        this.speedManager = speedManager;
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        HappyGhast ghast = speedManager.getPlayerGhast(player);

        if (ghast != null) {
            Material newItem = player.getInventory().getItem(event.getNewSlot()) != null ?
                    player.getInventory().getItem(event.getNewSlot()).getType() : Material.AIR;
            logger.debug("Player " + player.getName() + " changed held item to: " + newItem);

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                speedManager.updateGhastSpeed(ghast);
            }, 1L);
        }
    }
}
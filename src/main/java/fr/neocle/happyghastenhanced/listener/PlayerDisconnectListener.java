package fr.neocle.happyghastenhanced.listener;

import fr.neocle.happyghastenhanced.HappyGhastEnhanced;
import fr.neocle.happyghastenhanced.manager.SpeedManager;
import fr.neocle.happyghastenhanced.util.Logger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerDisconnectListener implements Listener {

    private final HappyGhastEnhanced plugin;
    private final Logger logger;
    private final SpeedManager speedManager;

    public PlayerDisconnectListener(HappyGhastEnhanced plugin, SpeedManager speedManager) {
        this.plugin = plugin;
        this.logger = plugin.getCustomLogger();
        this.speedManager = speedManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        speedManager.removePlayerFromAllGhasts(player);
        logger.debug("Player " + player.getName() + " disconnected and removed from all ghast tracking");
    }
}
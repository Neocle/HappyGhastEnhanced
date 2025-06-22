package fr.neocle.happyghastenhanced;

import fr.neocle.happyghastenhanced.config.ConfigManager;
import fr.neocle.happyghastenhanced.listener.GhastBuildingListener;
import fr.neocle.happyghastenhanced.listener.GhastMountListener;
import fr.neocle.happyghastenhanced.listener.ItemHeldListener;
import fr.neocle.happyghastenhanced.listener.PlayerDisconnectListener;
import fr.neocle.happyghastenhanced.manager.CameraManager;
import fr.neocle.happyghastenhanced.manager.SpeedManager;
import fr.neocle.happyghastenhanced.util.Logger;
import net.kyori.adventure.text.Component;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class HappyGhastEnhanced extends JavaPlugin {

    private ConfigManager configManager;
    private Logger logger;
    private SpeedManager speedManager;
    private CameraManager cameraManager;

    private GhastMountListener mountListener;
    private PlayerDisconnectListener disconnectListener;
    private ItemHeldListener itemHeldListener;
    private GhastBuildingListener buildingListener;

    @Override
    public void onEnable() {
        this.logger = new Logger(this);
        this.configManager = new ConfigManager(this);
        this.speedManager = new SpeedManager(this);
        this.cameraManager = new CameraManager(this);

        this.mountListener = new GhastMountListener(this, speedManager, cameraManager);
        this.disconnectListener = new PlayerDisconnectListener(this, speedManager);
        this.itemHeldListener = new ItemHeldListener(this, speedManager);
        this.buildingListener = new GhastBuildingListener(this);

        configManager.loadConfig();

        getServer().getPluginManager().registerEvents(mountListener, this);
        getServer().getPluginManager().registerEvents(disconnectListener, this);
        getServer().getPluginManager().registerEvents(itemHeldListener, this);
        getServer().getPluginManager().registerEvents(buildingListener, this);

        speedManager.startSpeedCheckTask();

        logger.info(configManager.getPlainMessage("plugin-enabled"));
        logger.config("Default speed: " + configManager.getDefaultSpeed());
        logger.config("Boosted speed: " + configManager.getBoostedSpeed());
        logger.config("Trigger item: " + configManager.getTriggerItem());
        logger.config("Camera distance: " + configManager.getCameraDistance());

        int pluginId = 26246;
        Metrics metrics = new Metrics(this, pluginId);
    }

    @Override
    public void onDisable() {
        if (speedManager != null) {
            speedManager.shutdown();
        }
        if (cameraManager != null) {
            cameraManager.shutdown();
        }
        logger.info(configManager.getPlainMessage("plugin-disabled"));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String @NotNull [] args) {
        if (command.getName().equalsIgnoreCase("hge")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("happyghastenhanced.reload")) {
                    Component message = configManager.getMessage("no-permission");
                    sender.sendMessage(message);
                    return true;
                }

                configManager.loadConfig();
                Component message = configManager.getMessage("config-reloaded");
                sender.sendMessage(message);
                logger.config("Configuration reloaded by " + sender.getName());
                return true;
            } else {
                Component infoMessage = configManager.getMessage("plugin-info", "{version}", getDescription().getVersion());
                Component usageMessage = configManager.getMessage("command-usage");
                sender.sendMessage(infoMessage);
                sender.sendMessage(usageMessage);
                return true;
            }
        }
        return false;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public Logger getCustomLogger() {
        return logger;
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }
}
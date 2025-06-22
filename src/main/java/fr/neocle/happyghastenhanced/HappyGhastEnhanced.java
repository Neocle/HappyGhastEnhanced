package fr.neocle.happyghastenhanced;

import fr.neocle.happyghastenhanced.config.ConfigManager;
import fr.neocle.happyghastenhanced.listener.GhastSpeedListener;
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
    private GhastSpeedListener ghastSpeedListener;

    @Override
    public void onEnable() {
        this.logger = new Logger(this);
        this.configManager = new ConfigManager(this);
        this.ghastSpeedListener = new GhastSpeedListener(this);

        configManager.loadConfig();
        getServer().getPluginManager().registerEvents(ghastSpeedListener, this);
        ghastSpeedListener.startSpeedCheckTask();

        logger.info(configManager.getPlainMessage("plugin-enabled"));
        logger.config("Default speed: " + configManager.getDefaultSpeed());
        logger.config("Boosted speed: " + configManager.getBoostedSpeed());
        logger.config("Trigger item: " + configManager.getTriggerItem());

        int pluginId = 26246;
        Metrics metrics = new Metrics(this, pluginId);
    }

    @Override
    public void onDisable() {
        if (ghastSpeedListener != null) {
            ghastSpeedListener.cleanup();
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
}
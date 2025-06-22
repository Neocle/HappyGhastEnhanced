package fr.neocle.happyghastenhanced.util;

import java.util.logging.Level;
import fr.neocle.happyghastenhanced.HappyGhastEnhanced;

public class Logger {

    private final HappyGhastEnhanced plugin;
    private final java.util.logging.Logger bukkitLogger;
    private final String prefix;

    public Logger(HappyGhastEnhanced plugin) {
        this.plugin = plugin;
        this.bukkitLogger = plugin.getLogger();
        this.prefix = "[HappyGhastEnhanced] ";
    }

    public void info(String message) {
        bukkitLogger.info(message);
    }

    public void warning(String message) {
        bukkitLogger.warning(message);
    }

    public void severe(String message) {
        bukkitLogger.severe(message);
    }

    public void debug(String message) {
        if (plugin.getConfigManager() != null && plugin.getConfigManager().isDebugEnabled()) {
            bukkitLogger.info("[DEBUG] " + message);
        }
    }

    public void logException(String context, Exception e) {
        severe(context + ": " + e.getMessage());
        if (plugin.getConfigManager() != null && plugin.getConfigManager().isDebugEnabled()) {
            bukkitLogger.log(Level.SEVERE, "Stack trace for: " + context, e);
        }
    }

    public void config(String message) {
        info("[CONFIG] " + message);
    }

    public void performance(String message) {
        if (plugin.getConfigManager() != null && plugin.getConfigManager().isDebugEnabled()) {
            info("[PERFORMANCE] " + message);
        }
    }
}
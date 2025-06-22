package fr.neocle.happyghastenhanced.config;

import fr.neocle.happyghastenhanced.HappyGhastEnhanced;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final HappyGhastEnhanced plugin;
    private final MiniMessage miniMessage;
    private FileConfiguration config;

    private double defaultSpeed;
    private double boostedSpeed;
    private double cameraDistance;
    private Material triggerItem;
    private boolean debugEnabled;
    private boolean allowBuildingOnGhast;

    public ConfigManager(HappyGhastEnhanced plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        defaultSpeed = config.getDouble("speeds.default", 0.05);
        boostedSpeed = config.getDouble("speeds.boosted", 0.15);
        cameraDistance = config.getDouble("camera-distance", 8.0);
        debugEnabled = config.getBoolean("debug", false);
        allowBuildingOnGhast = config.getBoolean("allow-building-on-ghast", true);

        String itemName = config.getString("trigger-item", "SNOWBALL");
        try {
            triggerItem = Material.valueOf(itemName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning(getPlainMessage("invalid-trigger-item", "{item}", itemName));
            triggerItem = Material.SNOWBALL;
        }

        validateConfig();
    }

    private void validateConfig() {
        if (defaultSpeed < 0 || defaultSpeed > 1) {
            plugin.getLogger().warning(getPlainMessage("speed-out-of-range",
                    "{type}", "Default",
                    "{value}", String.valueOf(defaultSpeed),
                    "{default}", "0.05"));
            defaultSpeed = 0.05;
        }

        if (boostedSpeed < 0 || boostedSpeed > 1) {
            plugin.getLogger().warning(getPlainMessage("speed-out-of-range",
                    "{type}", "Boosted",
                    "{value}", String.valueOf(boostedSpeed),
                    "{default}", "0.15"));
            boostedSpeed = 0.15;
        }

        if (boostedSpeed <= defaultSpeed) {
            plugin.getLogger().warning(getPlainMessage("boosted-speed-warning"));
            boostedSpeed = defaultSpeed + 0.1;
        }
    }

    public String getRawMessage(String key) {
        return config.getString("messages." + key, "<red>Message not found: " + key + "</red>");
    }

    public Component getMessage(String key) {
        return miniMessage.deserialize(getRawMessage(key));
    }

    public Component getMessage(String key, String... replacements) {
        String message = getRawMessage(key);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }
        return miniMessage.deserialize(message);
    }

    public String getPlainMessage(String key, String... replacements) {
        Component component = getMessage(key, replacements);
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component);
    }

    public double getDefaultSpeed() {
        return defaultSpeed;
    }

    public double getBoostedSpeed() {
        return boostedSpeed;
    }

    public double getCameraDistance() {
        return cameraDistance;
    }

    public Material getTriggerItem() {
        return triggerItem;
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public boolean isAllowBuildingOnGhast() {
        return allowBuildingOnGhast;
    }
}
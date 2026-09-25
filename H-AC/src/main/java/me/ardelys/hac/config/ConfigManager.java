package me.ardelys.hac.config;

import me.ardelys.hac.HAC;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ConfigManager {

    private final HAC plugin;
    private FileConfiguration config;

    private String prefix;
    private boolean debug;
    private boolean alertsEnabled;
    private String alertFormat;
    private String alertPermission;
    private double minVlToAlert;

    private int maxPing;
    private double minTps;
    private int teleportGraceTicks;
    private int respawnGraceTicks;
    private int velocityGraceTicks;

    private int decayIntervalSeconds;
    private double decayAmount;
    private double defaultPunishmentThreshold;
    private String defaultPunishmentCommand;

    private String languageDefault;
    private String languageFallback;

    public ConfigManager(HAC plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        migrateLegacyDataFolder();
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        this.prefix = config.getString("settings.prefix", "&8[&c&lH-AC&8]&r ");
        this.debug = config.getBoolean("settings.debug", false);

        this.alertsEnabled = config.getBoolean("alerts.enabled", true);
        this.alertFormat = config.getString("alerts.format",
                "&8[&c&lH-AC&8] &c%player% &7failed &e%check% &8(&7%type%&8) &7| &fVL: &c%vl% &7| &fPing: &a%ping%ms &7| &fTPS: &a%tps%");
        this.alertPermission = config.getString("alerts.permission", "hac.alerts");
        this.minVlToAlert = Math.max(0.0, config.getDouble("alerts.min-vl-to-alert", 3.0));

        this.maxPing = Math.max(50, config.getInt("exemptions.max-ping", 350));
        this.minTps = Math.max(5.0, config.getDouble("exemptions.min-tps", 18.0));
        this.teleportGraceTicks = Math.max(0, config.getInt("exemptions.teleport-grace-ticks", 20));
        this.respawnGraceTicks = Math.max(0, config.getInt("exemptions.respawn-grace-ticks", 35));
        this.velocityGraceTicks = Math.max(0, config.getInt("exemptions.velocity-grace-ticks", 20));

        this.decayIntervalSeconds = Math.max(1, config.getInt("violations.decay-interval-seconds", 10));
        this.decayAmount = Math.max(0.01, config.getDouble("violations.decay-amount", 0.5));
        this.defaultPunishmentThreshold = Math.max(1.0, config.getDouble("violations.default-punishment-threshold", 20.0));
        this.defaultPunishmentCommand = config.getString("violations.default-punishment-command",
                "kick %player% [H-AC] Unfair Advantage Detected (%check% %type%)");

        this.languageDefault = config.getString("language.default", "tr_TR");
        this.languageFallback = config.getString("language.fallback", "en_US");
    }

    private void migrateLegacyDataFolder() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            File parent = dataFolder.getParentFile();
            if (parent != null) {
                File legacyFolder = new File(parent, "HukumAC");
                if (legacyFolder.exists() && legacyFolder.isDirectory()) {
                    plugin.getLogger().info("[H-AC] Migrating legacy configuration from plugins/HukumAC to plugins/H-AC...");
                    try {
                        copyDirectory(legacyFolder, dataFolder);
                        plugin.getLogger().info("[H-AC] Legacy configuration successfully migrated.");
                    } catch (Throwable t) {
                        plugin.getLogger().warning("[H-AC] Failed to migrate legacy configuration: " + t.getMessage());
                    }
                }
            }
        }
    }

    private void copyDirectory(File source, File destination) throws IOException {
        if (source.isDirectory()) {
            if (!destination.exists()) {
                destination.mkdirs();
            }
            String[] files = source.list();
            if (files != null) {
                for (String file : files) {
                    File srcFile = new File(source, file);
                    File destFile = new File(destination, file);
                    copyDirectory(srcFile, destFile);
                }
            }
        } else {
            Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public String getLanguageDefault() {
        return languageDefault;
    }

    public String getLanguageFallback() {
        return languageFallback;
    }

    public boolean isCheckEnabled(String checkName) {
        if (checkName == null) return false;
        return config.getBoolean("checks." + checkName.toLowerCase() + ".enabled", true);
    }

    public double getCheckThreshold(String checkName) {
        if (checkName == null) return defaultPunishmentThreshold;
        return config.getDouble("checks." + checkName.toLowerCase() + ".threshold", defaultPunishmentThreshold);
    }

    public String getCheckPunishmentCommand(String checkName) {
        if (checkName == null) return defaultPunishmentCommand;
        return config.getString("checks." + checkName.toLowerCase() + ".command", defaultPunishmentCommand);
    }

    public double getDouble(String path, double defaultValue) {
        return config.getDouble(path, defaultValue);
    }

    public int getInt(String path, int defaultValue) {
        return config.getInt(path, defaultValue);
    }

    public boolean getBoolean(String path, boolean defaultValue) {
        return config.getBoolean(path, defaultValue);
    }

    public String getString(String path, String defaultValue) {
        return config.getString(path, defaultValue);
    }

    public String getPrefix() {
        return prefix;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        config.set("settings.debug", debug);
        plugin.saveConfig();
    }

    public boolean isAlertsEnabled() {
        return alertsEnabled;
    }

    public String getAlertFormat() {
        return alertFormat;
    }

    public String getAlertPermission() {
        return alertPermission;
    }

    public double getMinVlToAlert() {
        return minVlToAlert;
    }

    public int getMaxPing() {
        return maxPing;
    }

    public double getMinTps() {
        return minTps;
    }

    public int getTeleportGraceTicks() {
        return teleportGraceTicks;
    }

    public int getRespawnGraceTicks() {
        return respawnGraceTicks;
    }

    public int getVelocityGraceTicks() {
        return velocityGraceTicks;
    }

    public int getDecayIntervalSeconds() {
        return decayIntervalSeconds;
    }

    public double getDecayAmount() {
        return decayAmount;
    }

    public double getDefaultPunishmentThreshold() {
        return defaultPunishmentThreshold;
    }

    public String getDefaultPunishmentCommand() {
        return defaultPunishmentCommand;
    }

    public FileConfiguration getConfig() {
        return config;
    }
}

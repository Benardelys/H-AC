package me.ardelys.hac;

import me.ardelys.hac.alert.AlertManager;
import me.ardelys.hac.checks.CheckManager;
import me.ardelys.hac.command.HACCommand;
import me.ardelys.hac.config.ConfigManager;
import me.ardelys.hac.data.PlayerDataManager;
import me.ardelys.hac.discord.DiscordWebhookManager;
import me.ardelys.hac.language.LanguageManager;
import me.ardelys.hac.listener.*;
import me.ardelys.hac.logging.AsyncLogger;
import me.ardelys.hac.violation.PunishmentManager;
import me.ardelys.hac.violation.ViolationManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class HAC extends JavaPlugin {

    private static HAC instance;

    private long startTime;
    private ConfigManager configManager;
    private LanguageManager languageManager;
    private AsyncLogger asyncLogger;
    private PlayerDataManager playerDataManager;
    private AlertManager alertManager;
    private PunishmentManager punishmentManager;
    private ViolationManager violationManager;
    private ServerTickListener serverTickListener;
    private CheckManager checkManager;
    private DiscordWebhookManager discordWebhookManager;
    private me.ardelys.hac.client.ClientDetector clientDetector;
    private me.ardelys.hac.client.injector.InjectorDetector injectorDetector;

    @Override
    public void onEnable() {
        instance = this;
        this.startTime = System.currentTimeMillis();

        this.configManager = new ConfigManager(this);
        this.languageManager = new LanguageManager(this);

        this.asyncLogger = new AsyncLogger(this);
        this.asyncLogger.start();

        this.playerDataManager = new PlayerDataManager();
        this.alertManager = new AlertManager(this);
        this.punishmentManager = new PunishmentManager(this);
        this.violationManager = new ViolationManager(this);
        this.serverTickListener = new ServerTickListener(this);

        this.checkManager = new CheckManager(this);
        this.discordWebhookManager = new DiscordWebhookManager(this);
        this.discordWebhookManager.sendStartup();

        this.clientDetector = new me.ardelys.hac.client.ClientDetector(this);
        this.injectorDetector = new me.ardelys.hac.client.injector.InjectorDetector(this);

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerConnectionListener(this), this);
        pm.registerEvents(new PlayerMoveListener(this), this);
        pm.registerEvents(new PlayerCombatListener(this), this);
        pm.registerEvents(new PlayerInteractListener(this), this);
        pm.registerEvents(new PlayerBlockListener(this), this);
        pm.registerEvents(this.clientDetector, this);

        try {
            ClientBrandListener brandListener = new ClientBrandListener(this);
            getServer().getMessenger().registerIncomingPluginChannel(this, "minecraft:brand", brandListener);
        } catch (Throwable t) {
            getLogger().warning("Could not register minecraft:brand channel: " + t.getMessage());
        }

        HACCommand cmd = new HACCommand(this);
        boolean registered = false;
        try {
            PluginCommand hacCmd = getCommand("hac");
            if (hacCmd == null) {
                hacCmd = getCommand("hukumac");
            }
            if (hacCmd != null) {
                hacCmd.setExecutor(cmd);
                hacCmd.setTabCompleter(cmd);
                registered = true;
            }
        } catch (Throwable ignored) {
        }

        if (!registered) {
            registerDynamicCommand(cmd);
        }

        getLogger().info("==================================================");
        getLogger().info(" H-AC v" + getPluginMeta().getVersion() + " initialized successfully!");
        getLogger().info(" Author: Ardelys | Target: HükümCraft (Paper 26.3)");
        getLogger().info(" Loaded " + checkManager.getAllChecks().size() + " detection checks.");
        getLogger().info(" Discord Webhook: " + (discordWebhookManager.isEnabled() ? (discordWebhookManager.isConfigured() ? "Enabled" : "Config Error") : "Disabled"));
        getLogger().info("==================================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling H-AC and cleaning up resources...");

        if (injectorDetector != null) {
            injectorDetector.stop();
        }

        if (discordWebhookManager != null) {
            discordWebhookManager.sendShutdown();
            discordWebhookManager.stop();
        }

        if (violationManager != null) {
            violationManager.stop();
        }

        if (serverTickListener != null) {
            serverTickListener.stop();
        }

        if (punishmentManager != null) {
            punishmentManager.clear();
        }

        if (alertManager != null) {
            alertManager.clear();
        }

        if (playerDataManager != null) {
            playerDataManager.clear();
        }

        if (asyncLogger != null) {
            asyncLogger.stop();
        }

        try {
            getServer().getMessenger().unregisterIncomingPluginChannel(this, "minecraft:brand");
        } catch (Throwable ignored) {
        }

        instance = null;
        getLogger().info("H-AC successfully disabled.");
    }

    public static HAC getInstance() {
        return instance;
    }

    public long getStartTime() {
        return startTime;
    }

    public String getFormattedUptime() {
        long uptime = Math.max(0, System.currentTimeMillis() - startTime);
        long seconds = (uptime / 1000) % 60;
        long minutes = (uptime / (1000 * 60)) % 60;
        long hours = (uptime / (1000 * 60 * 60)) % 24;
        long days = uptime / (1000 * 60 * 60 * 24);
        if (days > 0) {
            return String.format("%dd %dh %dm %ds", days, hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else {
            return String.format("%dm %ds", minutes, seconds);
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public AsyncLogger getAsyncLogger() {
        return asyncLogger;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public AlertManager getAlertManager() {
        return alertManager;
    }

    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }

    public ViolationManager getViolationManager() {
        return violationManager;
    }

    public ServerTickListener getServerTickListener() {
        return serverTickListener;
    }

    public CheckManager getCheckManager() {
        return checkManager;
    }

    public DiscordWebhookManager getDiscordWebhookManager() {
        return discordWebhookManager;
    }

    public me.ardelys.hac.client.ClientDetector getClientDetector() {
        return clientDetector;
    }

    public me.ardelys.hac.client.injector.InjectorDetector getInjectorDetector() {
        return injectorDetector;
    }

    private void registerDynamicCommand(HACCommand cmd) {
        try {
            org.bukkit.command.Command command = new org.bukkit.command.Command(
                    "hac",
                    "Main command for H-AC Anti-Cheat",
                    "/hac [help|reload|alerts|debug|info|violations|status|webhook]",
                    java.util.List.of("hukumac", "anticheat")
            ) {
                @Override
                public boolean execute(@org.jetbrains.annotations.NotNull org.bukkit.command.CommandSender sender, @org.jetbrains.annotations.NotNull String commandLabel, @org.jetbrains.annotations.NotNull String[] args) {
                    return cmd.onCommand(sender, this, commandLabel, args);
                }

                @Override
                public @org.jetbrains.annotations.NotNull java.util.List<String> tabComplete(@org.jetbrains.annotations.NotNull org.bukkit.command.CommandSender sender, @org.jetbrains.annotations.NotNull String alias, @org.jetbrains.annotations.NotNull String[] args) {
                    java.util.List<String> result = cmd.onTabComplete(sender, this, alias, args);
                    return result != null ? result : java.util.Collections.emptyList();
                }
            };
            command.setPermission("hac.admin");
            getServer().getCommandMap().register("hac", command);
        } catch (Throwable t) {
            getLogger().warning("Failed to register dynamic command: " + t.getMessage());
        }
    }
}

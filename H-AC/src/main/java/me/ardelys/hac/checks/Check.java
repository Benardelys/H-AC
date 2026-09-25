package me.ardelys.hac.checks;

import me.ardelys.hac.HAC;
import me.ardelys.hac.data.PlayerData;
import org.bukkit.entity.Player;

public abstract class Check {

    protected final HAC plugin;
    private final String name;
    private final CheckType type;
    private final String description;
    private boolean enabled;
    private double threshold;
    private String punishmentCommand;

    public Check(HAC plugin) {
        this.plugin = plugin;

        CheckInfo info = getClass().getAnnotation(CheckInfo.class);
        if (info != null) {
            this.name = info.name();
            this.type = info.type();
            this.description = info.description();
            this.threshold = info.defaultThreshold();
        } else {
            this.name = getClass().getSimpleName();
            this.type = CheckType.PLAYER;
            this.description = "";
            this.threshold = 15.0;
        }

        reloadConfig();
    }

    public void reloadConfig() {
        this.enabled = plugin.getConfigManager().isCheckEnabled(this.name);
        this.threshold = plugin.getConfigManager().getCheckThreshold(this.name);
        this.punishmentCommand = plugin.getConfigManager().getCheckPunishmentCommand(this.name);
    }

    public void fail(PlayerData data, String subType, double addedVl, String details) {
        if (!enabled || data == null) {
            return;
        }

        Player player = data.getPlayer();
        if (player == null || !player.isOnline()) {
            return;
        }

        if (player.hasPermission("hac.bypass")
                || player.hasPermission("hukumac.bypass")
                || player.hasPermission("hac.admin")
                || player.hasPermission("hukumac.admin")) {
            return;
        }

        double tps = plugin.getServerTickListener().getRecentTps();
        boolean isCritical = this.name.equals("BadPackets")
                || this.name.equals("ImpossibleActions")
                || this.name.equals("Phase");

        if (tps < plugin.getConfigManager().getMinTps() && !isCritical) {
            return;
        }

        int ping = data.getPing();
        if (ping > plugin.getConfigManager().getMaxPing() && !isCritical) {
            return;
        }

        if (Double.isNaN(addedVl) || Double.isInfinite(addedVl) || addedVl <= 0.0) {
            addedVl = 1.0;
        }

        data.recordCategoryViolation(this.type);
        if (plugin.getClientDetector() != null) {
            plugin.getClientDetector().onCategoryViolation(data, this.type);
        }
        if (plugin.getInjectorDetector() != null) {
            plugin.getInjectorDetector().onCheckViolation(data, this, subType, addedVl);
        }
        DetectionConfidence confidence = data.calculateConfidence(this.type, tps, ping);
        double weightedVl = Math.max(0.1, addedVl * confidence.getMultiplier());

        double newVl = plugin.getViolationManager().addViolation(data, this, subType, weightedVl, details);

        if (newVl >= plugin.getConfigManager().getMinVlToAlert()) {
            plugin.getAlertManager().sendAlert(player, this.name, subType, newVl, ping, tps, details, confidence);
            if (plugin.getDiscordWebhookManager() != null) {
                plugin.getDiscordWebhookManager().sendDetection(player, this.name, subType, newVl, ping, tps, details, confidence);
            }
        }

        plugin.getAsyncLogger().logViolation(player.getName(), this.name, subType, newVl, ping, tps, details + " [" + confidence.getDisplayName() + "]");

        if (newVl >= threshold) {
            plugin.getPunishmentManager().punish(data, this, subType, punishmentCommand);
        }
    }

    public void debug(PlayerData data, String message) {
        if (plugin.getConfigManager().isDebug() && data != null && data.getPlayer() != null && message != null) {
            plugin.getAlertManager().sendDebug(data.getPlayer(), "[" + name + "] " + message);
        }
    }

    public String getName() {
        return name;
    }

    public CheckType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public String getPunishmentCommand() {
        return punishmentCommand;
    }
}

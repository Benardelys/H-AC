package me.ardelys.hac.alert;

import me.ardelys.hac.HAC;
import me.ardelys.hac.language.LanguagePlaceholder;
import me.ardelys.hac.utils.TimeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AlertManager {

    private final HAC plugin;
    private final Set<UUID> disabledAlerts = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> lastAlertTimes = new ConcurrentHashMap<>();

    public AlertManager(HAC plugin) {
        this.plugin = plugin;
    }

    public boolean toggleAlerts(Player player) {
        if (player == null) return false;
        UUID uuid = player.getUniqueId();
        if (disabledAlerts.contains(uuid)) {
            disabledAlerts.remove(uuid);
            return true;
        } else {
            disabledAlerts.add(uuid);
            return false;
        }
    }

    public boolean hasAlertsEnabled(Player player) {
        if (player == null || !player.isOnline()) return false;
        return !disabledAlerts.contains(player.getUniqueId())
                && (player.hasPermission(plugin.getConfigManager().getAlertPermission())
                || player.hasPermission("hac.alerts")
                || player.hasPermission("hukumac.alerts")
                || player.hasPermission("hac.admin")
                || player.hasPermission("hukumac.admin"));
    }

    public void sendAlert(Player player, String check, String type, double vl, int ping, double tps, String details) {
        sendAlert(player, check, type, vl, ping, tps, details, null);
    }

    public void sendAlert(Player player, String check, String type, double vl, int ping, double tps, String details, me.ardelys.hac.checks.DetectionConfidence confidence) {
        if (player == null || !plugin.getConfigManager().isAlertsEnabled()) {
            return;
        }

        String safeDetails = details != null ? details : "N/A";
        String safeCheck = check != null ? check : "Unknown";
        String safeType = type != null ? type : "A";
        String confStr = confidence != null ? confidence.getDisplayName() : "Medium";

        long now = TimeUtil.now();
        if (lastAlertTimes.size() > 1000) {
            lastAlertTimes.entrySet().removeIf(entry -> now - entry.getValue() > 10000L);
        }

        String cooldownKey = player.getUniqueId() + ":" + safeCheck.toLowerCase();
        Long lastTime = lastAlertTimes.get(cooldownKey);
        if (lastTime != null && (now - lastTime) < 600L) {
            return;
        }
        lastAlertTimes.put(cooldownKey, now);

        Map<String, Object> placeholders = LanguagePlaceholder.builder()
                .add("player", player.getName())
                .add("check", safeCheck)
                .add("check_name", safeCheck)
                .add("check_type", safeType)
                .add("vl", String.format("%.1f", vl))
                .add("ping", Math.max(0, ping))
                .add("tps", String.format("%.1f", tps))
                .add("reason", safeDetails)
                .add("confidence", confStr)
                .build();

        Component alertComponent = plugin.getLanguageManager().getComponent("alerts.staff-format", placeholders);
        Component hoverComponent = plugin.getLanguageManager().getComponent("alerts.hover-text", placeholders);

        String safePlayerName = player.getName().replaceAll("[^a-zA-Z0-9_]", "");
        Component finalComponent = alertComponent
                .hoverEvent(HoverEvent.showText(hoverComponent))
                .clickEvent(ClickEvent.runCommand("/tp " + safePlayerName));

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (hasAlertsEnabled(staff)) {
                staff.sendMessage(finalComponent);
            }
        }
    }

    public void cleanupPlayer(UUID uuid) {
        if (uuid == null) return;
        disabledAlerts.remove(uuid);
        String prefix = uuid.toString() + ":";
        lastAlertTimes.keySet().removeIf(k -> k.startsWith(prefix));
    }

    public void clear() {
        disabledAlerts.clear();
        lastAlertTimes.clear();
    }

    public void sendDebug(Player target, String message) {
        if (target == null || message == null) return;

        Map<String, Object> placeholders = LanguagePlaceholder.builder()
                .add("player", target.getName())
                .add("details", message)
                .build();

        Component debugComp = plugin.getLanguageManager().getComponent("alerts.debug-format", placeholders);

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("hac.debug") || staff.hasPermission("hukumac.debug")
                    || staff.hasPermission("hac.admin") || staff.hasPermission("hukumac.admin")) {
                staff.sendMessage(debugComp);
            }
        }
    }
}

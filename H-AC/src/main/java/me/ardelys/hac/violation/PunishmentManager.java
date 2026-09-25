package me.ardelys.hac.violation;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.Check;
import me.ardelys.hac.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PunishmentManager {

    private final HAC plugin;
    private final Set<UUID> pendingPunishments = ConcurrentHashMap.newKeySet();

    public PunishmentManager(HAC plugin) {
        this.plugin = plugin;
    }

    public void punish(PlayerData data, Check check, String subType, String rawCommand) {
        if (data == null || check == null || rawCommand == null) {
            return;
        }

        Player player = data.getPlayer();
        if (player == null || !player.isOnline()) {
            return;
        }

        UUID uuid = player.getUniqueId();
        if (!pendingPunishments.add(uuid)) {
            return;
        }

        String safePlayerName = player.getName().replaceAll("[^a-zA-Z0-9_]", "");
        String safeCheckName = check.getName().replaceAll("[^a-zA-Z0-9_]", "");
        String safeSubType = (subType != null ? subType : "").replaceAll("[\r\n;&|]", "");

        String command = rawCommand
                .replace("%player%", safePlayerName)
                .replace("%check%", safeCheckName)
                .replace("%type%", safeSubType);

        plugin.getLogger().warning("[H-AC] Punishing " + safePlayerName + " for " + safeCheckName + " (" + safeSubType + ")");

        if (plugin.getDiscordWebhookManager() != null) {
            String action = rawCommand.toLowerCase().startsWith("ban") ? "Ban" : "Kick";
            plugin.getDiscordWebhookManager().sendPunishment(data, check, action, safeCheckName + " (" + safeSubType + ")");
        }

        if (!plugin.isEnabled()) {
            pendingPunishments.remove(uuid);
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            } finally {
                if (plugin.isEnabled()) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> pendingPunishments.remove(uuid), 60L);
                } else {
                    pendingPunishments.remove(uuid);
                }
            }
        });
    }

    public void remove(UUID uuid) {
        if (uuid != null) {
            pendingPunishments.remove(uuid);
        }
    }

    public void clear() {
        pendingPunishments.clear();
    }
}

package me.ardelys.hac.data;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    private final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();

    public PlayerData get(Player player) {
        if (player == null) return null;
        return get(player.getUniqueId());
    }

    public PlayerData get(UUID uuid) {
        if (uuid == null) return null;
        return playerDataMap.computeIfAbsent(uuid, PlayerData::new);
    }

    public PlayerData getIfPresent(Player player) {
        if (player == null) return null;
        return getIfPresent(player.getUniqueId());
    }

    public PlayerData getIfPresent(UUID uuid) {
        if (uuid == null) return null;
        return playerDataMap.get(uuid);
    }

    public void remove(UUID uuid) {
        if (uuid != null) {
            PlayerData data = playerDataMap.remove(uuid);
            if (data != null) {
                data.clearViolations();
                data.cleanReferences();
            }
        }
    }

    public Collection<PlayerData> getAll() {
        return playerDataMap.values();
    }

    public void clear() {
        for (PlayerData data : playerDataMap.values()) {
            data.cleanReferences();
            data.clearViolations();
        }
        playerDataMap.clear();
    }
}

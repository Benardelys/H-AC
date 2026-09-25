package me.ardelys.hac.violation;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.Check;
import me.ardelys.hac.data.PlayerData;
import me.ardelys.hac.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ViolationManager {

    private final HAC plugin;
    private final Map<UUID, Deque<Violation>> violationHistory = new ConcurrentHashMap<>();
    private final Map<UUID, CachedProfile> offlineProfiles = new ConcurrentHashMap<>();
    private final Object historyLock = new Object();
    private BukkitTask decayTask;

    public record CachedProfile(Map<String, Double> violations, Deque<Violation> history, long timestamp) {}

    public ViolationManager(HAC plugin) {
        this.plugin = plugin;
        startDecayTask();
    }

    public void startDecayTask() {
        if (decayTask != null && !decayTask.isCancelled()) {
            decayTask.cancel();
        }

        int intervalSeconds = Math.max(1, plugin.getConfigManager().getDecayIntervalSeconds());
        long intervalTicks = intervalSeconds * 20L;

        decayTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            double decayAmount = plugin.getConfigManager().getDecayAmount();
            if (Double.isNaN(decayAmount) || Double.isInfinite(decayAmount) || decayAmount <= 0.0) {
                return;
            }
            for (PlayerData data : plugin.getPlayerDataManager().getAll()) {
                if (data != null) {
                    data.decayViolations(decayAmount);
                }
            }

            long now = TimeUtil.now();
            long maxAge = 600000L;
            offlineProfiles.entrySet().removeIf(entry -> now - entry.getValue().timestamp() > maxAge);
        }, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (decayTask != null) {
            decayTask.cancel();
            decayTask = null;
        }
        synchronized (historyLock) {
            violationHistory.clear();
            offlineProfiles.clear();
        }
    }

    public double addViolation(PlayerData data, Check check, String subType, double addedVl, String details) {
        if (data == null || check == null) {
            return 0.0;
        }

        if (Double.isNaN(addedVl) || Double.isInfinite(addedVl) || addedVl <= 0.0) {
            addedVl = 1.0;
        }

        double current = data.getViolationLevel(check.getName());
        if (Double.isNaN(current) || Double.isInfinite(current)) {
            current = 0.0;
        }

        double updated = Math.min(1000.0, current + addedVl);
        data.setViolationLevel(check.getName(), updated);

        Violation violation = new Violation(
                check,
                subType != null ? subType : "General",
                addedVl,
                updated,
                TimeUtil.now(),
                details != null ? details : "Suspicious activity detected"
        );

        synchronized (historyLock) {
            Deque<Violation> history = violationHistory.computeIfAbsent(data.getUuid(), k -> new ArrayDeque<>());
            history.addLast(violation);
            while (history.size() > 20) {
                history.removeFirst();
            }
        }

        return updated;
    }

    public List<Violation> getHistory(UUID uuid) {
        if (uuid == null) return Collections.emptyList();
        synchronized (historyLock) {
            Deque<Violation> history = violationHistory.get(uuid);
            if (history == null || history.isEmpty()) {
                return Collections.emptyList();
            }
            return new ArrayList<>(history);
        }
    }

    public void preservePlayerViolations(PlayerData data) {
        if (data == null) return;
        UUID uuid = data.getUuid();
        Map<String, Double> currentViolations = new HashMap<>(data.getViolations());
        synchronized (historyLock) {
            Deque<Violation> history = violationHistory.remove(uuid);
            if (!currentViolations.isEmpty() || (history != null && !history.isEmpty())) {
                offlineProfiles.put(uuid, new CachedProfile(currentViolations, history != null ? new ArrayDeque<>(history) : new ArrayDeque<>(), TimeUtil.now()));
            }
        }
    }

    public void restorePlayerViolations(PlayerData data) {
        if (data == null) return;
        UUID uuid = data.getUuid();
        CachedProfile profile = offlineProfiles.remove(uuid);
        if (profile != null && (TimeUtil.now() - profile.timestamp() < 600000L)) {
            data.setAllViolations(profile.violations());
            synchronized (historyLock) {
                if (profile.history() != null && !profile.history().isEmpty()) {
                    violationHistory.put(uuid, profile.history());
                }
            }
        }
    }

    public void clearHistory(UUID uuid) {
        if (uuid != null) {
            synchronized (historyLock) {
                violationHistory.remove(uuid);
                offlineProfiles.remove(uuid);
            }
        }
    }
}

package me.ardelys.hac.client.injector;

import me.ardelys.hac.checks.DetectionConfidence;
import me.ardelys.hac.utils.TimeUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ReconnectProtection {

    public record SuspiciousSnapshot(
            UUID uuid,
            String playerName,
            double score,
            DetectionConfidence confidence,
            List<String> modules,
            long timestamp
    ) {}

    private final Map<UUID, SuspiciousSnapshot> snapshots = new ConcurrentHashMap<>();
    private static final int MAX_SNAPSHOTS = 500;
    private static final long EXPIRATION_MS = 600000L;

    public void saveSnapshot(UUID uuid, String name, double score, DetectionConfidence confidence, List<String> modules) {
        if (uuid == null || score < 25.0) return;

        if (snapshots.size() >= MAX_SNAPSHOTS) {
            pruneOldest();
        }

        snapshots.put(uuid, new SuspiciousSnapshot(
                uuid,
                name != null ? name : uuid.toString(),
                score,
                confidence,
                modules != null ? new ArrayList<>(modules) : Collections.emptyList(),
                TimeUtil.now()
        ));
    }

    public SuspiciousSnapshot getSnapshot(UUID uuid) {
        if (uuid == null) return null;
        SuspiciousSnapshot snapshot = snapshots.get(uuid);
        if (snapshot == null) return null;

        if (TimeUtil.now() - snapshot.timestamp() > EXPIRATION_MS) {
            snapshots.remove(uuid);
            return null;
        }
        return snapshot;
    }

    public SuspiciousSnapshot getSnapshotByName(String name) {
        if (name == null || name.isEmpty()) return null;
        long now = TimeUtil.now();
        for (SuspiciousSnapshot snapshot : snapshots.values()) {
            if (snapshot.playerName().equalsIgnoreCase(name)) {
                if (now - snapshot.timestamp() > EXPIRATION_MS) {
                    snapshots.remove(snapshot.uuid());
                    return null;
                }
                return snapshot;
            }
        }
        return null;
    }

    public Collection<SuspiciousSnapshot> getAllSnapshots() {
        pruneExpired();
        return Collections.unmodifiableCollection(snapshots.values());
    }

    public void removeSnapshot(UUID uuid) {
        if (uuid != null) {
            snapshots.remove(uuid);
        }
    }

    public void pruneExpired() {
        long now = TimeUtil.now();
        snapshots.entrySet().removeIf(e -> now - e.getValue().timestamp() > EXPIRATION_MS);
    }

    private void pruneOldest() {
        UUID oldestKey = null;
        long oldestTime = Long.MAX_VALUE;
        for (Map.Entry<UUID, SuspiciousSnapshot> entry : snapshots.entrySet()) {
            if (entry.getValue().timestamp() < oldestTime) {
                oldestTime = entry.getValue().timestamp();
                oldestKey = entry.getKey();
            }
        }
        if (oldestKey != null) {
            snapshots.remove(oldestKey);
        }
    }

    public int size() {
        return snapshots.size();
    }

    public void clear() {
        snapshots.clear();
    }
}

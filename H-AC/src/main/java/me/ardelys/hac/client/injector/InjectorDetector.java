package me.ardelys.hac.client.injector;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.Check;
import me.ardelys.hac.checks.CheckType;
import me.ardelys.hac.checks.DetectionConfidence;
import me.ardelys.hac.data.PlayerData;
import me.ardelys.hac.utils.TimeUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InjectorDetector {

    public static class ModuleViolationRecord {
        private final String moduleName;
        private final CheckType category;
        private int violationCount;
        private double accumulatedScore;
        private long lastViolationTime;

        public ModuleViolationRecord(String moduleName, CheckType category, double score, long time) {
            this.moduleName = moduleName;
            this.category = category;
            this.violationCount = 1;
            this.accumulatedScore = score;
            this.lastViolationTime = time;
        }

        public void add(double score, long time) {
            this.violationCount++;
            this.accumulatedScore += score;
            this.lastViolationTime = time;
        }

        public String getModuleName() { return moduleName; }
        public CheckType getCategory() { return category; }
        public int getViolationCount() { return violationCount; }
        public double getAccumulatedScore() { return accumulatedScore; }
        public long getLastViolationTime() { return lastViolationTime; }
    }

    public static class TrackedInjectorState {
        private final UUID uuid;
        private String playerName;
        private final Map<String, ModuleViolationRecord> activeModules = new ConcurrentHashMap<>();
        private double totalScore = 0.0;
        private long lastDecayTime = TimeUtil.now();

        public TrackedInjectorState(UUID uuid, String playerName) {
            this.uuid = uuid;
            this.playerName = playerName;
        }

        public UUID getUuid() { return uuid; }
        public String getPlayerName() { return playerName; }
        public void setPlayerName(String playerName) { this.playerName = playerName; }
        public Map<String, ModuleViolationRecord> getActiveModules() { return activeModules; }
        public double getTotalScore() { return totalScore; }
        public void setTotalScore(double score) { this.totalScore = Math.max(0.0, Math.min(100.0, score)); }

        public void addScore(double amount) {
            this.totalScore = Math.max(0.0, Math.min(100.0, this.totalScore + amount));
        }

        public void decay(double amount, long now) {
            if (totalScore <= 0.0) return;
            totalScore = Math.max(0.0, totalScore - amount);
            lastDecayTime = now;
            activeModules.entrySet().removeIf(entry -> now - entry.getValue().getLastViolationTime() > 45000L);
        }

        public int getRecentActiveModuleCount(long maxAgeMs) {
            long now = TimeUtil.now();
            int count = 0;
            for (ModuleViolationRecord rec : activeModules.values()) {
                if (now - rec.getLastViolationTime() <= maxAgeMs) {
                    count++;
                }
            }
            return count;
        }

        public Set<CheckType> getRecentCategories(long maxAgeMs) {
            long now = TimeUtil.now();
            Set<CheckType> categories = new HashSet<>();
            for (ModuleViolationRecord rec : activeModules.values()) {
                if (now - rec.getLastViolationTime() <= maxAgeMs) {
                    categories.add(rec.getCategory());
                }
            }
            return categories;
        }

        public List<String> getActiveModuleNames(long maxAgeMs) {
            long now = TimeUtil.now();
            List<String> list = new ArrayList<>();
            for (ModuleViolationRecord rec : activeModules.values()) {
                if (now - rec.getLastViolationTime() <= maxAgeMs) {
                    list.add(rec.getModuleName());
                }
            }
            return list;
        }
    }

    private final HAC plugin;
    private final ReconnectProtection reconnectProtection;
    private final Map<UUID, TrackedInjectorState> trackedStates = new ConcurrentHashMap<>();
    private BukkitTask decayTask;

    private boolean enabled = true;
    private boolean autoKick = true;
    private DetectionConfidence autoKickConfidence = DetectionConfidence.CRITICAL;
    private String kickMessage = "H-AC has detected an unauthorized modified client.";
    private double baseThreshold = 85.0;
    private double decayAmount = 1.0;

    public InjectorDetector(HAC plugin) {
        this.plugin = plugin;
        this.reconnectProtection = new ReconnectProtection();
        loadConfiguration();
        startDecayTask();
    }

    public void loadConfiguration() {
        this.enabled = plugin.getConfig().getBoolean("injector-detection.enabled", true);
        this.autoKick = plugin.getConfig().getBoolean("injector-detection.auto-kick", true);
        String confStr = plugin.getConfig().getString("injector-detection.auto-kick-confidence", "CRITICAL").toUpperCase();
        try {
            this.autoKickConfidence = DetectionConfidence.valueOf(confStr);
        } catch (IllegalArgumentException e) {
            this.autoKickConfidence = DetectionConfidence.CRITICAL;
        }
        this.kickMessage = plugin.getConfig().getString("injector-detection.kick-message", "H-AC has detected an unauthorized modified client.");
        this.baseThreshold = plugin.getConfig().getDouble("injector-detection.base-threshold", 85.0);
        this.decayAmount = plugin.getConfig().getDouble("injector-detection.decay-amount", 1.0);
    }

    private void startDecayTask() {
        if (decayTask != null && !decayTask.isCancelled()) {
            decayTask.cancel();
        }
        decayTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = TimeUtil.now();
            for (TrackedInjectorState state : trackedStates.values()) {
                state.decay(decayAmount, now);
            }
            reconnectProtection.pruneExpired();
        }, 100L, 100L);
    }

    public void onCheckViolation(PlayerData data, Check check, String subType, double addedVl) {
        if (!enabled || data == null || check == null) return;

        Player player = data.getPlayer();
        if (player == null || !player.isOnline()) return;

        if (player.hasPermission("hac.bypass")
                || player.hasPermission("hukumac.bypass")
                || player.hasPermission("hac.admin")
                || player.hasPermission("hukumac.admin")) {
            return;
        }

        String checkName = check.getName();
        CheckType category = check.getType();
        String moduleName = mapCheckToCheatModule(checkName);

        TrackedInjectorState state = trackedStates.computeIfAbsent(player.getUniqueId(),
                u -> new TrackedInjectorState(u, player.getName()));
        state.setPlayerName(player.getName());

        long now = TimeUtil.now();
        state.getActiveModules().compute(moduleName, (k, existing) -> {
            if (existing == null) {
                return new ModuleViolationRecord(moduleName, category, addedVl, now);
            } else {
                existing.add(addedVl, now);
                return existing;
            }
        });

        int distinctModules = state.getRecentActiveModuleCount(30000L);
        Set<CheckType> distinctCategories = state.getRecentCategories(30000L);

        double synergyMultiplier = 1.0;
        if (distinctModules >= 4) {
            synergyMultiplier *= 2.5;
        } else if (distinctModules >= 3) {
            synergyMultiplier *= 1.9;
        } else if (distinctModules >= 2) {
            synergyMultiplier *= 1.4;
        }

        if (distinctCategories.size() >= 3) {
            synergyMultiplier *= 1.6;
        } else if (distinctCategories.size() >= 2) {
            synergyMultiplier *= 1.3;
        }

        double scoreInc = Math.min(15.0, addedVl * 1.5) * synergyMultiplier;
        state.addScore(scoreInc);

        double totalScore = state.getTotalScore();
        List<String> activeModules = state.getActiveModuleNames(30000L);
        List<String> signals = new ArrayList<>();

        for (String mod : activeModules) {
            signals.add("Module[" + mod + "]");
        }
        if (distinctCategories.size() >= 2) {
            signals.add("CrossCategory[" + distinctCategories.size() + "]");
        }
        if (distinctModules >= 2) {
            signals.add("Synergy[" + distinctModules + "mods]");
        }

        String brand = data.getClientBrand();
        if (brand != null && brand.equalsIgnoreCase("vanilla") && totalScore >= 40.0) {
            signals.add("BrandAnomaly[VanillaMask]");
        }

        DetectionConfidence confidence;
        if (totalScore >= baseThreshold && distinctModules >= 2) {
            confidence = DetectionConfidence.CRITICAL;
        } else if (totalScore >= 60.0) {
            confidence = DetectionConfidence.HIGH;
        } else if (totalScore >= 30.0) {
            confidence = DetectionConfidence.MEDIUM;
        } else {
            confidence = DetectionConfidence.LOW;
        }

        InjectorDetectionResult result = new InjectorDetectionResult(confidence, totalScore, activeModules, signals, now);
        data.setInjectorResult(result);

        if (confidence.ordinal() >= autoKickConfidence.ordinal()) {
            dispatchKick(player, data, result);
        } else if (confidence == DetectionConfidence.HIGH) {
            reconnectProtection.saveSnapshot(player.getUniqueId(), player.getName(), totalScore, confidence, activeModules);
        }
    }

    private void dispatchKick(Player player, PlayerData data, InjectorDetectionResult result) {
        if (player == null || !player.isOnline()) return;

        reconnectProtection.saveSnapshot(player.getUniqueId(), player.getName(), result.getScore(), result.getConfidence(), result.getDetectedModules());

        String details = "Injected Client Detected: score=" + String.format("%.1f", result.getScore())
                + ", modules=" + result.getFormattedModules()
                + ", signals=" + result.getFormattedSignals();

        plugin.getAsyncLogger().logViolation(player.getName(), "InjectorProtection", "ModifiedClient", result.getScore(),
                data.getPing(), plugin.getServerTickListener().getRecentTps(), details);

        plugin.getAlertManager().sendAlert(player, "InjectorProtection", "ModifiedClient", result.getScore(),
                data.getPing(), plugin.getServerTickListener().getRecentTps(), details, result.getConfidence());

        if (plugin.getDiscordWebhookManager() != null) {
            plugin.getDiscordWebhookManager().sendDetection(player, "InjectorProtection", "ModifiedClient", result.getScore(),
                    data.getPing(), plugin.getServerTickListener().getRecentTps(), details, result.getConfidence());
        }

        if (autoKick) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    player.kick(Component.text(kickMessage));
                }
            });
        }
    }

    public void handlePlayerJoin(Player player, PlayerData data) {
        if (!enabled || player == null) return;

        ReconnectProtection.SuspiciousSnapshot snapshot = reconnectProtection.getSnapshot(player.getUniqueId());
        if (snapshot != null) {
            TrackedInjectorState state = trackedStates.computeIfAbsent(player.getUniqueId(),
                    u -> new TrackedInjectorState(u, player.getName()));
            state.setPlayerName(player.getName());
            state.setTotalScore(snapshot.score() * 0.85);

            long now = TimeUtil.now();
            for (String mod : snapshot.modules()) {
                state.getActiveModules().put(mod, new ModuleViolationRecord(mod, CheckType.COMBAT, snapshot.score() * 0.2, now));
            }

            InjectorDetectionResult restoredResult = new InjectorDetectionResult(
                    snapshot.confidence(),
                    state.getTotalScore(),
                    snapshot.modules(),
                    List.of("RestoredFromReconnectSnapshot"),
                    now
            );
            data.setInjectorResult(restoredResult);

            String alertMsg = "§8[§c§lH-AC§8] §e§lRECONNECT ALERT: §c" + player.getName()
                    + " §7reconnected with prior injector suspicion (Score: §c" + String.format("%.1f", snapshot.score())
                    + "§7, Modules: §e" + String.join(", ", snapshot.modules()) + "§7)";

            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.hasPermission("hac.alerts") || staff.hasPermission("hac.admin")) {
                    staff.sendMessage(alertMsg);
                }
            }

            plugin.getAsyncLogger().logViolation(player.getName(), "ReconnectProtection", "SuspiciousReconnect", snapshot.score(),
                    data.getPing(), plugin.getServerTickListener().getRecentTps(), "Restored prior suspicion: " + String.join(", ", snapshot.modules()));
        }
    }

    public void handlePlayerQuit(Player player, PlayerData data) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        TrackedInjectorState state = trackedStates.get(uuid);
        if (state != null) {
            if (state.getTotalScore() >= 25.0) {
                List<String> modules = state.getActiveModuleNames(60000L);
                DetectionConfidence confidence = state.getTotalScore() >= baseThreshold ? DetectionConfidence.CRITICAL :
                        (state.getTotalScore() >= 60.0 ? DetectionConfidence.HIGH : DetectionConfidence.MEDIUM);
                reconnectProtection.saveSnapshot(uuid, player.getName(), state.getTotalScore(), confidence, modules);
            }
            trackedStates.remove(uuid);
        }
    }

    private String mapCheckToCheatModule(String checkName) {
        if (checkName == null) return "GenericModule";
        return switch (checkName.toLowerCase()) {
            case "aimassist", "aim" -> "AimAssist";
            case "autoclicker", "cps" -> "AutoClicker";
            case "killaura", "aura" -> "KillAura";
            case "reach" -> "Reach";
            case "triggerbot" -> "TriggerBot";
            case "velocity", "antiknockback" -> "Velocity";
            case "criticals", "crits" -> "Criticals";
            case "speed" -> "Speed";
            case "fly", "flight" -> "Fly";
            case "noslow", "noslowdown" -> "NoSlow";
            case "timer", "timercheck" -> "Timer";
            case "scaffold", "tower" -> "Scaffold";
            case "fastplace" -> "FastPlace";
            case "fastbreak" -> "FastBreak";
            case "badpackets" -> "BadPackets";
            case "impossibleactions" -> "ImpossibleActions";
            case "inventorymove" -> "InvMove";
            case "nofall" -> "NoFall";
            case "jesus", "waterwalk" -> "Jesus";
            case "step" -> "Step";
            case "phase" -> "Phase";
            default -> checkName;
        };
    }

    public TrackedInjectorState getTrackedState(UUID uuid) {
        if (uuid == null) return null;
        return trackedStates.get(uuid);
    }

    public ReconnectProtection getReconnectProtection() {
        return reconnectProtection;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void stop() {
        if (decayTask != null && !decayTask.isCancelled()) {
            decayTask.cancel();
            decayTask = null;
        }
        trackedStates.clear();
        reconnectProtection.clear();
    }
}

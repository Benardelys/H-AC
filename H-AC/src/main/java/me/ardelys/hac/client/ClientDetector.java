package me.ardelys.hac.client;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.CheckType;
import me.ardelys.hac.checks.DetectionConfidence;
import me.ardelys.hac.data.PlayerData;
import me.ardelys.hac.utils.TimeUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRegisterChannelEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClientDetector implements Listener {

    private final HAC plugin;
    private final Map<String, ClientProfile> profiles = new ConcurrentHashMap<>();
    private boolean enabled;
    private boolean autoKick;
    private DetectionConfidence autoKickConfidence;
    private String kickMessage;

    public ClientDetector(HAC plugin) {
        this.plugin = plugin;
        loadDefaults();
        loadConfiguration();
    }

    private void loadDefaults() {
        profiles.clear();
        registerProfile(new ClientProfile("Meteor",
                List.of("meteor"),
                List.of("meteor:client", "meteor:meteor", "meteor"),
                Set.of(CheckType.COMBAT, CheckType.MOVEMENT, CheckType.WORLD), 40.0));

        registerProfile(new ClientProfile("Wurst",
                List.of("wurst"),
                List.of("wurst:wurst", "wurst:client", "wurst:channel", "wurst"),
                Set.of(CheckType.MOVEMENT, CheckType.COMBAT, CheckType.WORLD), 40.0));

        registerProfile(new ClientProfile("LiquidBounce",
                List.of("liquidbounce", "liquid-bounce"),
                List.of("liquidbounce:channel", "liquidbounce:client", "lb:client", "liquidbounce"),
                Set.of(CheckType.COMBAT, CheckType.MOVEMENT, CheckType.PLAYER), 40.0));

        registerProfile(new ClientProfile("Boze",
                List.of("boze"),
                List.of("boze:client", "boze"),
                Set.of(CheckType.COMBAT, CheckType.MOVEMENT), 40.0));

        registerProfile(new ClientProfile("Vape",
                List.of("vape"),
                List.of("vape:channel", "vape_inject", "vape"),
                Set.of(CheckType.COMBAT), 35.0));

        registerProfile(new ClientProfile("Raven",
                List.of("raven", "ravenxd", "bplus"),
                List.of("raven:channel", "ravenxd"),
                Set.of(CheckType.COMBAT), 35.0));

        registerProfile(new ClientProfile("Future",
                List.of("future"),
                List.of("future:client", "future"),
                Set.of(CheckType.COMBAT, CheckType.MOVEMENT), 40.0));

        registerProfile(new ClientProfile("Inertia",
                List.of("inertia"),
                List.of("inertia:client", "inertia"),
                Set.of(CheckType.COMBAT, CheckType.MOVEMENT), 40.0));

        registerProfile(new ClientProfile("Lambda",
                List.of("lambda"),
                List.of("lambda:client", "lambda"),
                Set.of(CheckType.COMBAT, CheckType.MOVEMENT), 40.0));

        registerProfile(new ClientProfile("BleachHack",
                List.of("bleachhack"),
                List.of("bleachhack:channel", "bleachhack"),
                Set.of(CheckType.COMBAT, CheckType.MOVEMENT), 40.0));

        registerProfile(new ClientProfile("Aristois",
                List.of("aristois"),
                List.of("aristois:channel", "aristois:client", "aristois"),
                Set.of(CheckType.COMBAT, CheckType.MOVEMENT), 40.0));

        registerProfile(new ClientProfile("Impact",
                List.of("impact"),
                List.of("impact:channel", "impact"),
                Set.of(CheckType.COMBAT, CheckType.MOVEMENT), 40.0));

        registerProfile(new ClientProfile("RusherHack",
                List.of("rusherhack"),
                List.of("rusherhack:channel", "rusherhack"),
                Set.of(CheckType.COMBAT, CheckType.MOVEMENT), 40.0));

        registerProfile(new ClientProfile("Sigma",
                List.of("sigma", "sigmaclient"),
                List.of("sigma:client", "sigma"),
                Set.of(CheckType.COMBAT, CheckType.MOVEMENT), 40.0));

        registerProfile(new ClientProfile("Doomsday",
                List.of("doomsday"),
                List.of("doomsday:client", "doomsday"),
                Set.of(CheckType.COMBAT, CheckType.MOVEMENT), 40.0));

        registerProfile(new ClientProfile("CustomCheatMod",
                List.of("cheat", "hack", "clientmod", "autorun"),
                List.of("cheat:", "hack:"),
                Set.of(CheckType.COMBAT, CheckType.MOVEMENT), 30.0));
    }

    public void registerProfile(ClientProfile profile) {
        if (profile != null) {
            profiles.put(profile.getName().toLowerCase(), profile);
        }
    }

    public void loadConfiguration() {
        this.enabled = plugin.getConfig().getBoolean("client-detection.enabled", true);
        this.autoKick = plugin.getConfig().getBoolean("client-detection.auto-kick", true);
        String confStr = plugin.getConfig().getString("client-detection.auto-kick-confidence", "HIGH").toUpperCase();
        try {
            this.autoKickConfidence = DetectionConfidence.valueOf(confStr);
        } catch (IllegalArgumentException e) {
            this.autoKickConfidence = DetectionConfidence.HIGH;
        }

        this.kickMessage = plugin.getConfig().getString("client-detection.kick-message", "H-AC has detected an unauthorized modified client.");

        ConfigurationSection customSec = plugin.getConfig().getConfigurationSection("client-detection.custom-profiles");
        if (customSec != null) {
            for (String key : customSec.getKeys(false)) {
                String name = customSec.getString(key + ".name", key);
                List<String> brands = customSec.getStringList(key + ".brands");
                List<String> channels = customSec.getStringList(key + ".channels");
                double weight = customSec.getDouble(key + ".weight", 35.0);
                registerProfile(new ClientProfile(name, brands, channels, Set.of(CheckType.COMBAT, CheckType.MOVEMENT), weight));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChannelRegister(PlayerRegisterChannelEvent event) {
        if (!enabled) return;
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player);
        if (data == null) return;

        String channel = event.getChannel();
        data.addRegisteredChannel(channel);

        evaluate(data);
    }

    public void onBrandReceived(Player player, String rawBrand) {
        if (!enabled || player == null || rawBrand == null) return;
        PlayerData data = plugin.getPlayerDataManager().get(player);
        if (data == null) return;

        String cleanedBrand = rawBrand.replaceAll("[\\p{Cntrl}\\u200B-\\u200D\\uFEFF]", "");
        data.setClientBrand(cleanedBrand);

        evaluate(data);
    }

    public void onCategoryViolation(PlayerData data, CheckType type) {
        if (!enabled || data == null) return;
        evaluate(data);
    }

    public void evaluate(PlayerData data) {
        if (!enabled || data == null) return;
        Player player = data.getPlayer();
        if (player == null || !player.isOnline()) return;

        if (player.hasPermission("hac.bypass")
                || player.hasPermission("hukumac.bypass")
                || player.hasPermission("hac.admin")
                || player.hasPermission("hukumac.admin")) {
            return;
        }

        String brand = data.getClientBrand();
        Set<String> channels = data.getRegisteredChannels();

        ClientProfile bestMatch = null;
        double highestScore = 0.0;
        List<String> matchedSignals = new ArrayList<>();

        for (ClientProfile profile : profiles.values()) {
            double currentScore = 0.0;
            List<String> profileSignals = new ArrayList<>();

            if (brand != null && profile.matchesBrand(brand)) {
                currentScore += profile.getBaseWeight();
                profileSignals.add("Brand[" + profile.getName() + "]");
            }

            for (String ch : channels) {
                if (profile.matchesChannel(ch)) {
                    currentScore += profile.getBaseWeight() + 10.0;
                    profileSignals.add("Channel[" + ch + "]");
                    break;
                }
            }

            int matchingViolations = 0;
            for (CheckType correlated : profile.getCorrelatedCheckTypes()) {
                if (data.hasRecentViolation(correlated, 10000L)) {
                    matchingViolations++;
                }
            }

            if (matchingViolations > 0) {
                double boost = matchingViolations * 15.0;
                currentScore += boost;
                profileSignals.add("BehavioralCorrelation[" + matchingViolations + " categories]");
            }

            if (currentScore > highestScore) {
                highestScore = currentScore;
                bestMatch = profile;
                matchedSignals = profileSignals;
            }
        }

        if (brand != null && !brand.isEmpty()) {
            String lower = brand.toLowerCase();
            if (lower.equals("vanilla") && !channels.isEmpty()) {
                boolean hasModChannel = false;
                for (String ch : channels) {
                    if (ch.startsWith("fabric:") || ch.startsWith("forge:") || ch.startsWith("fml:")) {
                        hasModChannel = true;
                        break;
                    }
                }
                if (hasModChannel) {
                    highestScore += 25.0;
                    matchedSignals.add("Mismatch[VanillaBrand_With_ModChannels]");
                }
            }
        }

        if (highestScore <= 0.0 || bestMatch == null) {
            return;
        }

        int signalCount = matchedSignals.size();
        DetectionConfidence confidence;
        if (highestScore >= 80.0 && signalCount >= 2) {
            confidence = DetectionConfidence.CRITICAL;
        } else if (highestScore >= 55.0 && signalCount >= 2) {
            confidence = DetectionConfidence.HIGH;
        } else if (highestScore >= 25.0) {
            confidence = DetectionConfidence.MEDIUM;
        } else {
            confidence = DetectionConfidence.LOW;
        }

        data.setDetectedClient(bestMatch.getName(), confidence, highestScore, matchedSignals);

        if (confidence.ordinal() >= autoKickConfidence.ordinal()) {
            dispatchKick(data, bestMatch.getName(), confidence, highestScore, matchedSignals);
        } else if (confidence == DetectionConfidence.MEDIUM || confidence == DetectionConfidence.HIGH) {
            plugin.getAlertManager().sendAlert(player, "ClientIdentification", bestMatch.getName(), highestScore, data.getPing(), plugin.getServerTickListener().getRecentTps(), String.join(", ", matchedSignals), confidence);
        }
    }

    private void dispatchKick(PlayerData data, String clientName, DetectionConfidence confidence, double score, List<String> signals) {
        Player player = data.getPlayer();
        if (player == null || !player.isOnline()) return;

        String details = "Detected " + clientName + " (" + confidence.getDisplayName() + ", score=" + String.format("%.1f", score) + ") via " + String.join(", ", signals);

        plugin.getAsyncLogger().logViolation(player.getName(), "ClientIdentification", clientName, score, data.getPing(), plugin.getServerTickListener().getRecentTps(), details);

        plugin.getAlertManager().sendAlert(player, "ClientIdentification", clientName, score, data.getPing(), plugin.getServerTickListener().getRecentTps(), details, confidence);

        if (plugin.getDiscordWebhookManager() != null) {
            plugin.getDiscordWebhookManager().sendDetection(player, "ClientIdentification", clientName, score, data.getPing(), plugin.getServerTickListener().getRecentTps(), details, confidence);
        }

        if (autoKick) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    player.kick(Component.text(kickMessage));
                }
            });
        }
    }

    public Collection<ClientProfile> getProfiles() {
        return Collections.unmodifiableCollection(profiles.values());
    }

    public boolean isEnabled() {
        return enabled;
    }
}

package me.ardelys.hac.discord;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.Check;
import me.ardelys.hac.checks.DetectionConfidence;
import me.ardelys.hac.data.PlayerData;
import me.ardelys.hac.language.LanguagePlaceholder;
import me.ardelys.hac.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DiscordWebhookManager {

    private final HAC plugin;
    private final DiscordHttpClient httpClient;
    private BlockingQueue<DiscordMessage> messageQueue;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread workerThread;

    private boolean enabled;
    private boolean configured;
    private String webhookUrl;
    private String username;
    private String avatarUrl;

    private boolean eventDetections;
    private boolean eventPunishments;
    private boolean eventStartup;
    private boolean eventShutdown;

    private int cooldownSeconds;
    private int maxQueueSize;
    private double minVlToSend;

    private int colorDetection;
    private int colorPunishment;
    private int colorStartup;
    private int colorShutdown;
    private int colorTest;

    private final Map<String, AggregatedDetection> aggregationMap = new ConcurrentHashMap<>();

    private static class AggregatedDetection {
        final UUID playerUuid;
        final String check;
        final String type;
        final long firstTime;
        long lastTime;
        int count;
        double latestVl;
        String latestReason;
        int lastPing;
        double lastTps;

        AggregatedDetection(UUID playerUuid, String check, String type, long now, double vl, String reason, int ping, double tps) {
            this.playerUuid = playerUuid;
            this.check = check;
            this.type = type;
            this.firstTime = now;
            this.lastTime = now;
            this.count = 1;
            this.latestVl = vl;
            this.latestReason = reason;
            this.lastPing = ping;
            this.lastTps = tps;
        }
    }

    public DiscordWebhookManager(HAC plugin) {
        this.plugin = plugin;
        this.httpClient = new DiscordHttpClient();
        loadConfiguration();
        startWorker();
    }

    public void loadConfiguration() {
        FileConfiguration config = plugin.getConfig();

        this.enabled = config.getBoolean("discord-webhook.enabled", false);
        this.webhookUrl = config.getString("discord-webhook.url", "").trim();
        this.username = config.getString("discord-webhook.username", "H-AC");
        this.avatarUrl = config.getString("discord-webhook.avatar-url", "");

        this.eventDetections = config.getBoolean("discord-webhook.events.detections", true);
        this.eventPunishments = config.getBoolean("discord-webhook.events.punishments", true);
        this.eventStartup = config.getBoolean("discord-webhook.events.startup", false);
        this.eventShutdown = config.getBoolean("discord-webhook.events.shutdown", false);

        this.cooldownSeconds = Math.max(1, config.getInt("discord-webhook.settings.cooldown", 3));
        this.maxQueueSize = Math.max(10, config.getInt("discord-webhook.settings.max-queue-size", 100));
        this.minVlToSend = Math.max(0.0, config.getDouble("discord-webhook.settings.min-vl-to-send", 5.0));

        this.colorDetection = config.getInt("discord-webhook.colors.detection", 16776960);
        this.colorPunishment = config.getInt("discord-webhook.colors.punishment", 15158332);
        this.colorStartup = config.getInt("discord-webhook.colors.startup", 5763719);
        this.colorShutdown = config.getInt("discord-webhook.colors.shutdown", 9807270);
        this.colorTest = config.getInt("discord-webhook.colors.test", 3447003);

        this.configured = isValidWebhookUrl(webhookUrl);

        if (enabled && !configured) {
            plugin.getLogger().warning("[H-AC] Discord Webhook is enabled but URL is missing or invalid. Webhook delivery disabled.");
        }

        if (messageQueue == null) {
            this.messageQueue = new LinkedBlockingQueue<>(maxQueueSize);
        }
    }

    private boolean isValidWebhookUrl(String url) {
        if (url == null || url.isBlank()) return false;
        return url.matches("^https://(canary\\.|ptb\\.)?discord(app)?\\.com/api(/v\\d+)?/webhooks/\\d+/[A-Za-z0-9_-]+.*$");
    }

    private void startWorker() {
        if (running.compareAndSet(false, true)) {
            workerThread = new Thread(this::runWorker, "H-AC-DiscordWebhook");
            workerThread.setDaemon(true);
            workerThread.start();
        }
    }

    public void reload() {
        loadConfiguration();
    }

    public void stop() {
        if (running.compareAndSet(true, false)) {
            if (workerThread != null) {
                workerThread.interrupt();
                try {
                    workerThread.join(1500);
                } catch (InterruptedException ignored) {
                }
            }
            if (messageQueue != null) {
                messageQueue.clear();
            }
            aggregationMap.clear();
        }
    }

    public void queueMessage(DiscordMessage message) {
        if (!enabled || !configured || message == null || messageQueue == null) {
            return;
        }

        if (messageQueue.size() >= maxQueueSize) {
            if (message.isHighPriority()) {
                messageQueue.poll();
                messageQueue.offer(message);
            }
            return;
        }

        messageQueue.offer(message);
    }

    public void sendDetection(Player player, String check, String type, double vl, int ping, double tps, String details) {
        sendDetection(player, check, type, vl, ping, tps, details, null);
    }

    public void sendDetection(Player player, String check, String type, double vl, int ping, double tps, String details, DetectionConfidence confidence) {
        if (!enabled || !configured || !eventDetections || player == null) {
            return;
        }

        if (vl < minVlToSend) {
            return;
        }

        long now = TimeUtil.now();
        String aggKey = player.getUniqueId() + ":" + check.toLowerCase();
        long cooldownMs = cooldownSeconds * 1000L;

        AggregatedDetection existing = aggregationMap.get(aggKey);
        if (existing != null) {
            if (now - existing.firstTime < cooldownMs) {
                existing.count++;
                existing.lastTime = now;
                existing.latestVl = vl;
                existing.latestReason = details;
                existing.lastPing = ping;
                existing.lastTps = tps;
                return;
            } else {
                aggregationMap.remove(aggKey);
                dispatchAggregatedDetection(player, check, type, existing, confidence);
            }
        }

        aggregationMap.put(aggKey, new AggregatedDetection(player.getUniqueId(), check, type, now, vl, details, ping, tps));
        dispatchDetection(player, check, type, vl, ping, tps, details, 1, 0, confidence);
    }

    private void dispatchAggregatedDetection(Player player, String check, String type, AggregatedDetection agg, DetectionConfidence confidence) {
        long durationSec = Math.max(1, (agg.lastTime - agg.firstTime) / 1000L);
        dispatchDetection(player, check, type, agg.latestVl, agg.lastPing, agg.lastTps, agg.latestReason, agg.count, durationSec, confidence);
    }

    private void dispatchDetection(Player player, String check, String type, double vl, int ping, double tps, String details, int count, long durationSec, DetectionConfidence confidence) {
        String confStr = confidence != null ? confidence.getDisplayName() : "Medium";
        Map<String, Object> placeholders = LanguagePlaceholder.builder()
                .add("player", player.getName())
                .add("check", check)
                .add("check_type", type)
                .add("vl", String.format("%.1f", vl))
                .add("ping", ping)
                .add("tps", String.format("%.1f", tps))
                .add("reason", details != null ? details : "N/A")
                .add("count", count)
                .add("duration", durationSec)
                .add("confidence", confStr)
                .build();

        String title = plugin.getLanguageManager().getRaw("discord.detection.title", placeholders);
        String desc = plugin.getLanguageManager().getRaw("discord.detection.description", placeholders);
        String footer = plugin.getLanguageManager().getRaw("discord.embed.footer");

        DiscordEmbed embed = new DiscordEmbed()
                .setTitle(title)
                .setDescription(desc)
                .setColor(colorDetection)
                .setFooter(footer)
                .setTimestamp(Instant.now().toString())
                .addField(plugin.getLanguageManager().getRaw("discord.detection.player-field"), player.getName(), true)
                .addField(plugin.getLanguageManager().getRaw("discord.detection.check-field"), check + " (" + type + ")", true)
                .addField(plugin.getLanguageManager().getRaw("discord.detection.vl-field"), String.format("%.1f", vl), true)
                .addField(plugin.getLanguageManager().getRaw("discord.detection.confidence-field"), confStr, true)
                .addField(plugin.getLanguageManager().getRaw("discord.detection.ping-field"), ping + "ms", true)
                .addField(plugin.getLanguageManager().getRaw("discord.detection.tps-field"), String.format("%.1f", tps), true);

        PlayerData data = plugin.getPlayerDataManager().get(player);
        if (data != null && data.getClientBrand() != null && !data.getClientBrand().isEmpty()) {
            embed.addField("Client", data.getClientBrand(), true);
        }

        embed.addField(plugin.getLanguageManager().getRaw("discord.detection.reason-field"), details != null ? details : "N/A", false);

        if (count > 1) {
            embed.addField("Burst Info", plugin.getLanguageManager().getRaw("discord.detection.aggregated", placeholders), false);
        }

        DiscordMessage message = new DiscordMessage(username, avatarUrl, false);
        message.addEmbed(embed);
        queueMessage(message);
    }

    public void sendPunishment(PlayerData data, Check check, String action, String reason) {
        if (!enabled || !configured || !eventPunishments || data == null || check == null) {
            return;
        }

        Player player = data.getPlayer();
        String playerName = player != null ? player.getName() : data.getUuid().toString();
        double vl = data.getViolationLevel(check.getName());

        Map<String, Object> placeholders = LanguagePlaceholder.builder()
                .add("player", playerName)
                .add("check", check.getName())
                .add("action", action != null ? action : "Kick")
                .add("vl", String.format("%.1f", vl))
                .add("reason", reason != null ? reason : "Unfair Advantage")
                .build();

        String title = plugin.getLanguageManager().getRaw("discord.punishment.title", placeholders);
        String desc = plugin.getLanguageManager().getRaw("discord.punishment.description", placeholders);
        String footer = plugin.getLanguageManager().getRaw("discord.embed.footer");

        DiscordEmbed embed = new DiscordEmbed()
                .setTitle(title)
                .setDescription(desc)
                .setColor(colorPunishment)
                .setFooter(footer)
                .setTimestamp(Instant.now().toString())
                .addField(plugin.getLanguageManager().getRaw("discord.punishment.player-field"), playerName, true)
                .addField(plugin.getLanguageManager().getRaw("discord.punishment.action-field"), action != null ? action : "Kick", true)
                .addField(plugin.getLanguageManager().getRaw("discord.punishment.check-field"), check.getName(), true)
                .addField(plugin.getLanguageManager().getRaw("discord.punishment.vl-field"), String.format("%.1f", vl), true);

        if (data.getClientBrand() != null && !data.getClientBrand().isEmpty()) {
            embed.addField("Client", data.getClientBrand(), true);
        }

        embed.addField(plugin.getLanguageManager().getRaw("discord.punishment.reason-field"), reason != null ? reason : "N/A", false);

        DiscordMessage message = new DiscordMessage(username, avatarUrl, true);
        message.addEmbed(embed);
        queueMessage(message);
    }

    public void sendStartup() {
        if (!enabled || !configured || !eventStartup) {
            return;
        }

        Map<String, Object> placeholders = LanguagePlaceholder.of("version", plugin.getPluginMeta().getVersion());
        String title = plugin.getLanguageManager().getRaw("discord.startup.title", placeholders);
        String desc = plugin.getLanguageManager().getRaw("discord.startup.description", placeholders);
        String footer = plugin.getLanguageManager().getRaw("discord.embed.footer");

        DiscordEmbed embed = new DiscordEmbed()
                .setTitle(title)
                .setDescription(desc)
                .setColor(colorStartup)
                .setFooter(footer)
                .setTimestamp(Instant.now().toString());

        DiscordMessage message = new DiscordMessage(username, avatarUrl, false);
        message.addEmbed(embed);
        queueMessage(message);
    }

    public void sendShutdown() {
        if (!enabled || !configured || !eventShutdown) {
            return;
        }

        String title = plugin.getLanguageManager().getRaw("discord.shutdown.title");
        String desc = plugin.getLanguageManager().getRaw("discord.shutdown.description");
        String footer = plugin.getLanguageManager().getRaw("discord.embed.footer");

        DiscordEmbed embed = new DiscordEmbed()
                .setTitle(title)
                .setDescription(desc)
                .setColor(colorShutdown)
                .setFooter(footer)
                .setTimestamp(Instant.now().toString());

        DiscordMessage message = new DiscordMessage(username, avatarUrl, true);
        message.addEmbed(embed);

        if (httpClient != null) {
            httpClient.send(webhookUrl, message.toJson(), java.time.Duration.ofMillis(1500));
        }
    }

    public boolean sendTest(CommandSender sender) {
        if (!enabled || !configured) {
            return false;
        }

        String title = plugin.getLanguageManager().getRaw("discord.test.title");
        String desc = plugin.getLanguageManager().getRaw("discord.test.description");
        String footer = plugin.getLanguageManager().getRaw("discord.embed.footer");

        DiscordEmbed embed = new DiscordEmbed()
                .setTitle(title)
                .setDescription(desc)
                .setColor(colorTest)
                .setFooter(footer)
                .setTimestamp(Instant.now().toString())
                .addField(plugin.getLanguageManager().getRaw("discord.test.server-field"), plugin.getServer().getName(), true)
                .addField(plugin.getLanguageManager().getRaw("discord.test.status-field"), plugin.getLanguageManager().getRaw("discord.test.status-value"), true);

        DiscordMessage message = new DiscordMessage(username, avatarUrl, true);
        message.addEmbed(embed);
        queueMessage(message);
        return true;
    }

    public String getMaskedUrl() {
        if (!configured || webhookUrl.isEmpty()) {
            return "N/A";
        }
        int lastSlash = webhookUrl.lastIndexOf('/');
        if (lastSlash != -1 && lastSlash < webhookUrl.length() - 6) {
            return webhookUrl.substring(0, lastSlash + 1) + "********";
        }
        return "********";
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isConfigured() {
        return configured;
    }

    public void cleanupPlayer(UUID uuid) {
        if (uuid == null) return;
        String prefix = uuid.toString() + ":";
        aggregationMap.keySet().removeIf(k -> k.startsWith(prefix));
    }

    private void flushPendingAggregations(long now) {
        long cooldownMs = cooldownSeconds * 1000L;
        for (Map.Entry<String, AggregatedDetection> entry : aggregationMap.entrySet()) {
            AggregatedDetection agg = entry.getValue();
            if (now - agg.lastTime >= cooldownMs) {
                if (aggregationMap.remove(entry.getKey(), agg)) {
                    if (agg.count > 1) {
                        Player p = Bukkit.getPlayer(agg.playerUuid);
                        if (p != null && p.isOnline()) {
                            dispatchAggregatedDetection(p, agg.check, agg.type, agg, null);
                        }
                    }
                }
            }
        }
    }

    private void runWorker() {
        long lastAggregationFlush = TimeUtil.now();
        while (running.get() || (messageQueue != null && !messageQueue.isEmpty())) {
            try {
                DiscordMessage message = messageQueue.poll(250, TimeUnit.MILLISECONDS);
                if (message != null && configured && enabled) {
                    int attempts = 0;
                    boolean delivered = false;
                    while (attempts < 3 && !delivered && running.get()) {
                        attempts++;
                        DiscordHttpClient.ResponseStatus response = httpClient.send(webhookUrl, message.toJson());
                        if (response.success()) {
                            delivered = true;
                        } else if (response.statusCode() == 429) {
                            long sleepMs = Math.min(5000L, Math.max(500L, response.retryAfterMs()));
                            Thread.sleep(sleepMs);
                        } else if (response.statusCode() == 401 || response.statusCode() == 404) {
                            this.configured = false;
                            plugin.getLogger().warning("[H-AC] Discord Webhook URL is invalid or deleted (HTTP " + response.statusCode() + "). Disabling delivery.");
                            break;
                        } else if (response.statusCode() == 400) {
                            plugin.getLogger().warning("[H-AC] Discord Webhook rejected embed format (HTTP 400).");
                            break;
                        } else {
                            if (attempts >= 3) {
                                plugin.getLogger().warning("[H-AC] Discord Webhook delivery failed: " + response.errorMessage());
                            } else {
                                Thread.sleep(1000L);
                            }
                        }
                    }
                }

                long now = TimeUtil.now();
                if (now - lastAggregationFlush > 5000L) {
                    lastAggregationFlush = now;
                    flushPendingAggregations(now);
                }
            } catch (InterruptedException e) {
                break;
            } catch (Throwable ignored) {
            }
        }
    }
}

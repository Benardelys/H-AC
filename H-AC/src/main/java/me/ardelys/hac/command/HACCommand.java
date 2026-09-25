package me.ardelys.hac.command;

import me.ardelys.hac.HAC;
import me.ardelys.hac.client.injector.InjectorDetectionResult;
import me.ardelys.hac.client.injector.InjectorDetector;
import me.ardelys.hac.client.injector.ReconnectProtection;
import me.ardelys.hac.data.PlayerData;
import me.ardelys.hac.discord.DiscordWebhookManager;
import me.ardelys.hac.language.LanguagePlaceholder;
import me.ardelys.hac.utils.TimeUtil;
import me.ardelys.hac.violation.Violation;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class HACCommand implements CommandExecutor, TabCompleter {

    private final HAC plugin;

    public HACCommand(HAC plugin) {
        this.plugin = plugin;
    }

    private boolean hasAdminPerm(CommandSender sender) {
        return sender.hasPermission("hac.admin")
                || sender.hasPermission("hukumac.admin")
                || sender.hasPermission("hac.*")
                || sender.hasPermission("hukumac.*");
    }

    private boolean hasAlertsPerm(CommandSender sender) {
        return sender.hasPermission("hac.alerts")
                || sender.hasPermission("hukumac.alerts")
                || hasAdminPerm(sender);
    }

    private boolean hasDebugPerm(CommandSender sender) {
        return sender.hasPermission("hac.debug")
                || sender.hasPermission("hukumac.debug")
                || hasAdminPerm(sender);
    }

    private boolean hasWebhookPerm(CommandSender sender) {
        return sender.hasPermission("hac.webhook")
                || sender.hasPermission("hukumac.webhook")
                || hasAdminPerm(sender);
    }

    private boolean hasInjectorPerm(CommandSender sender) {
        return sender.hasPermission("hac.injector")
                || sender.hasPermission("hukumac.injector")
                || hasAdminPerm(sender);
    }

    private boolean isValidPlayerName(String name) {
        return name != null && name.length() <= 16 && name.matches("^[a-zA-Z0-9_]{1,16}$");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!hasAdminPerm(sender) && !hasAlertsPerm(sender) && !hasDebugPerm(sender) && !hasWebhookPerm(sender) && !hasInjectorPerm(sender)) {
            plugin.getLanguageManager().sendPrefixedMessage(sender, "errors.no-permission");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload" -> {
                if (!hasAdminPerm(sender)) {
                    plugin.getLanguageManager().sendPrefixedMessage(sender, "errors.no-permission");
                    return true;
                }
                plugin.getConfigManager().loadConfig();
                plugin.getLanguageManager().loadLanguages();
                plugin.getCheckManager().reloadChecks();
                plugin.getViolationManager().startDecayTask();
                if (plugin.getClientDetector() != null) {
                    plugin.getClientDetector().loadConfiguration();
                }
                if (plugin.getInjectorDetector() != null) {
                    plugin.getInjectorDetector().loadConfiguration();
                }
                if (plugin.getDiscordWebhookManager() != null) {
                    plugin.getDiscordWebhookManager().reload();
                }
                plugin.getLanguageManager().sendPrefixedMessage(sender, "commands.reload.success");
            }
            case "alerts" -> {
                if (!hasAlertsPerm(sender)) {
                    plugin.getLanguageManager().sendPrefixedMessage(sender, "errors.no-permission");
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    plugin.getLanguageManager().sendPrefixedMessage(sender, "errors.player-only");
                    return true;
                }
                boolean enabled = plugin.getAlertManager().toggleAlerts(player);
                plugin.getLanguageManager().sendPrefixedMessage(
                        sender,
                        enabled ? "commands.alerts.enabled" : "commands.alerts.disabled"
                );
            }
            case "debug" -> {
                if (!hasDebugPerm(sender)) {
                    plugin.getLanguageManager().sendPrefixedMessage(sender, "errors.no-permission");
                    return true;
                }
                if (args.length < 2) {
                    plugin.getLanguageManager().sendPrefixedMessage(sender, "commands.debug.usage");
                    return true;
                }
                if (!isValidPlayerName(args[1])) {
                    sender.sendMessage("§8[§c§lH-AC§8] §cInvalid player name format.");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null || !target.isOnline()) {
                    plugin.getLanguageManager().sendPrefixedMessage(
                            sender,
                            "errors.player-not-found",
                            LanguagePlaceholder.of("player", args[1])
                    );
                    return true;
                }
                PlayerData data = plugin.getPlayerDataManager().get(target);
                data.setDebugWatched(!data.isDebugWatched());
                plugin.getLanguageManager().sendPrefixedMessage(
                        sender,
                        data.isDebugWatched() ? "commands.debug.started" : "commands.debug.stopped",
                        LanguagePlaceholder.of("player", target.getName())
                );
            }
            case "status" -> {
                if (!hasAdminPerm(sender)) {
                    plugin.getLanguageManager().sendPrefixedMessage(sender, "errors.no-permission");
                    return true;
                }
                handleStatusCommand(sender);
            }
            case "info" -> {
                if (!hasAdminPerm(sender)) {
                    plugin.getLanguageManager().sendPrefixedMessage(sender, "errors.no-permission");
                    return true;
                }
                if (args.length < 2) {
                    plugin.getLanguageManager().sendPrefixedMessage(sender, "commands.info.usage");
                    return true;
                }
                if (!isValidPlayerName(args[1])) {
                    sender.sendMessage("§8[§c§lH-AC§8] §cInvalid player name format.");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null || !target.isOnline()) {
                    plugin.getLanguageManager().sendPrefixedMessage(
                            sender,
                            "errors.player-not-found",
                            LanguagePlaceholder.of("player", args[1])
                    );
                    return true;
                }
                PlayerData data = plugin.getPlayerDataManager().get(target);
                plugin.getLanguageManager().sendMessage(sender, "commands.info.header");
                plugin.getLanguageManager().sendMessage(
                        sender,
                        "commands.info.player-info",
                        LanguagePlaceholder.of("player", target.getName(), "uuid", target.getUniqueId())
                );
                plugin.getLanguageManager().sendMessage(
                        sender,
                        "commands.info.metrics",
                        LanguagePlaceholder.builder()
                                .add("ping", data.getPing())
                                .add("ground", data.isOnGround())
                                .add("math_ground", data.isMathematicallyOnGround())
                                .build()
                );
                plugin.getLanguageManager().sendMessage(
                        sender,
                        "commands.info.vl",
                        LanguagePlaceholder.of("vl", String.format("%.2f", data.getTotalViolationLevel()))
                );
                plugin.getLanguageManager().sendMessage(
                        sender,
                        "commands.info.active-checks",
                        LanguagePlaceholder.of("checks_count", plugin.getCheckManager().getAllChecks().size())
                );
                plugin.getLanguageManager().sendMessage(
                        sender,
                        "commands.info.client-brand",
                        LanguagePlaceholder.of("brand", data.getClientBrand(), "suspicious", data.hasSuspiciousBrand() ? "(!)" : "")
                );
                if (data.getDetectedClientName() != null) {
                    sender.sendMessage("§8[§c§lH-AC§8] §7Detected Client: §c§l" + data.getDetectedClientName() + " §8[§e" + (data.getClientDetectionConfidence() != null ? data.getClientDetectionConfidence().getDisplayName() : "N/A") + "§8]");
                }
                if (data.getInjectorResult() != null && data.getInjectorResult().getScore() > 0.0) {
                    sender.sendMessage("§8[§c§lH-AC§8] §7Injector Suspicion: §c" + String.format("%.1f", data.getInjectorResult().getScore()) + "/100 §8[§e" + data.getInjectorResult().getConfidence().getDisplayName() + "§8] §7Modules: §f" + data.getInjectorResult().getFormattedModules());
                }
                plugin.getLanguageManager().sendMessage(sender, "commands.info.footer");
            }
            case "violations" -> {
                if (!hasAdminPerm(sender)) {
                    plugin.getLanguageManager().sendPrefixedMessage(sender, "errors.no-permission");
                    return true;
                }
                if (args.length < 2) {
                    plugin.getLanguageManager().sendPrefixedMessage(sender, "commands.violations.usage");
                    return true;
                }
                if (!isValidPlayerName(args[1])) {
                    sender.sendMessage("§8[§c§lH-AC§8] §cInvalid player name format.");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null || !target.isOnline()) {
                    plugin.getLanguageManager().sendPrefixedMessage(
                            sender,
                            "errors.player-not-found",
                            LanguagePlaceholder.of("player", args[1])
                    );
                    return true;
                }
                PlayerData data = plugin.getPlayerDataManager().get(target);
                plugin.getLanguageManager().sendMessage(sender, "commands.violations.header");
                plugin.getLanguageManager().sendMessage(
                        sender,
                        "commands.violations.title",
                        LanguagePlaceholder.of("player", target.getName())
                );
                Map<String, Double> vls = data.getViolations();
                if (vls.isEmpty()) {
                    plugin.getLanguageManager().sendMessage(sender, "commands.violations.none");
                } else {
                    for (Map.Entry<String, Double> entry : vls.entrySet()) {
                        plugin.getLanguageManager().sendMessage(
                                sender,
                                "commands.violations.entry",
                                LanguagePlaceholder.of("check", entry.getKey(), "vl", String.format("%.1f", entry.getValue()))
                        );
                    }
                }

                List<Violation> history = plugin.getViolationManager().getHistory(target.getUniqueId());
                if (!history.isEmpty()) {
                    plugin.getLanguageManager().sendMessage(sender, "commands.violations.recent-header");
                    int count = 0;
                    for (Violation v : history) {
                        plugin.getLanguageManager().sendMessage(
                                sender,
                                "commands.violations.recent-entry",
                                LanguagePlaceholder.builder()
                                        .add("check", v.check().getName())
                                        .add("check_type", v.checkType())
                                        .add("vl", String.format("%.1f", v.addedVl()))
                                        .add("reason", v.details())
                                        .build()
                        );
                        if (++count >= 5) break;
                    }
                }
                plugin.getLanguageManager().sendMessage(sender, "commands.violations.footer");
            }
            case "webhook" -> {
                if (!hasWebhookPerm(sender)) {
                    plugin.getLanguageManager().sendPrefixedMessage(sender, "errors.no-permission");
                    return true;
                }
                handleWebhookCommand(sender, args);
            }
            case "injector" -> {
                if (!hasInjectorPerm(sender)) {
                    plugin.getLanguageManager().sendPrefixedMessage(sender, "errors.no-permission");
                    return true;
                }
                handleInjectorCommand(sender, args);
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleInjectorCommand(CommandSender sender, String[] args) {
        InjectorDetector detector = plugin.getInjectorDetector();
        if (detector == null) {
            sender.sendMessage("§8[§c§lH-AC§8] §cInjector protection subsystem is not initialized.");
            return;
        }

        if (args.length < 2) {
            ReconnectProtection rp = detector.getReconnectProtection();
            sender.sendMessage("§8§m----------------§r §c§lH-AC Injector Protection §8§m----------------");
            sender.sendMessage("§7Status: §a" + (detector.isEnabled() ? "Active" : "Disabled"));
            sender.sendMessage("§7Reconnect Protection: §e" + (rp != null ? rp.size() + " snapshots cached" : "0"));

            int elevatedCount = 0;
            for (Player p : Bukkit.getOnlinePlayers()) {
                PlayerData data = plugin.getPlayerDataManager().get(p);
                if (data != null && data.getInjectorResult() != null && data.getInjectorResult().getScore() >= 25.0) {
                    elevatedCount++;
                    sender.sendMessage(" §8- §c" + p.getName() + " §7| Score: §e" + String.format("%.1f", data.getInjectorResult().getScore())
                            + " §8[§b" + data.getInjectorResult().getConfidence().getDisplayName() + "§8] §7Modules: §f" + data.getInjectorResult().getFormattedModules());
                }
            }
            if (elevatedCount == 0) {
                sender.sendMessage("§7Active Suspicious Players: §aNone");
            }

            if (rp != null && rp.size() > 0) {
                sender.sendMessage("§7Cached Reconnect Snapshots:");
                int shown = 0;
                long now = TimeUtil.now();
                for (ReconnectProtection.SuspiciousSnapshot snap : rp.getAllSnapshots()) {
                    long remainingSec = Math.max(0, 600 - (now - snap.timestamp()) / 1000);
                    sender.sendMessage(" §8- §e" + snap.playerName() + " §7| Score: §c" + String.format("%.1f", snap.score())
                            + " §8[§6" + snap.confidence().getDisplayName() + "§8] §7Modules: §f" + String.join(", ", snap.modules()) + " §8(§7expires in " + remainingSec + "s§8)");
                    if (++shown >= 5) break;
                }
            }
            sender.sendMessage("§7Use §f/hac injector <player> §7to inspect a specific player.");
            sender.sendMessage("§8§m----------------------------------------------------");
            return;
        }

        String targetName = args[1];
        if (!isValidPlayerName(targetName)) {
            sender.sendMessage("§8[§c§lH-AC§8] §cInvalid player name format.");
            return;
        }

        Player onlineTarget = Bukkit.getPlayer(targetName);
        if (onlineTarget != null && onlineTarget.isOnline()) {
            PlayerData data = plugin.getPlayerDataManager().get(onlineTarget);
            InjectorDetectionResult result = data != null ? data.getInjectorResult() : null;

            sender.sendMessage("§8§m----------------§r §c§lInjector Inspection: §e" + onlineTarget.getName() + " §8§m----------------");
            sender.sendMessage("§7UUID: §8" + onlineTarget.getUniqueId());
            sender.sendMessage("§7Client Brand: §f" + (data != null ? data.getClientBrand() : "Unknown"));
            if (result != null && result.getScore() > 0.0) {
                sender.sendMessage("§7Suspicion Score: §c" + String.format("%.1f", result.getScore()) + "/100");
                sender.sendMessage("§7Confidence: §e" + result.getConfidence().getDisplayName());
                sender.sendMessage("§7Active Modules: §f" + result.getFormattedModules());
                sender.sendMessage("§7Detected Signals: §f" + result.getFormattedSignals());
            } else {
                sender.sendMessage("§7Suspicion Score: §a0.0/100 (Clean)");
                sender.sendMessage("§7Active Modules: §aNone");
            }
            sender.sendMessage("§8§m----------------------------------------------------");
            return;
        }

        ReconnectProtection rp = detector.getReconnectProtection();
        ReconnectProtection.SuspiciousSnapshot snapshot = rp != null ? rp.getSnapshotByName(targetName) : null;
        if (snapshot != null) {
            long remainingSec = Math.max(0, 600 - (TimeUtil.now() - snapshot.timestamp()) / 1000);
            sender.sendMessage("§8§m----------------§r §c§lInjector Snapshot (Offline): §e" + snapshot.playerName() + " §8§m----------------");
            sender.sendMessage("§7UUID: §8" + snapshot.uuid());
            sender.sendMessage("§7Cached Score: §c" + String.format("%.1f", snapshot.score()) + "/100");
            sender.sendMessage("§7Cached Confidence: §e" + snapshot.confidence().getDisplayName());
            sender.sendMessage("§7Detected Modules: §f" + (snapshot.modules().isEmpty() ? "None" : String.join(", ", snapshot.modules())));
            sender.sendMessage("§7Cache Expiration: §7In " + remainingSec + " seconds");
            sender.sendMessage("§8§m----------------------------------------------------");
            return;
        }

        sender.sendMessage("§8[§c§lH-AC§8] §7No active suspicion or cached snapshot found for §e" + targetName + "§7.");
    }

    private void handleStatusCommand(CommandSender sender) {
        String version = plugin.getPluginMeta().getVersion();
        String uptime = plugin.getFormattedUptime();
        double tps = plugin.getServerTickListener().getRecentTps();
        int activeChecks = plugin.getCheckManager().getAllChecks().size();
        int trackedPlayers = Bukkit.getOnlinePlayers().size();
        int debugPlayers = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerData pd = plugin.getPlayerDataManager().get(p);
            if (pd != null && pd.isDebugWatched()) {
                debugPlayers++;
            }
        }

        DiscordWebhookManager wm = plugin.getDiscordWebhookManager();
        String webhookStatus;
        if (wm == null || !wm.isEnabled()) {
            webhookStatus = plugin.getLanguageManager().getRaw("commands.webhook.status-off");
        } else if (wm.isConfigured()) {
            webhookStatus = plugin.getLanguageManager().getRaw("commands.webhook.status-on");
        } else {
            webhookStatus = plugin.getLanguageManager().getRaw("commands.webhook.status-invalid");
        }

        plugin.getLanguageManager().sendMessage(sender, "commands.status.header");
        plugin.getLanguageManager().sendMessage(sender, "commands.status.title");
        plugin.getLanguageManager().sendMessage(sender, "commands.status.version", LanguagePlaceholder.of("version", version));
        plugin.getLanguageManager().sendMessage(sender, "commands.status.uptime", LanguagePlaceholder.of("uptime", uptime));
        plugin.getLanguageManager().sendMessage(sender, "commands.status.tps", LanguagePlaceholder.of("tps", String.format("%.2f", tps)));
        plugin.getLanguageManager().sendMessage(sender, "commands.status.checks", LanguagePlaceholder.of("checks_count", activeChecks));
        if (plugin.getClientDetector() != null) {
            sender.sendMessage("§8[§c§lH-AC§8] §7Client Profiles: §a" + plugin.getClientDetector().getProfiles().size() + " loaded §7| Auto-Kick: §e" + (plugin.getClientDetector().isEnabled() ? "Active" : "Disabled"));
        }
        if (plugin.getInjectorDetector() != null) {
            sender.sendMessage("§8[§c§lH-AC§8] §7Injector Protection: §a" + (plugin.getInjectorDetector().isEnabled() ? "Active" : "Disabled") + " §7| Reconnect Cache: §e" + plugin.getInjectorDetector().getReconnectProtection().size() + " cached");
        }
        plugin.getLanguageManager().sendMessage(sender, "commands.status.players", LanguagePlaceholder.builder()
                .add("tracked_players", trackedPlayers)
                .add("debug_players", debugPlayers)
                .build());
        plugin.getLanguageManager().sendMessage(sender, "commands.status.webhook", LanguagePlaceholder.of("webhook_status", webhookStatus));
        plugin.getLanguageManager().sendMessage(sender, "commands.status.footer");
    }

    private void handleWebhookCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getLanguageManager().sendPrefixedMessage(sender, "commands.webhook.usage");
            return;
        }

        DiscordWebhookManager wm = plugin.getDiscordWebhookManager();
        if (wm == null) {
            plugin.getLanguageManager().sendPrefixedMessage(sender, "commands.webhook.disabled");
            return;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "status" -> {
                plugin.getLanguageManager().sendMessage(sender, "commands.webhook.status-header");
                plugin.getLanguageManager().sendMessage(sender, "commands.webhook.status-title");
                plugin.getLanguageManager().sendMessage(sender, "commands.webhook.status-enabled",
                        LanguagePlaceholder.of("status", wm.isEnabled() ?
                                plugin.getLanguageManager().getRaw("commands.webhook.status-on") :
                                plugin.getLanguageManager().getRaw("commands.webhook.status-off")));
                plugin.getLanguageManager().sendMessage(sender, "commands.webhook.status-configured",
                        LanguagePlaceholder.of("configured", wm.isConfigured() ?
                                plugin.getLanguageManager().getRaw("commands.webhook.status-valid") :
                                plugin.getLanguageManager().getRaw("commands.webhook.status-invalid")));
                plugin.getLanguageManager().sendMessage(sender, "commands.webhook.status-url",
                        LanguagePlaceholder.of("url", wm.getMaskedUrl()));
                plugin.getLanguageManager().sendMessage(sender, "commands.webhook.status-footer");
            }
            case "test" -> {
                if (!wm.isEnabled()) {
                    plugin.getLanguageManager().sendPrefixedMessage(sender, "commands.webhook.disabled");
                    return;
                }
                plugin.getLanguageManager().sendPrefixedMessage(sender, "commands.webhook.test-sending");
                boolean success = wm.sendTest(sender);
                if (success) {
                    plugin.getLanguageManager().sendPrefixedMessage(sender, "commands.webhook.test-success");
                } else {
                    plugin.getLanguageManager().sendPrefixedMessage(sender, "commands.webhook.test-failed");
                }
            }
            case "reload" -> {
                wm.reload();
                plugin.getLanguageManager().sendPrefixedMessage(sender, "commands.webhook.reloaded");
            }
            default -> plugin.getLanguageManager().sendPrefixedMessage(sender, "commands.webhook.usage");
        }
    }

    private void sendHelp(CommandSender sender) {
        String version = plugin.getPluginMeta().getVersion();
        Map<String, Object> versionMap = LanguagePlaceholder.of("version", version);

        plugin.getLanguageManager().sendMessage(sender, "commands.help.header");
        plugin.getLanguageManager().sendMessage(sender, "commands.help.title", versionMap);
        plugin.getLanguageManager().sendMessage(sender, "commands.help.author");
        plugin.getLanguageManager().sendMessage(sender, "commands.help.empty");
        plugin.getLanguageManager().sendMessage(sender, "commands.help.reload");
        plugin.getLanguageManager().sendMessage(sender, "commands.help.alerts");
        plugin.getLanguageManager().sendMessage(sender, "commands.help.debug");
        plugin.getLanguageManager().sendMessage(sender, "commands.help.info");
        plugin.getLanguageManager().sendMessage(sender, "commands.help.violations");
        plugin.getLanguageManager().sendMessage(sender, "commands.help.status");
        plugin.getLanguageManager().sendMessage(sender, "commands.help.webhook");
        sender.sendMessage("§c/hac injector [player] §8- §7Inspect injector and modified client detections");
        plugin.getLanguageManager().sendMessage(sender, "commands.help.footer");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!hasAdminPerm(sender) && !hasAlertsPerm(sender) && !hasDebugPerm(sender) && !hasWebhookPerm(sender) && !hasInjectorPerm(sender)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            if (hasAdminPerm(sender)) {
                subs.add("reload");
                subs.add("status");
                subs.add("info");
                subs.add("violations");
            }
            if (hasInjectorPerm(sender)) {
                subs.add("injector");
            }
            if (hasAlertsPerm(sender)) {
                subs.add("alerts");
            }
            if (hasDebugPerm(sender)) {
                subs.add("debug");
            }
            if (hasWebhookPerm(sender)) {
                subs.add("webhook");
            }
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("webhook") && hasWebhookPerm(sender)) {
                return Arrays.asList("status", "test", "reload").stream().filter(s -> s.startsWith(args[1].toLowerCase())).toList();
            }
            if ((args[0].equalsIgnoreCase("debug") && hasDebugPerm(sender))
                    || ((args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("violations")) && hasAdminPerm(sender))
                    || (args[0].equalsIgnoreCase("injector") && hasInjectorPerm(sender))) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase())).toList();
            }
        }

        return Collections.emptyList();
    }
}

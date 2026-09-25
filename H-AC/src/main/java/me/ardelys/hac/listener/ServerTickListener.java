package me.ardelys.hac.listener;

import me.ardelys.hac.HAC;
import me.ardelys.hac.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayDeque;
import java.util.Deque;

public class ServerTickListener {

    private final HAC plugin;
    private final Deque<Long> tickDurations = new ArrayDeque<>();
    private long lastTickTime = TimeUtil.now();
    private volatile double currentTps = 20.0;
    private BukkitTask task;

    public ServerTickListener(HAC plugin) {
        this.plugin = plugin;
        start();
    }

    public void start() {
        if (task != null) {
            task.cancel();
        }

        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = TimeUtil.now();
            long elapsed = now - lastTickTime;
            lastTickTime = now;

            tickDurations.addLast(elapsed);
            while (tickDurations.size() > 20) {
                tickDurations.removeFirst();
            }

            long total = 0;
            for (long dur : tickDurations) {
                total += dur;
            }

            if (!tickDurations.isEmpty()) {
                double avgDurationMs = (double) total / (tickDurations.size() * 20.0);
                this.currentTps = Math.min(20.0, Math.max(0.0, 1000.0 / Math.max(50.0, avgDurationMs)));
            }
        }, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        tickDurations.clear();
    }

    public double getRecentTps() {
        try {
            double[] tps = Bukkit.getTPS();
            if (tps != null && tps.length > 0 && !Double.isNaN(tps[0]) && !Double.isInfinite(tps[0])) {
                return Math.min(20.0, Math.max(0.0, tps[0]));
            }
        } catch (Throwable ignored) {
        }
        return currentTps;
    }
}

package me.ardelys.hac.logging;

import me.ardelys.hac.HAC;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncLogger {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final HAC plugin;
    private final File logFile;
    private final BlockingQueue<String> logQueue;
    private final AtomicBoolean running;
    private Thread loggingThread;

    public AsyncLogger(HAC plugin) {
        this.plugin = plugin;
        this.logQueue = new LinkedBlockingQueue<>(10000);
        this.running = new AtomicBoolean(false);

        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        String fileName = plugin.getConfig().getString("logging.file-name", "logs/h-ac.log");
        if (fileName == null || fileName.trim().isEmpty() || fileName.contains("..")) {
            fileName = "logs/h-ac.log";
        }
        File targetFile = new File(dataFolder, fileName);
        try {
            String targetPath = targetFile.getCanonicalPath();
            String basePath = dataFolder.getCanonicalPath();
            if (!targetPath.startsWith(basePath)) {
                targetFile = new File(dataFolder, "logs/h-ac.log");
            }
        } catch (Throwable t) {
            targetFile = new File(dataFolder, "logs/h-ac.log");
        }
        this.logFile = targetFile;
        if (logFile.getParentFile() != null && !logFile.getParentFile().exists()) {
            logFile.getParentFile().mkdirs();
        }
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            loggingThread = new Thread(this::processQueue, "H-AC-AsyncLogger");
            loggingThread.setDaemon(true);
            loggingThread.start();
        }
    }

    public void stop() {
        if (running.compareAndSet(true, false)) {
            if (loggingThread != null) {
                loggingThread.interrupt();
                try {
                    loggingThread.join(2000);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    public void logViolation(String player, String check, String type, double vl, int ping, double tps, String details) {
        if (!plugin.getConfig().getBoolean("logging.file-logging", true)) {
            return;
        }

        String timestamp = LocalDateTime.now().format(FORMATTER);
        String logEntry = String.format("[%s] [VIOLATION] Player: %s | Check: %s (%s) | VL: %.2f | Ping: %dms | TPS: %.2f | Info: %s",
                timestamp, player, check, type, vl, ping, tps, details);

        logQueue.offer(logEntry);

        if (plugin.getConfig().getBoolean("logging.console-logging", false)) {
            plugin.getLogger().info(logEntry);
        }
    }

    private void processQueue() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, StandardCharsets.UTF_8, true))) {
            while (running.get() || !logQueue.isEmpty()) {
                String line = logQueue.poll(250, TimeUnit.MILLISECONDS);
                if (line != null) {
                    writer.write(line);
                    writer.newLine();

                    int drainCount = 0;
                    while (drainCount < 50 && (line = logQueue.poll()) != null) {
                        writer.write(line);
                        writer.newLine();
                        drainCount++;
                    }
                    writer.flush();
                }
            }
            writer.flush();
        } catch (IOException | InterruptedException ignored) {
        }
    }
}

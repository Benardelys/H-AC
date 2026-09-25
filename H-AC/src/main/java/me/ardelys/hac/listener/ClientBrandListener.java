package me.ardelys.hac.listener;

import me.ardelys.hac.HAC;
import me.ardelys.hac.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.charset.StandardCharsets;

public class ClientBrandListener implements PluginMessageListener {

    private final HAC plugin;

    public ClientBrandListener(HAC plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
        if (!"minecraft:brand".equalsIgnoreCase(channel)) {
            return;
        }

        if (message == null || message.length == 0) {
            return;
        }

        String brand = extractBrand(message);
        if (brand.isEmpty()) {
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().get(player);
        if (data != null) {
            data.setClientBrand(brand);
        }

        if (plugin.getClientDetector() != null) {
            plugin.getClientDetector().onBrandReceived(player, brand);
        }
    }

    private String extractBrand(byte[] message) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(message);
            DataInputStream dis = new DataInputStream(bais);
            return dis.readUTF();
        } catch (Throwable ignored) {
        }

        try {
            int offset = 0;
            int length = 0;
            int bytesRead = 0;
            while (offset < message.length) {
                byte b = message[offset++];
                length |= (b & 0x7F) << (bytesRead++ * 7);
                if ((b & 0x80) == 0) break;
            }
            if (length > 0 && offset + length <= message.length) {
                return new String(message, offset, length, StandardCharsets.UTF_8).trim();
            }
        } catch (Throwable ignored) {
        }

        return new String(message, StandardCharsets.UTF_8).trim();
    }
}

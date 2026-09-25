package me.ardelys.hac.language;

import me.ardelys.hac.HAC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.file.YamlConfiguration;

public class LanguageManager {

    private final HAC plugin;
    private final Map<String, Language> loadedLanguages = new ConcurrentHashMap<>();
    private final Set<String> loggedMissingKeys = ConcurrentHashMap.newKeySet();
    private String currentLanguageCode = "tr_TR";
    private String fallbackLanguageCode = "en_US";

    public LanguageManager(HAC plugin) {
        this.plugin = plugin;
        loadLanguages();
    }

    public void loadLanguages() {
        loadedLanguages.clear();
        loggedMissingKeys.clear();

        File langFolder = new File(plugin.getDataFolder(), "languages");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        saveDefaultLanguageFile("tr_TR.yml");
        saveDefaultLanguageFile("en_US.yml");

        File[] files = langFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String code = file.getName().substring(0, file.getName().length() - 4);
                Language language = new Language(code);
                language.load(file);
                loadedLanguages.put(code.toLowerCase(), language);
            }
        }

        String configuredDefault = plugin.getConfigManager().getString("language.default", "tr_TR");
        String configuredFallback = plugin.getConfigManager().getString("language.fallback", "en_US");

        if (loadedLanguages.containsKey(configuredDefault.toLowerCase())) {
            this.currentLanguageCode = configuredDefault.toLowerCase();
        } else {
            plugin.getLogger().warning("[H-AC] Configured language '" + configuredDefault + "' not found. Falling back to en_US.");
            this.currentLanguageCode = "en_us";
        }

        if (loadedLanguages.containsKey(configuredFallback.toLowerCase())) {
            this.fallbackLanguageCode = configuredFallback.toLowerCase();
        } else {
            this.fallbackLanguageCode = "en_us";
        }
    }

    private void saveDefaultLanguageFile(String resourceName) {
        File file = new File(new File(plugin.getDataFolder(), "languages"), resourceName);
        if (!file.exists()) {
            try {
                plugin.saveResource("languages/" + resourceName, false);
            } catch (Throwable t) {
                try (InputStream in = plugin.getResource("languages/" + resourceName)) {
                    if (in != null) {
                        YamlConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
                        config.save(file);
                    }
                } catch (Throwable ignored) {
                }
            }
        }
    }

    public String getRaw(String key) {
        return getRaw(key, null);
    }

    public String getRaw(String key, Map<String, Object> placeholders) {
        if (key == null) return "";

        Language current = loadedLanguages.get(currentLanguageCode);
        String text = current != null ? current.get(key) : null;

        if (text == null) {
            Language fallback = loadedLanguages.get(fallbackLanguageCode);
            text = fallback != null ? fallback.get(key) : null;
        }

        if (text == null) {
            if (loggedMissingKeys.add(key)) {
                plugin.getLogger().warning("[H-AC] Missing translation key: " + key);
            }
            return key;
        }

        if (placeholders != null && !placeholders.isEmpty()) {
            for (Map.Entry<String, Object> entry : placeholders.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    text = text.replace("%" + entry.getKey() + "%", String.valueOf(entry.getValue()));
                }
            }
        }

        return text;
    }

    public Component getComponent(String key) {
        return getComponent(key, null);
    }

    public Component getComponent(String key, Map<String, Object> placeholders) {
        String raw = getRaw(key, placeholders);
        return LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
    }

    public void sendMessage(CommandSender sender, String key) {
        sendMessage(sender, key, null);
    }

    public void sendMessage(CommandSender sender, String key, Map<String, Object> placeholders) {
        if (sender == null || key == null) return;
        sender.sendMessage(getComponent(key, placeholders));
    }

    public void sendPrefixedMessage(CommandSender sender, String key) {
        sendPrefixedMessage(sender, key, null);
    }

    public void sendPrefixedMessage(CommandSender sender, String key, Map<String, Object> placeholders) {
        if (sender == null || key == null) return;
        String prefix = getRaw("prefix");
        String message = getRaw(key, placeholders);
        Component comp = LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + message);
        sender.sendMessage(comp);
    }

    public String getCurrentLanguageCode() {
        return currentLanguageCode;
    }

    public String getFallbackLanguageCode() {
        return fallbackLanguageCode;
    }
}

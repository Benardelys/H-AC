package me.ardelys.hac.language;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Language {

    private final String code;
    private final Map<String, String> messages = new HashMap<>();

    public Language(String code) {
        this.code = code;
    }

    public void load(File file) {
        messages.clear();
        if (file == null || !file.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(true)) {
            if (config.isString(key)) {
                messages.put(key, config.getString(key));
            }
        }
    }

    public void load(YamlConfiguration config) {
        messages.clear();
        if (config == null) {
            return;
        }

        for (String key : config.getKeys(true)) {
            if (config.isString(key)) {
                messages.put(key, config.getString(key));
            }
        }
    }

    public String get(String key) {
        if (key == null) return null;
        return messages.get(key);
    }

    public boolean has(String key) {
        if (key == null) return false;
        return messages.containsKey(key);
    }

    public String getCode() {
        return code;
    }

    public Map<String, String> getMessages() {
        return Collections.unmodifiableMap(messages);
    }
}

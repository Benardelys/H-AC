package me.ardelys.hac.language;

import java.util.HashMap;
import java.util.Map;

public final class LanguagePlaceholder {

    private final Map<String, Object> placeholders = new HashMap<>();

    private LanguagePlaceholder() {
    }

    public static LanguagePlaceholder builder() {
        return new LanguagePlaceholder();
    }

    public static Map<String, Object> of(String k1, Object v1) {
        Map<String, Object> map = new HashMap<>();
        map.put(k1, v1);
        return map;
    }

    public static Map<String, Object> of(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }

    public static Map<String, Object> of(String k1, Object v1, String k2, Object v2, String k3, Object v3) {
        Map<String, Object> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return map;
    }

    public LanguagePlaceholder add(String key, Object value) {
        if (key != null && value != null) {
            placeholders.put(key, value);
        }
        return this;
    }

    public Map<String, Object> build() {
        return new HashMap<>(placeholders);
    }
}

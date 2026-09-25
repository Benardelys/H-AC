package me.ardelys.hac.discord;

import java.util.ArrayList;
import java.util.List;

public class DiscordEmbed {

    public record Field(String name, String value, boolean inline) {}

    private String title;
    private String description;
    private int color;
    private final List<Field> fields = new ArrayList<>();
    private String footerText;
    private String timestamp;

    public DiscordEmbed setTitle(String title) {
        if (title != null) {
            this.title = title.length() > 256 ? title.substring(0, 256) : title;
        }
        return this;
    }

    public DiscordEmbed setDescription(String description) {
        if (description != null) {
            this.description = description.length() > 4096 ? description.substring(0, 4096) : description;
        }
        return this;
    }

    public DiscordEmbed setColor(int color) {
        this.color = color;
        return this;
    }

    public DiscordEmbed addField(String name, String value, boolean inline) {
        if (fields.size() >= 25) {
            return this;
        }
        String fieldName = (name == null || name.trim().isEmpty()) ? "Info" : name.trim();
        String fieldValue = (value == null || value.trim().isEmpty()) ? "N/A" : value.trim();

        if (fieldName.length() > 256) {
            fieldName = fieldName.substring(0, 256);
        }
        if (fieldValue.length() > 1024) {
            fieldValue = fieldValue.substring(0, 1024);
        }

        this.fields.add(new Field(fieldName, fieldValue, inline));
        return this;
    }

    public DiscordEmbed setFooter(String footerText) {
        if (footerText != null) {
            this.footerText = footerText.length() > 2048 ? footerText.substring(0, 2048) : footerText;
        }
        return this;
    }

    public DiscordEmbed setTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;

        if (title != null) {
            sb.append("\"title\":\"").append(escape(title)).append("\"");
            first = false;
        }

        if (description != null) {
            if (!first) sb.append(",");
            sb.append("\"description\":\"").append(escape(description)).append("\"");
            first = false;
        }

        if (color > 0) {
            if (!first) sb.append(",");
            sb.append("\"color\":").append(color);
            first = false;
        }

        if (footerText != null) {
            if (!first) sb.append(",");
            sb.append("\"footer\":{\"text\":\"").append(escape(footerText)).append("\"}");
            first = false;
        }

        if (timestamp != null) {
            if (!first) sb.append(",");
            sb.append("\"timestamp\":\"").append(escape(timestamp)).append("\"");
            first = false;
        }

        if (!fields.isEmpty()) {
            if (!first) sb.append(",");
            sb.append("\"fields\":[");
            for (int i = 0; i < fields.size(); i++) {
                Field f = fields.get(i);
                if (i > 0) sb.append(",");
                sb.append("{\"name\":\"").append(escape(f.name()))
                  .append("\",\"value\":\"").append(escape(f.value()))
                  .append("\",\"inline\":").append(f.inline()).append("}");
            }
            sb.append("]");
        }

        sb.append("}");
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < ' ') {
                        String t = "000" + Integer.toHexString(c);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}

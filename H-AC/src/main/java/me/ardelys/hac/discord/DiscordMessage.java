package me.ardelys.hac.discord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DiscordMessage {

    private final String username;
    private final String avatarUrl;
    private final List<DiscordEmbed> embeds = new ArrayList<>();
    private final boolean highPriority;

    public DiscordMessage(String username, String avatarUrl, boolean highPriority) {
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.highPriority = highPriority;
    }

    public DiscordMessage addEmbed(DiscordEmbed embed) {
        if (embed != null) {
            this.embeds.add(embed);
        }
        return this;
    }

    public List<DiscordEmbed> getEmbeds() {
        return Collections.unmodifiableList(embeds);
    }

    public boolean isHighPriority() {
        return highPriority;
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;

        if (username != null && !username.isEmpty()) {
            sb.append("\"username\":\"").append(escape(username)).append("\"");
            first = false;
        }

        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            if (!first) sb.append(",");
            sb.append("\"avatar_url\":\"").append(escape(avatarUrl)).append("\"");
            first = false;
        }

        if (!embeds.isEmpty()) {
            if (!first) sb.append(",");
            sb.append("\"embeds\":[");
            for (int i = 0; i < embeds.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(embeds.get(i).toJson());
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

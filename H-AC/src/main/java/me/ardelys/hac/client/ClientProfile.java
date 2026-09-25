package me.ardelys.hac.client;

import me.ardelys.hac.checks.CheckType;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ClientProfile {

    private final String name;
    private final List<String> brandKeywords;
    private final List<String> channelKeywords;
    private final Set<CheckType> correlatedCheckTypes;
    private final double baseWeight;

    public ClientProfile(String name, List<String> brandKeywords, List<String> channelKeywords, Set<CheckType> correlatedCheckTypes, double baseWeight) {
        this.name = name;
        this.brandKeywords = brandKeywords != null ? brandKeywords : Collections.emptyList();
        this.channelKeywords = channelKeywords != null ? channelKeywords : Collections.emptyList();
        this.correlatedCheckTypes = correlatedCheckTypes != null ? correlatedCheckTypes : Collections.emptySet();
        this.baseWeight = baseWeight;
    }

    public String getName() {
        return name;
    }

    public List<String> getBrandKeywords() {
        return brandKeywords;
    }

    public List<String> getChannelKeywords() {
        return channelKeywords;
    }

    public Set<CheckType> getCorrelatedCheckTypes() {
        return correlatedCheckTypes;
    }

    public double getBaseWeight() {
        return baseWeight;
    }

    public boolean matchesBrand(String brand) {
        if (brand == null || brand.isEmpty()) return false;
        String lower = brand.toLowerCase();
        for (String kw : brandKeywords) {
            if (lower.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    public boolean matchesChannel(String channel) {
        if (channel == null || channel.isEmpty()) return false;
        String lower = channel.toLowerCase();
        for (String kw : channelKeywords) {
            if (lower.contains(kw)) {
                return true;
            }
        }
        return false;
    }
}

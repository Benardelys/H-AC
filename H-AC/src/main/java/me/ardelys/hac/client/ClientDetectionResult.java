package me.ardelys.hac.client;

import me.ardelys.hac.checks.DetectionConfidence;

import java.util.Collections;
import java.util.List;

public class ClientDetectionResult {

    private final ClientProfile profile;
    private final DetectionConfidence confidence;
    private final double score;
    private final List<String> signals;
    private final long detectedAt;

    public ClientDetectionResult(ClientProfile profile, DetectionConfidence confidence, double score, List<String> signals, long detectedAt) {
        this.profile = profile;
        this.confidence = confidence;
        this.score = score;
        this.signals = signals != null ? Collections.unmodifiableList(signals) : Collections.emptyList();
        this.detectedAt = detectedAt;
    }

    public ClientProfile getProfile() {
        return profile;
    }

    public DetectionConfidence getConfidence() {
        return confidence;
    }

    public double getScore() {
        return score;
    }

    public List<String> getSignals() {
        return signals;
    }

    public long getDetectedAt() {
        return detectedAt;
    }

    public String getFormattedSignals() {
        if (signals.isEmpty()) return "None";
        return String.join(", ", signals);
    }
}

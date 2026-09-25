package me.ardelys.hac.client.injector;

import me.ardelys.hac.checks.DetectionConfidence;

import java.util.Collections;
import java.util.List;

public class InjectorDetectionResult {

    private final DetectionConfidence confidence;
    private final double score;
    private final List<String> detectedModules;
    private final List<String> signals;
    private final long timestamp;

    public InjectorDetectionResult(DetectionConfidence confidence, double score, List<String> detectedModules, List<String> signals, long timestamp) {
        this.confidence = confidence;
        this.score = score;
        this.detectedModules = detectedModules != null ? Collections.unmodifiableList(detectedModules) : Collections.emptyList();
        this.signals = signals != null ? Collections.unmodifiableList(signals) : Collections.emptyList();
        this.timestamp = timestamp;
    }

    public DetectionConfidence getConfidence() {
        return confidence;
    }

    public double getScore() {
        return score;
    }

    public List<String> getDetectedModules() {
        return detectedModules;
    }

    public List<String> getSignals() {
        return signals;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFormattedModules() {
        if (detectedModules.isEmpty()) return "None";
        return String.join(", ", detectedModules);
    }

    public String getFormattedSignals() {
        if (signals.isEmpty()) return "None";
        return String.join(", ", signals);
    }
}

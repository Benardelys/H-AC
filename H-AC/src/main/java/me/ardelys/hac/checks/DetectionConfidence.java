package me.ardelys.hac.checks;

public enum DetectionConfidence {
    LOW(0.65, "Low"),
    MEDIUM(1.0, "Medium"),
    HIGH(1.35, "High"),
    CRITICAL(1.75, "Critical"),
    DEFINITIVE(2.0, "Definitive");

    private final double multiplier;
    private final String displayName;

    DetectionConfidence(double multiplier, String displayName) {
        this.multiplier = multiplier;
        this.displayName = displayName;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public String getDisplayName() {
        return displayName;
    }
}

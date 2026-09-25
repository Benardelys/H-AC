package me.ardelys.hac.violation;

import me.ardelys.hac.checks.Check;

public record Violation(
        Check check,
        String checkType,
        double addedVl,
        double totalVl,
        long timestamp,
        String details
) {
}

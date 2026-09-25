package me.ardelys.hac.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MathUtil {

    private MathUtil() {
    }

    public static double hypot(double x, double z) {
        if (Double.isNaN(x) || Double.isInfinite(x) || Double.isNaN(z) || Double.isInfinite(z)) {
            return 0.0;
        }
        return Math.sqrt(x * x + z * z);
    }

    public static double hypot3D(double x, double y, double z) {
        if (Double.isNaN(x) || Double.isInfinite(x) || Double.isNaN(y) || Double.isInfinite(y)
                || Double.isNaN(z) || Double.isInfinite(z)) {
            return 0.0;
        }
        return Math.sqrt(x * x + y * y + z * z);
    }

    public static double clamp(double value, double min, double max) {
        if (Double.isNaN(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    public static float normalizeAngle(float angle) {
        if (Float.isNaN(angle) || Float.isInfinite(angle)) {
            return 0.0f;
        }
        float normalized = angle % 360.0f;
        if (normalized > 180.0f) {
            normalized -= 360.0f;
        }
        if (normalized <= -180.0f) {
            normalized += 360.0f;
        }
        return normalized;
    }

    public static float getAngleDistance(float alpha, float beta) {
        if (Float.isNaN(alpha) || Float.isInfinite(alpha) || Float.isNaN(beta) || Float.isInfinite(beta)) {
            return 0.0f;
        }
        float diff = Math.abs(alpha - beta) % 360.0f;
        return diff > 180.0f ? 360.0f - diff : diff;
    }

    public static long gcd(long a, long b) {
        a = Math.abs(a);
        b = Math.abs(b);
        while (b > 0) {
            long temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }

    public static double gcd(double a, double b) {
        if (Double.isNaN(a) || Double.isInfinite(a) || Double.isNaN(b) || Double.isInfinite(b)) {
            return 0.0;
        }
        a = Math.abs(a);
        b = Math.abs(b);
        if (a < b) {
            double temp = a;
            a = b;
            b = temp;
        }
        int iterations = 0;
        while (b >= 0.0001 && iterations < 50) {
            double remainder = a - Math.floor(a / b) * b;
            a = b;
            b = remainder;
        }
        return a;
    }

    public static double mean(long[] values, int length) {
        if (values == null || length <= 0) return 0.0;
        int count = Math.min(values.length, length);
        double sum = 0.0;
        for (int i = 0; i < count; i++) {
            sum += values[i];
        }
        return sum / count;
    }

    public static double variance(long[] values, int length) {
        if (values == null || length <= 1) return 0.0;
        int count = Math.min(values.length, length);
        double mean = 0.0;
        double m2 = 0.0;
        for (int i = 0; i < count; i++) {
            double x = values[i];
            double delta = x - mean;
            mean += delta / (i + 1);
            double delta2 = x - mean;
            m2 += delta * delta2;
        }
        return m2 / (count - 1);
    }

    public static double standardDeviation(long[] values, int length) {
        return Math.sqrt(variance(values, length));
    }

    public static double mean(Iterable<? extends Number> numbers) {
        if (numbers == null) return 0.0;
        double sum = 0.0;
        int count = 0;
        for (Number number : numbers) {
            if (number == null) continue;
            double val = number.doubleValue();
            if (!Double.isNaN(val) && !Double.isInfinite(val)) {
                sum += val;
                count++;
            }
        }
        return count == 0 ? 0.0 : sum / count;
    }

    public static double variance(Iterable<? extends Number> numbers) {
        if (numbers == null) return 0.0;
        int count = 0;
        double mean = 0.0;
        double m2 = 0.0;
        for (Number number : numbers) {
            if (number == null) continue;
            double val = number.doubleValue();
            if (Double.isNaN(val) || Double.isInfinite(val)) continue;
            count++;
            double delta = val - mean;
            mean += delta / count;
            double delta2 = val - mean;
            m2 += delta * delta2;
        }
        return count <= 1 ? 0.0 : m2 / (count - 1);
    }

    public static double standardDeviation(Iterable<? extends Number> numbers) {
        return Math.sqrt(variance(numbers));
    }

    public static double kurtosis(long[] values, int length) {
        if (values == null || length < 4) return 0.0;
        int count = Math.min(values.length, length);
        double mean = mean(values, count);
        double secondMoment = 0.0;
        double fourthMoment = 0.0;
        for (int i = 0; i < count; i++) {
            double diff = values[i] - mean;
            double diff2 = diff * diff;
            secondMoment += diff2;
            fourthMoment += diff2 * diff2;
        }
        double var = secondMoment / count;
        if (var <= 0.0) return 0.0;
        return (fourthMoment / count) / (var * var) - 3.0;
    }

    public static double round(double value, int places) {
        if (places < 0) return value;
        if (Double.isNaN(value) || Double.isInfinite(value)) return 0.0;
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}

package me.ardelys.hac.utils;

public final class TimeUtil {

    private TimeUtil() {
    }

    public static long now() {
        return System.currentTimeMillis();
    }

    public static long elapsed(long startMillis) {
        return Math.max(0L, System.currentTimeMillis() - startMillis);
    }

    public static boolean hasPassed(long startMillis, long durationMillis) {
        return elapsed(startMillis) >= durationMillis;
    }
}

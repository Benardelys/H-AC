package me.ardelys.hac.utils;

import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public final class BoundingBoxUtil {

    private BoundingBoxUtil() {
    }

    public static BoundingBox expand(BoundingBox box, double x, double y, double z) {
        if (box == null) {
            return new BoundingBox(0, 0, 0, 0, 0, 0);
        }
        return new BoundingBox(
                box.getMinX() - x,
                box.getMinY() - y,
                box.getMinZ() - z,
                box.getMaxX() + x,
                box.getMaxY() + y,
                box.getMaxZ() + z
        );
    }

    public static double distance(BoundingBox box, Vector point) {
        if (box == null || point == null) {
            return Double.MAX_VALUE;
        }
        double dx = Math.max(0.0, Math.max(box.getMinX() - point.getX(), point.getX() - box.getMaxX()));
        double dy = Math.max(0.0, Math.max(box.getMinY() - point.getY(), point.getY() - box.getMaxY()));
        double dz = Math.max(0.0, Math.max(box.getMinZ() - point.getZ(), point.getZ() - box.getMaxZ()));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static RayTraceResult rayTrace(BoundingBox box, Vector start, Vector direction, double maxDistance) {
        if (box == null || start == null || direction == null) {
            return null;
        }
        return box.rayTrace(start, direction, maxDistance);
    }
}

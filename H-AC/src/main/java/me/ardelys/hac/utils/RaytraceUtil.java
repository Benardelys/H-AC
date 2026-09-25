package me.ardelys.hac.utils;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public final class RaytraceUtil {

    private RaytraceUtil() {
    }

    public static Vector getDirection(float yaw, float pitch) {
        double rotX = Math.toRadians(yaw);
        double rotY = Math.toRadians(pitch);

        double xz = Math.cos(rotY);
        double x = -xz * Math.sin(rotX);
        double y = -Math.sin(rotY);
        double z = xz * Math.cos(rotX);

        return new Vector(x, y, z);
    }

    public static double getDistanceToBox(Location eyeLoc, Entity target) {
        if (eyeLoc == null || target == null || eyeLoc.getWorld() == null || target.getWorld() == null) {
            return Double.MAX_VALUE;
        }
        if (!eyeLoc.getWorld().equals(target.getWorld())) {
            return Double.MAX_VALUE;
        }

        BoundingBox box = target.getBoundingBox();
        Vector eye = eyeLoc.toVector();

        if (box.contains(eye)) {
            return 0.0;
        }

        Vector dir = eyeLoc.getDirection();
        if (Double.isNaN(dir.getX()) || Double.isNaN(dir.getY()) || Double.isNaN(dir.getZ())
                || Double.isInfinite(dir.getX()) || Double.isInfinite(dir.getY()) || Double.isInfinite(dir.getZ())
                || dir.lengthSquared() < 1.0E-6) {
            return BoundingBoxUtil.distance(box, eye);
        }

        RayTraceResult result = box.rayTrace(eye, dir, 6.0);
        if (result != null && result.getHitPosition() != null) {
            return eye.distance(result.getHitPosition());
        }

        return BoundingBoxUtil.distance(box, eye);
    }

    public static boolean hasLineOfSight(Location eyeLoc, Entity target, double maxDistance) {
        if (eyeLoc == null || target == null || eyeLoc.getWorld() == null || target.getWorld() == null) {
            return false;
        }
        if (!eyeLoc.getWorld().equals(target.getWorld())) {
            return false;
        }

        int chunkX = eyeLoc.getBlockX() >> 4;
        int chunkZ = eyeLoc.getBlockZ() >> 4;
        if (!eyeLoc.getWorld().isChunkLoaded(chunkX, chunkZ)) {
            return true;
        }

        Vector eye = eyeLoc.toVector();
        Vector dir = eyeLoc.getDirection();
        if (Double.isNaN(dir.getX()) || Double.isNaN(dir.getY()) || Double.isNaN(dir.getZ())
                || Double.isInfinite(dir.getX()) || Double.isInfinite(dir.getY()) || Double.isInfinite(dir.getZ())
                || dir.lengthSquared() < 1.0E-6) {
            return true;
        }

        RayTraceResult blockHit = eyeLoc.getWorld().rayTraceBlocks(
                eyeLoc,
                dir,
                maxDistance,
                FluidCollisionMode.NEVER,
                true
        );

        if (blockHit == null || blockHit.getHitPosition() == null || blockHit.getHitBlock() == null) {
            return true;
        }

        if (blockHit.getHitBlock().isPassable()) {
            return true;
        }

        double blockDistance = eye.distance(blockHit.getHitPosition());
        double entityDistance = getDistanceToBox(eyeLoc, target);

        return entityDistance <= blockDistance + 0.25;
    }
}

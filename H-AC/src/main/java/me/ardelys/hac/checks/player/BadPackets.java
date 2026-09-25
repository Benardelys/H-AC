package me.ardelys.hac.checks.player;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.Check;
import me.ardelys.hac.checks.CheckInfo;
import me.ardelys.hac.checks.CheckType;
import me.ardelys.hac.data.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@CheckInfo(name = "BadPackets", type = CheckType.PLAYER, description = "Detects malformed, out-of-range, and impossible packet parameters", defaultThreshold = 8.0)
public class BadPackets extends Check {

    public BadPackets(HAC plugin) {
        super(plugin);
    }

    public void check(PlayerData data) {
        if (data == null) return;

        Player player = data.getPlayer();
        if (player == null) return;

        float pitch = data.getPitch();
        float yaw = data.getYaw();

        if (Math.abs(pitch) > 90.0f) {
            fail(data, "A (Pitch)", 2.0, "Impossible pitch: " + pitch);
        }

        if (Float.isNaN(pitch) || Float.isInfinite(pitch) || Float.isNaN(yaw) || Float.isInfinite(yaw)) {
            fail(data, "B (NaN_Rotation)", 3.0, "NaN or Infinite rotation parameters");
        }

        Location loc = data.getCurrentLocation();
        if (loc != null) {
            if (Double.isNaN(loc.getX()) || Double.isInfinite(loc.getX())
                    || Double.isNaN(loc.getY()) || Double.isInfinite(loc.getY())
                    || Double.isNaN(loc.getZ()) || Double.isInfinite(loc.getZ())) {
                fail(data, "C (NaN_Position)", 3.0, "NaN or Infinite position coordinates");
            }

            if (Math.abs(loc.getX()) > 3.0E7 || Math.abs(loc.getZ()) > 3.0E7 || loc.getY() < -2000.0 || loc.getY() > 200000.0) {
                fail(data, "D (OutOfBounds)", 3.0, "Position coordinates outside world limits");
            }
        }

        if (data.getTeleportGraceTicks() > 10) {
            float deltaYaw = data.getDeltaYaw();
            if (deltaYaw > 360.0f) {
                fail(data, "E (ImpossibleRotation)", 2.0, "Impossible yaw delta: " + deltaYaw);
            }

            double deltaXZ = data.getDeltaXZ();
            double deltaY = Math.abs(data.getDeltaY());
            if ((deltaXZ > 35.0 || deltaY > 60.0) && data.getRespawnGraceTicks() > 20 && !player.isGliding()) {
                fail(data, "F (ImpossibleMovementDelta)", 3.0, "Extreme displacement without teleport: dXZ=" + String.format("%.2f", deltaXZ) + ", dY=" + String.format("%.2f", deltaY));
            }
        }

        if (player.isDead() && (data.getDeltaXZ() > 0.05 || Math.abs(data.getDeltaY()) > 0.05)) {
            fail(data, "G (DeadMovement)", 3.0, "Player moved while dead");
        }

        if (player.isSleeping() && data.getDeltaXZ() > 0.1) {
            fail(data, "H (SleepingMovement)", 2.0, "Player moved while sleeping");
        }

        if (data.getConsecutiveSprintSneakTicks() > 15 && player.getGameMode() == GameMode.SURVIVAL) {
            fail(data, "I (SimultaneousSprintSneak)", 2.0, "Prolonged sprint and sneak simultaneously");
        }

        long packetDelta = data.getLastMovePacketDelta();
        if (packetDelta >= 0 && packetDelta < 5 && data.getTeleportGraceTicks() > 10 && plugin.getServerTickListener().getRecentTps() >= 19.0) {
            if (data.getAirDistance() > 1.5) {
                fail(data, "J (PacketBurst)", 2.0, "Sub-tick packet burst in air: delta=" + packetDelta + "ms");
            }
        }
    }
}

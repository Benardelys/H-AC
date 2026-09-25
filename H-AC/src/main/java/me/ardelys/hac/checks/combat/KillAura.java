package me.ardelys.hac.checks.combat;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.Check;
import me.ardelys.hac.checks.CheckInfo;
import me.ardelys.hac.checks.CheckType;
import me.ardelys.hac.data.PlayerData;
import me.ardelys.hac.utils.MathUtil;
import me.ardelys.hac.utils.RaytraceUtil;
import me.ardelys.hac.utils.TimeUtil;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.UUID;

@CheckInfo(name = "KillAura", type = CheckType.COMBAT, description = "Detects automated attack aim, switch timing, and line-of-sight anomalies", defaultThreshold = 15.0)
public class KillAura extends Check {

    private double maxAngleChange;
    private long minSwitchDelayMs;

    public KillAura(HAC plugin) {
        super(plugin);
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        this.maxAngleChange = plugin.getConfigManager().getDouble("checks.killaura.max-angle-change", 45.0);
        this.minSwitchDelayMs = plugin.getConfigManager().getInt("checks.killaura.min-switch-delay-ms", 45);
    }

    public void checkAttack(PlayerData data, Entity target) {
        if (data == null || target == null) return;
        Player player = data.getPlayer();
        if (player == null || !player.isOnline()) return;

        if (target.getWorld() == null || !target.getWorld().equals(player.getWorld())) {
            return;
        }

        long now = TimeUtil.now();

        Location eyeLoc = player.getEyeLocation();
        if (!RaytraceUtil.hasLineOfSight(eyeLoc, target, 4.5)) {
            fail(data, "C (LineOfSight)", 1.5, "Attacked entity through solid obstacle");
        }

        Vector eyeVec = eyeLoc.toVector();
        Vector targetCenter = target.getBoundingBox().getCenter();
        Vector toTarget = targetCenter.subtract(eyeVec);
        double horizontalDist = Math.hypot(toTarget.getX(), toTarget.getZ());
        if (horizontalDist > 0.5) {
            Vector eyeDir = eyeLoc.getDirection();
            double dirH = Math.hypot(eyeDir.getX(), eyeDir.getZ());
            if (dirH > 0.001) {
                double dot = (eyeDir.getX() * toTarget.getX() + eyeDir.getZ() * toTarget.getZ()) / (dirH * horizontalDist);
                double angle = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot))));
                if (angle > 105.0) {
                    fail(data, "D (Angle)", 2.0, "Attacked entity outside field of view: " + String.format("%.1f", angle) + " deg");
                }
            }
        }

        long lastSwing = data.getLastSwingTime();
        if (lastSwing == 0 || (now - lastSwing > 300L)) {
            fail(data, "E (NoSwing)", 2.0, "Attacked entity without client arm swing animation");
        }

        UUID lastTargetUuid = data.getLastTargetUuid();
        if (lastTargetUuid != null && !lastTargetUuid.equals(target.getUniqueId())) {
            long timeSinceLast = now - data.getLastAttackTime();
            if (timeSinceLast < 30L) {
                fail(data, "B (MultiAura)", 2.5, "Attacked multiple distinct targets in " + timeSinceLast + "ms");
            } else if (timeSinceLast < minSwitchDelayMs) {
                fail(data, "B (Switch)", 2.0, "Target switched in " + timeSinceLast + "ms (min: " + minSwitchDelayMs + "ms)");
            }
        }

        data.setPreAttackRotation(data.getLastYaw(), data.getLastPitch(), now);
        data.setLastTarget(target);
        data.setLastAttackTime(now);
    }

    public void checkRotation(PlayerData data) {
        if (data == null) return;
        Player player = data.getPlayer();
        if (player == null || !player.isOnline()) return;

        float deltaYaw = data.getDeltaYaw();
        float deltaPitch = data.getDeltaPitch();
        float lastDeltaYaw = data.getLastDeltaYaw();
        float lastDeltaPitch = data.getLastDeltaPitch();

        long now = TimeUtil.now();
        long snapbackTime = data.getPreAttackTime();
        if (snapbackTime > 0 && (now - snapbackTime) < 160L) {
            float distToPreAttack = MathUtil.getAngleDistance(data.getYaw(), data.getPreAttackYaw());
            float distPitchToPre = Math.abs(data.getPitch() - data.getPreAttackPitch());
            if (distToPreAttack < 3.0f && distPitchToPre < 3.0f && deltaYaw > 25.0f) {
                fail(data, "G (SnapBack)", 2.0, "Immediate post-attack rotation snap-back: " + String.format("%.1f", deltaYaw) + " deg");
                data.setPreAttackRotation(0, 0, 0);
            }
        }

        if (deltaYaw > maxAngleChange && deltaPitch > 15.0f && lastDeltaYaw < 2.0f && lastDeltaPitch < 2.0f) {
            long timeSinceAttack = now - data.getLastAttackTime();
            if (timeSinceAttack < 150) {
                fail(data, "A (Snap)", 1.0, "Impossible snap: deltaYaw=" + String.format("%.2f", deltaYaw) + ", deltaPitch=" + String.format("%.2f", deltaPitch));
            }
        }

        if (deltaYaw > 0.8f && lastDeltaYaw > 0.8f) {
            long currentScaled = (long) (deltaYaw * 16777216.0);
            long lastScaled = (long) (lastDeltaYaw * 16777216.0);
            long gcd = MathUtil.gcd(currentScaled, lastScaled);
            if (gcd < 65536L && currentScaled > 167772160L) {
                fail(data, "A (GCD)", 0.5, "Non-quantized rotational step: gcd=" + gcd);
            }
        }
    }
}

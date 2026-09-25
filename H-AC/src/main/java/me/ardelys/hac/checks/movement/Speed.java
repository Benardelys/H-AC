package me.ardelys.hac.checks.movement;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.Check;
import me.ardelys.hac.checks.CheckInfo;
import me.ardelys.hac.checks.CheckType;
import me.ardelys.hac.data.PlayerData;
import me.ardelys.hac.utils.PlayerUtil;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

@CheckInfo(name = "Speed", type = CheckType.MOVEMENT, description = "Detects horizontal movement exceeding vanilla physics limits", defaultThreshold = 20.0)
public class Speed extends Check {

    public Speed(HAC plugin) {
        super(plugin);
    }

    public void check(PlayerData data) {
        if (data == null || data.isExemptMovement()) {
            return;
        }

        Player player = data.getPlayer();
        if (player == null || !player.isOnline()) return;

        double deltaXZ = data.getDeltaXZ();
        if (Double.isNaN(deltaXZ) || Double.isInfinite(deltaXZ) || deltaXZ < 0.25) {
            decayBuffer(data);
            return;
        }

        double maxSpeed = 0.36;
        if (player.isSprinting()) {
            maxSpeed = 0.65;
        }

        if (!data.isOnGround()) {
            double airLimit = (data.getLastDeltaXZ() * 0.91) + 0.04;
            maxSpeed = Math.max(maxSpeed, airLimit);
        }

        int speedAmp = PlayerUtil.getPotionLevel(player, PotionEffectType.SPEED);
        if (speedAmp > 0) {
            maxSpeed += speedAmp * 0.09;
        }

        double attrSpeed = PlayerUtil.getMovementSpeedAttribute(player);
        if (attrSpeed > 0.10) {
            maxSpeed *= (attrSpeed / 0.10);
        }

        if (data.isOnSoul() && PlayerUtil.hasSoulSpeed(player)) {
            maxSpeed += 0.35;
        }

        if (player.hasPotionEffect(PotionEffectType.DOLPHINS_GRACE)) {
            maxSpeed += 0.50;
        }

        if (data.getIceTicks() > 0) {
            maxSpeed += 0.40;
        }

        if (data.getSlimeTicks() > 0) {
            maxSpeed += 0.48;
        }

        if (data.getVelocityTicks() < 20) {
            maxSpeed += 0.60;
        }

        if (deltaXZ > maxSpeed) {
            double currentBuffer = data.getSpeedBuffer();
            double buffer = currentBuffer + (deltaXZ - maxSpeed);
            data.setSpeedBuffer(buffer);

            if (buffer > 0.45) {
                fail(data, "A", 1.5, "Speed: " + String.format("%.3f", deltaXZ) + " (max allowed: " + String.format("%.3f", maxSpeed) + ", buf=" + String.format("%.2f", buffer) + ")");
                data.setSpeedBuffer(0.20);
            }
        } else {
            decayBuffer(data);
        }

        if (data.getAirTicks() > 2 && deltaXZ > 0.32 && data.getIceTicks() == 0 && data.getSlimeTicks() == 0 && data.getVelocityTicks() > 25) {
            double moveX = data.getDeltaX();
            double moveZ = data.getDeltaZ();
            double lastMoveX = data.getLastDeltaX();
            double lastMoveZ = data.getLastDeltaZ();
            double lastSpeed = data.getLastDeltaXZ();
            if (lastSpeed > 0.25) {
                double dot = (moveX * lastMoveX + moveZ * lastMoveZ) / (deltaXZ * lastSpeed);
                if (dot < 0.25 && deltaXZ >= lastSpeed * 0.98) {
                    fail(data, "B (Strafe)", 1.5, "Sharp air vector redirection without momentum decay: dot=" + String.format("%.2f", dot));
                }
            }
        }
    }

    private void decayBuffer(PlayerData data) {
        if (data == null) return;
        double buf = data.getSpeedBuffer();
        if (buf > 0.0) {
            data.setSpeedBuffer(Math.max(0.0, buf - 0.05));
        }
    }
}

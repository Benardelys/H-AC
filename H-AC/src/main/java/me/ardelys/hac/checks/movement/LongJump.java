package me.ardelys.hac.checks.movement;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.Check;
import me.ardelys.hac.checks.CheckInfo;
import me.ardelys.hac.checks.CheckType;
import me.ardelys.hac.data.PlayerData;
import me.ardelys.hac.utils.PlayerUtil;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

@CheckInfo(name = "LongJump", type = CheckType.MOVEMENT, description = "Detects horizontal distance anomalies during jump arcs", defaultThreshold = 14.0)
public class LongJump extends Check {

    public LongJump(HAC plugin) {
        super(plugin);
    }

    public void check(PlayerData data) {
        if (data == null || data.isExemptMovement()) {
            return;
        }

        Player player = data.getPlayer();
        if (player == null || !player.isOnline()) return;

        if (data.getSlimeTicks() > 0 || data.getIceTicks() > 0 || data.getVelocityTicks() < 25
                || data.getLiquidTicks() > 0 || data.getClimbableTicks() > 0) {
            data.setAirDistance(0.0);
            return;
        }

        if (data.isOnGround()) {
            data.setAirDistance(0.0);
            return;
        }

        int airTicks = data.getAirTicks();
        if (airTicks > 0) {
            double currentDist = data.getAirDistance() + data.getDeltaXZ();
            data.setAirDistance(currentDist);

            if (airTicks > 6 && airTicks < 16) {
                int speedAmp = PlayerUtil.getPotionLevel(player, PotionEffectType.SPEED);
                double maxAllowedAirDistance = 4.7 + (speedAmp * 0.85);

                if (currentDist > maxAllowedAirDistance) {
                    fail(data, "A", 1.5, "Air jump distance " + String.format("%.2f", currentDist) + " exceeded limit (" + String.format("%.2f", maxAllowedAirDistance) + ")");
                    data.setAirDistance(0.0);
                }
            }
        }
    }
}

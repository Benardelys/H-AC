package me.ardelys.hac.checks.movement;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.Check;
import me.ardelys.hac.checks.CheckInfo;
import me.ardelys.hac.checks.CheckType;
import me.ardelys.hac.data.PlayerData;
import me.ardelys.hac.utils.PlayerUtil;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

@CheckInfo(name = "HighJump", type = CheckType.MOVEMENT, description = "Detects jumping higher than vanilla physics allow", defaultThreshold = 14.0)
public class HighJump extends Check {

    public HighJump(HAC plugin) {
        super(plugin);
    }

    public void check(PlayerData data) {
        if (data == null || data.isExemptMovement()) {
            return;
        }

        Player player = data.getPlayer();
        if (player == null || !player.isOnline()) return;

        if (data.getSlimeTicks() > 0 || data.getVelocityTicks() < 20 || data.getClimbableTicks() > 0 || data.getLiquidTicks() > 0) {
            return;
        }

        double deltaY = data.getDeltaY();
        int airTicks = data.getAirTicks();

        if (airTicks == 1 && deltaY > 0.42) {
            int jumpBoost = PlayerUtil.getPotionLevel(player, PotionEffectType.JUMP_BOOST);
            double maxJumpY = 0.42 + (jumpBoost * 0.12);

            if (deltaY > maxJumpY + 0.05) {
                fail(data, "A", 1.5, "Jump initial deltaY=" + String.format("%.3f", deltaY) + " (max allowed: " + String.format("%.3f", maxJumpY) + ")");
            }
        }
    }
}

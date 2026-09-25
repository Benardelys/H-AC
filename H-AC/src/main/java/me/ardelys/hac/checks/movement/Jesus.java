package me.ardelys.hac.checks.movement;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.Check;
import me.ardelys.hac.checks.CheckInfo;
import me.ardelys.hac.checks.CheckType;
import me.ardelys.hac.data.PlayerData;
import me.ardelys.hac.utils.PlayerUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

@CheckInfo(name = "Jesus", type = CheckType.MOVEMENT, description = "Detects water-walking and staying afloat artificially on liquid surfaces", defaultThreshold = 12.0)
public class Jesus extends Check {

    public Jesus(HAC plugin) {
        super(plugin);
    }

    public void check(PlayerData data) {
        if (data == null || data.isExemptMovement()) {
            return;
        }

        Player player = data.getPlayer();
        if (player == null || !player.isOnline()) return;

        if (PlayerUtil.hasFrostWalker(player)) {
            return;
        }

        if (player.hasPotionEffect(PotionEffectType.DOLPHINS_GRACE)) {
            return;
        }

        Location loc = data.getCurrentLocation();
        if (loc == null) return;

        boolean onLiquid = data.isOnLiquid();
        boolean inLiquid = data.isInLiquid();

        if (onLiquid && !inLiquid) {
            double deltaY = data.getDeltaY();
            double deltaXZ = data.getDeltaXZ();

            if (Math.abs(deltaY) < 0.001 && deltaXZ > 0.18) {
                fail(data, "A (SurfaceMove)", 1.5, "Moving across liquid surface without sinking: deltaXZ=" + String.format("%.2f", deltaXZ));
            }

            if (deltaXZ > 0.30) {
                fail(data, "B (Speed)", 1.0, "High horizontal speed on water surface: deltaXZ=" + String.format("%.2f", deltaXZ));
            }
        }
    }
}

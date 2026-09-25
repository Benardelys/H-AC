package me.ardelys.hac.checks.movement;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.Check;
import me.ardelys.hac.checks.CheckInfo;
import me.ardelys.hac.checks.CheckType;
import me.ardelys.hac.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

@CheckInfo(name = "Glide", type = CheckType.MOVEMENT, description = "Detects artificial slow descent or glide hacks", defaultThreshold = 12.0)
public class Glide extends Check {

    public Glide(HAC plugin) {
        super(plugin);
    }

    public void check(PlayerData data) {
        if (data == null || data.isExemptMovement()) {
            return;
        }

        Player player = data.getPlayer();
        if (player == null || !player.isOnline()) return;

        if (data.getClimbableTicks() > 0 || data.getWebTicks() > 0 || data.getLiquidTicks() > 0) {
            return;
        }
        if (player.hasPotionEffect(PotionEffectType.SLOW_FALLING) || player.hasPotionEffect(PotionEffectType.LEVITATION)) {
            return;
        }

        double deltaY = data.getDeltaY();
        double lastDeltaY = data.getLastDeltaY();

        if (data.getAirTicks() > 8 && deltaY < -0.05 && deltaY > -0.25) {
            if (Math.abs(deltaY - lastDeltaY) < 0.005) {
                fail(data, "A", 1.0, "Constant glide descent: deltaY=" + String.format("%.3f", deltaY) + " (airTicks=" + data.getAirTicks() + ")");
            }
        }
    }
}

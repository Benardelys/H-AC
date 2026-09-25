package me.ardelys.hac.checks.movement;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.Check;
import me.ardelys.hac.checks.CheckInfo;
import me.ardelys.hac.checks.CheckType;
import me.ardelys.hac.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

@CheckInfo(name = "NoFall", type = CheckType.MOVEMENT, description = "Detects spoofed onGround packets and fall damage avoidance hacks", defaultThreshold = 10.0)
public class NoFall extends Check {

    public NoFall(HAC plugin) {
        super(plugin);
    }

    @SuppressWarnings("deprecation")
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
        boolean clientOnGround = player.isOnGround();
        int serverAirTicks = data.getServerAirTicks();

        if (clientOnGround && deltaY < -0.35 && !data.isMathematicallyOnGround() && serverAirTicks > 3) {
            fail(data, "A (SpoofedGround)", 1.5, "Claimed onGround while deltaY=" + String.format("%.3f", deltaY) + " (serverAir=" + serverAirTicks + ")");
        }

        float fallDistance = player.getFallDistance();
        if (fallDistance == 0.0f && deltaY < -0.8 && serverAirTicks > 8) {
            fail(data, "B (DistanceReset)", 2.0, "Fall distance reset to 0 while falling: deltaY=" + String.format("%.3f", deltaY) + " (serverAir=" + serverAirTicks + ")");
        }
    }
}

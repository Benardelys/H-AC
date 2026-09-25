package me.ardelys.hac.checks.movement;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.Check;
import me.ardelys.hac.checks.CheckInfo;
import me.ardelys.hac.checks.CheckType;
import me.ardelys.hac.data.PlayerData;
import me.ardelys.hac.utils.PlayerUtil;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

@CheckInfo(name = "Fly", type = CheckType.MOVEMENT, description = "Detects airborne hovering, unnatural ascent, and gravity bypass", defaultThreshold = 15.0)
public class Fly extends Check {

    public Fly(HAC plugin) {
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
        if (player.hasPotionEffect(PotionEffectType.LEVITATION) || player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
            return;
        }
        if (data.getSlimeTicks() > 0 || data.getVelocityTicks() < 20) {
            return;
        }

        int airTicks = data.getAirTicks();
        double deltaY = data.getDeltaY();

        if (airTicks > 5 && Math.abs(deltaY) < 0.005) {
            fail(data, "A (Hover)", 1.5, "Hovering in air for " + airTicks + " ticks with deltaY near zero");
            return;
        }

        if (airTicks > 12 && deltaY < 0.0 && deltaY > -0.04 && Math.abs(deltaY - data.getLastDeltaY()) < 0.002) {
            fail(data, "D (SlowGlide)", 1.5, "Unnatural mid-air floating without gravity acceleration: deltaY=" + String.format("%.3f", deltaY));
            return;
        }

        int jumpBoost = PlayerUtil.getPotionLevel(player, PotionEffectType.JUMP_BOOST);
        int maxAscentTicks = 4 + (jumpBoost * 3);
        if (airTicks > maxAscentTicks && deltaY > 0.0) {
            fail(data, "B (Ascent)", 2.0, "Ascending in mid-air: deltaY=" + String.format("%.3f", deltaY) + " (airTicks=" + airTicks + ")");
            return;
        }

        if (deltaY > 0.35 && airTicks > 2 && data.getServerAirTicks() > 2 && !data.isMathematicallyOnGround() && !data.isLastOnGround()) {
            fail(data, "C (AirJump)", 2.0, "Jumped while in mid-air: deltaY=" + String.format("%.3f", deltaY) + " (airTicks=" + airTicks + ", serverAir=" + data.getServerAirTicks() + ")");
        }
    }
}

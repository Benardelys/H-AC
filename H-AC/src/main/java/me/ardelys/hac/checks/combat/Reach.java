package me.ardelys.hac.checks.combat;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.Check;
import me.ardelys.hac.checks.CheckInfo;
import me.ardelys.hac.checks.CheckType;
import me.ardelys.hac.data.PlayerData;
import me.ardelys.hac.utils.RaytraceUtil;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

@CheckInfo(name = "Reach", type = CheckType.COMBAT, description = "Detects attacks hitting entities beyond the legitimate server reach limit", defaultThreshold = 12.0)
public class Reach extends Check {

    private double maxReach;

    public Reach(HAC plugin) {
        super(plugin);
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        this.maxReach = plugin.getConfigManager().getDouble("checks.reach.max-reach", 3.05);
    }

    public void check(PlayerData data, Entity target) {
        if (data == null || target == null) return;
        Player player = data.getPlayer();
        if (player == null || !player.isOnline()) return;

        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        if (target.getWorld() == null || !target.getWorld().equals(player.getWorld())) {
            return;
        }

        double distance = RaytraceUtil.getDistanceToBox(player.getEyeLocation(), target);
        if (Double.isNaN(distance) || Double.isInfinite(distance)) {
            return;
        }

        data.setLastReach(distance);

        int ping = data.getPing();
        double pingAllowance = Math.min(0.55, (ping / 1000.0) * 2.0);

        Vector targetVel = target.getVelocity();
        double targetSpeed = Math.sqrt(targetVel.getX() * targetVel.getX() + targetVel.getZ() * targetVel.getZ());
        double motionAllowance = Math.min(0.35, targetSpeed * (ping / 1000.0));

        double baseReach = maxReach;
        try {
            org.bukkit.attribute.AttributeInstance inst = player.getAttribute(org.bukkit.attribute.Attribute.ENTITY_INTERACTION_RANGE);
            if (inst != null) {
                baseReach = Math.max(maxReach, inst.getValue() + 0.05);
            }
        } catch (Throwable ignored) {
        }

        double threshold = baseReach + pingAllowance + motionAllowance;

        if (distance > threshold && distance < 10.0) {
            double excess = distance - threshold;
            double addedVl = Math.max(1.0, excess * 4.0);
            fail(data, "A", addedVl, "Reach: " + String.format("%.2f", distance) + " blocks (max allowed: " + String.format("%.2f", threshold) + ")");
        }
    }
}

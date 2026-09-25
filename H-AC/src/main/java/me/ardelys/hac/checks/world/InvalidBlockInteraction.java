package me.ardelys.hac.checks.world;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.Check;
import me.ardelys.hac.checks.CheckInfo;
import me.ardelys.hac.checks.CheckType;
import me.ardelys.hac.data.PlayerData;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

@CheckInfo(name = "InvalidBlockInteraction", type = CheckType.WORLD, description = "Detects block interactions through walls or beyond interaction reach", defaultThreshold = 10.0)
public class InvalidBlockInteraction extends Check {

    private double maxReach;

    public InvalidBlockInteraction(HAC plugin) {
        super(plugin);
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        this.maxReach = plugin.getConfigManager().getDouble("checks.invalidblockinteraction.max-interaction-reach", 5.5);
    }

    public void check(PlayerData data, Block block) {
        if (data == null || block == null) return;
        Player player = data.getPlayer();
        if (player == null || !player.isOnline()) return;

        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        Location eye = player.getEyeLocation();
        Location blockCenter = block.getLocation().clone().add(0.5, 0.5, 0.5);

        if (eye.getWorld() == null || !eye.getWorld().equals(block.getWorld())) {
            return;
        }

        double distance = eye.distance(blockCenter);

        double allowedReach = maxReach;
        try {
            org.bukkit.attribute.AttributeInstance inst = player.getAttribute(org.bukkit.attribute.Attribute.BLOCK_INTERACTION_RANGE);
            if (inst != null) {
                allowedReach = Math.max(maxReach, inst.getValue() + 1.0);
            }
        } catch (Throwable ignored) {
        }

        if (distance > allowedReach) {
            fail(data, "A (Reach)", 1.5, "Interacted with block at " + String.format("%.2f", distance) + " blocks (max: " + String.format("%.2f", allowedReach) + ")");
            return;
        }

        if (distance < 0.3) {
            return;
        }

        Vector diff = blockCenter.toVector().subtract(eye.toVector());
        if (diff.lengthSquared() < 1.0E-6) {
            return;
        }
        Vector dir = diff.normalize();
        RayTraceResult hit = eye.getWorld().rayTraceBlocks(
                eye,
                dir,
                Math.max(0.1, distance - 0.5),
                FluidCollisionMode.NEVER,
                true
        );

        if (hit != null && hit.getHitBlock() != null && !hit.getHitBlock().equals(block)) {
            if (hit.getHitBlock().getType().isOccluding() && !hit.getHitBlock().isPassable()) {
                fail(data, "B (Wall)", 1.5, "Interacted with block through solid " + hit.getHitBlock().getType().name());
            }
        }
    }
}

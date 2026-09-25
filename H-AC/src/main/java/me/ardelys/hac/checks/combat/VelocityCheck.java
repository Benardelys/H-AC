package me.ardelys.hac.checks.combat;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.Check;
import me.ardelys.hac.checks.CheckInfo;
import me.ardelys.hac.checks.CheckType;
import me.ardelys.hac.data.PlayerData;
import me.ardelys.hac.utils.MathUtil;
import me.ardelys.hac.utils.PlayerUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

@CheckInfo(name = "Velocity", type = CheckType.COMBAT, description = "Detects Anti-Knockback and velocity modification", defaultThreshold = 14.0)
public class VelocityCheck extends Check {

    private double minHorizontalFactor;
    private double minVerticalFactor;

    public VelocityCheck(HAC plugin) {
        super(plugin);
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        this.minHorizontalFactor = plugin.getConfigManager().getDouble("checks.velocity.min-horizontal-factor", 0.40);
        this.minVerticalFactor = plugin.getConfigManager().getDouble("checks.velocity.min-vertical-factor", 0.40);
    }

    public void check(PlayerData data) {
        if (data == null || data.isVelocityHandled() || data.isExemptMovement()) {
            return;
        }

        Player player = data.getPlayer();
        if (player == null || !player.isOnline()) {
            data.setVelocityHandled(true);
            return;
        }

        int ping = data.getPing();
        int latencyTicks = Math.max(1, ping / 50);
        int velocityTicks = data.getVelocityTicks();

        if (velocityTicks < latencyTicks) {
            return;
        }

        if (velocityTicks > latencyTicks + 3) {
            data.setVelocityHandled(true);
            return;
        }

        if (data.getWebTicks() > 0 || data.getClimbableTicks() > 0 || data.getLiquidTicks() > 0) {
            data.setVelocityHandled(true);
            return;
        }

        Location loc = data.getCurrentLocation();
        if (loc == null || loc.getWorld() == null) {
            data.setVelocityHandled(true);
            return;
        }

        double kbRes = PlayerUtil.getKnockbackResistance(player);
        if (kbRes >= 0.85) {
            data.setVelocityHandled(true);
            return;
        }

        double effectiveMinH = Math.max(0.08, minHorizontalFactor * (1.0 - kbRes));
        double effectiveMinV = Math.max(0.08, minVerticalFactor * (1.0 - kbRes));

        Vector expected = data.getExpectedVelocity();
        double expectedH = MathUtil.hypot(expected.getX(), expected.getZ());
        double actualH = data.getDeltaXZ();

        boolean horizontalObstacle = false;
        if (expectedH > 0.1) {
            double checkX = loc.getX() + (expected.getX() > 0 ? 0.4 : -0.4);
            double checkZ = loc.getZ() + (expected.getZ() > 0 ? 0.4 : -0.4);
            int cx = ((int) Math.floor(checkX)) >> 4;
            int cz = ((int) Math.floor(checkZ)) >> 4;
            if (loc.getWorld().isChunkLoaded(cx, cz)) {
                Block bFeet = loc.getWorld().getBlockAt((int) Math.floor(checkX), loc.getBlockY(), (int) Math.floor(checkZ));
                Block bHead = loc.getWorld().getBlockAt((int) Math.floor(checkX), loc.getBlockY() + 1, (int) Math.floor(checkZ));
                if (bFeet.getType().isSolid() || bHead.getType().isSolid()) {
                    horizontalObstacle = true;
                }
            }
        }

        if (expectedH > 0.20 && !horizontalObstacle) {
            double ratioH = actualH / expectedH;
            if (ratioH < effectiveMinH) {
                fail(data, "A (Horizontal)", 1.5, "Velocity reduction: ratio=" + String.format("%.2f", ratioH) + " (min: " + String.format("%.2f", effectiveMinH) + ")");
            }
        }

        double expectedV = expected.getY();
        double actualV = data.getDeltaY();
        if (expectedV > 0.22) {
            boolean ceilingObstacle = false;
            int cx = loc.getBlockX() >> 4;
            int cz = loc.getBlockZ() >> 4;
            if (loc.getWorld().isChunkLoaded(cx, cz)) {
                Block ceiling1 = loc.getWorld().getBlockAt(loc.getBlockX(), loc.getBlockY() + 2, loc.getBlockZ());
                Block ceiling2 = loc.getWorld().getBlockAt(loc.getBlockX(), loc.getBlockY() + 3, loc.getBlockZ());
                if (ceiling1.getType().isSolid() || ceiling2.getType().isSolid()) {
                    ceilingObstacle = true;
                }
            }
            if (!ceilingObstacle) {
                if (actualV < 0.05) {
                    fail(data, "B (Vertical)", 2.0, "Vertical knockback cancelled: deltaY=" + String.format("%.3f", actualV) + " (expected: " + String.format("%.3f", expectedV) + ")");
                } else {
                    double ratioV = actualV / expectedV;
                    if (ratioV < effectiveMinV) {
                        fail(data, "B (Vertical)", 1.5, "Vertical knockback reduction: ratio=" + String.format("%.2f", ratioV) + " (min: " + String.format("%.2f", effectiveMinV) + ")");
                    }
                }
            }
        }
        data.setVelocityHandled(true);
    }
}

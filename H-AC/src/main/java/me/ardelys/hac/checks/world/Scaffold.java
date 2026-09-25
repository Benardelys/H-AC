package me.ardelys.hac.checks.world;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.Check;
import me.ardelys.hac.checks.CheckInfo;
import me.ardelys.hac.checks.CheckType;
import me.ardelys.hac.data.PlayerData;
import me.ardelys.hac.utils.TimeUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

@CheckInfo(name = "Scaffold", type = CheckType.WORLD, description = "Detects automated bridging, headless placement, and scaffold hacks", defaultThreshold = 14.0)
public class Scaffold extends Check {

    public Scaffold(HAC plugin) {
        super(plugin);
    }

    public void check(PlayerData data, Block placedBlock, BlockFace face) {
        if (data == null || placedBlock == null) return;
        Player player = data.getPlayer();
        if (player == null || !player.isOnline()) return;

        if (player.isSneaking()) {
            return;
        }

        Location eye = player.getEyeLocation();
        float pitch = eye.getPitch();

        Location blockLoc = placedBlock.getLocation();

        if (blockLoc.getY() < player.getLocation().getY() && pitch < 25.0f) {
            double distXZ = Math.hypot(eye.getX() - (blockLoc.getX() + 0.5), eye.getZ() - (blockLoc.getZ() + 0.5));
            if (distXZ < 1.2 && player.isSprinting()) {
                fail(data, "A (Headless)", 1.5, "Placed block below while sprinting with pitch=" + String.format("%.1f", pitch));
            }
        }

        if (face == BlockFace.UP && eye.getY() < blockLoc.getY() - 0.25) {
            fail(data, "B (InvalidFace)", 1.0, "Placed against top face while eye level is below block");
        } else if (face == BlockFace.DOWN && eye.getY() > blockLoc.getY() + 1.25) {
            fail(data, "B (InvalidFace)", 1.0, "Placed against bottom face while eye level is above block");
        }

        Vector toBlock = new Vector(blockLoc.getX() + 0.5 - eye.getX(), blockLoc.getY() + 0.5 - eye.getY(), blockLoc.getZ() + 0.5 - eye.getZ());
        Vector dir = eye.getDirection();
        if (toBlock.lengthSquared() > 1.44 && dir.lengthSquared() > 0.001) {
            double dot = dir.normalize().dot(toBlock.normalize());
            if (dot < -0.15) {
                fail(data, "C (Angle)", 1.5, "Placed block behind player field of vision: dot=" + String.format("%.2f", dot));
            }
        }

        long now = TimeUtil.now();
        long lastPlace = data.getLastBlockPlaceTime();
        if (lastPlace > 0) {
            long placeInterval = now - lastPlace;
            if (placeInterval < 45L && data.getDeltaXZ() > 0.2) {
                fail(data, "D (RapidCadence)", 1.0, "Automated fast placement while moving: interval=" + placeInterval + "ms");
            }
        }
        data.setLastBlockPlaceTime(now);
    }
}

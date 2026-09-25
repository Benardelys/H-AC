package me.ardelys.hac.checks.movement;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.Check;
import me.ardelys.hac.checks.CheckInfo;
import me.ardelys.hac.checks.CheckType;
import me.ardelys.hac.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

@CheckInfo(name = "Phase", type = CheckType.MOVEMENT, description = "Detects clipping or phasing through solid blocks and walls", defaultThreshold = 10.0)
public class Phase extends Check {

    public Phase(HAC plugin) {
        super(plugin);
    }

    public void check(PlayerData data) {
        if (data == null || data.isExemptMovement()) {
            return;
        }

        Player player = data.getPlayer();
        if (player == null || !player.isOnline()) return;

        Location to = data.getCurrentLocation();
        if (to == null || to.getWorld() == null) return;

        Block block = to.getBlock();
        if (block.isPassable()) {
            return;
        }

        Material mat = block.getType();
        if (mat.isSolid() && mat.isOccluding()) {
            if (Tag.STAIRS.isTagged(mat)
                    || Tag.SLABS.isTagged(mat)
                    || Tag.DOORS.isTagged(mat)
                    || Tag.FENCE_GATES.isTagged(mat)
                    || Tag.TRAPDOORS.isTagged(mat)
                    || Tag.BEDS.isTagged(mat)
                    || mat == Material.PISTON
                    || mat == Material.STICKY_PISTON
                    || mat == Material.PISTON_HEAD
                    || mat == Material.MOVING_PISTON) {
                return;
            }

            fail(data, "A", 2.0, "Inside solid block: " + mat.name());
        }
    }
}

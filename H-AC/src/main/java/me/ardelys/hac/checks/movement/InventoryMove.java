package me.ardelys.hac.checks.movement;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.Check;
import me.ardelys.hac.checks.CheckInfo;
import me.ardelys.hac.checks.CheckType;
import me.ardelys.hac.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;

@CheckInfo(name = "InventoryMove", type = CheckType.MOVEMENT, description = "Detects moving or sprinting while having container inventories open", defaultThreshold = 12.0)
public class InventoryMove extends Check {

    public InventoryMove(HAC plugin) {
        super(plugin);
    }

    public void check(PlayerData data) {
        if (data == null || data.isExemptMovement()) {
            return;
        }

        Player player = data.getPlayer();
        if (player == null || !player.isOnline()) return;

        if (data.getIceTicks() > 0 || data.getSlimeTicks() > 0 || data.getVelocityTicks() < 15 || data.getLiquidTicks() > 0 || data.getGroundTicks() <= 1) {
            return;
        }

        if (data.getInventoryOpenTicks() <= 2 || (me.ardelys.hac.utils.TimeUtil.now() - data.getLastInventoryOpenTime() < 200L)) {
            return;
        }

        InventoryView view = player.getOpenInventory();
        InventoryType type = view.getType();

        if (type != InventoryType.CRAFTING) {
            double deltaXZ = data.getDeltaXZ();
            if (deltaXZ > 0.20 || player.isSprinting()) {
                fail(data, "A", 1.5, "Moving (deltaXZ=" + String.format("%.2f", deltaXZ) + ") with open " + type.name() + " container");
            }
        }
    }
}

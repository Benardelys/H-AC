package me.ardelys.hac.checks.movement;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.Check;
import me.ardelys.hac.checks.CheckInfo;
import me.ardelys.hac.checks.CheckType;
import me.ardelys.hac.data.PlayerData;
import me.ardelys.hac.utils.PlayerUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@CheckInfo(name = "NoSlow", type = CheckType.MOVEMENT, description = "Detects sprint/walk speed bypass while using items or eating", defaultThreshold = 15.0)
public class NoSlow extends Check {

    public NoSlow(HAC plugin) {
        super(plugin);
    }

    public void check(PlayerData data) {
        if (data == null || data.isExemptMovement()) {
            return;
        }

        Player player = data.getPlayer();
        if (player == null || !player.isOnline()) return;

        if (data.getIceTicks() > 0 || data.getSlimeTicks() > 0 || data.getVelocityTicks() < 15 || data.getGroundTicks() <= 2) {
            return;
        }

        if (player.isHandRaised()) {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            ItemStack offHand = player.getInventory().getItemInOffHand();

            boolean slowingItem = PlayerUtil.isMovementSlowingItem(mainHand) || PlayerUtil.isMovementSlowingItem(offHand);
            if (!slowingItem) {
                return;
            }

            double deltaXZ = data.getDeltaXZ();
            if (deltaXZ > 0.20 || player.isSprinting()) {
                fail(data, "A", 1.5, "Moving at " + String.format("%.3f", deltaXZ) + " blocks/tick while using movement-slowing item");
            }
        }
    }
}

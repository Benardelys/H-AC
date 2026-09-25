package me.ardelys.hac.checks.combat;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.Check;
import me.ardelys.hac.checks.CheckInfo;
import me.ardelys.hac.checks.CheckType;
import me.ardelys.hac.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

@CheckInfo(name = "Criticals", type = CheckType.COMBAT, description = "Detects packet, mini-hop, and impossible critical attacks", defaultThreshold = 10.0)
public class Criticals extends Check {

    public Criticals(HAC plugin) {
        super(plugin);
    }

    @SuppressWarnings("deprecation")
    public void check(PlayerData data) {
        if (data == null) return;
        Player player = data.getPlayer();
        if (player == null || !player.isOnline()) return;

        if (data.isExemptMovement()) {
            return;
        }

        if (player.getInventory().getItemInMainHand().getType() == org.bukkit.Material.MACE) {
            return;
        }

        if (data.getClimbableTicks() > 0 || data.getLiquidTicks() > 0 || data.getWebTicks() > 0) {
            return;
        }

        if (player.hasPotionEffect(PotionEffectType.LEVITATION) || player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
            return;
        }

        double deltaY = data.getDeltaY();
        float fallDistance = player.getFallDistance();

        if (fallDistance > 0.0f && data.isMathematicallyOnGround() && Math.abs(deltaY) < 0.0001 && data.getGroundTicks() > 2) {
            fail(data, "A (Packet)", 1.5, "Spoofed fall distance " + String.format("%.2f", fallDistance) + " while flat on ground");
        }

        if (!player.isOnGround() && !data.isMathematicallyOnGround() && deltaY > 0.001 && deltaY < 0.08 && data.getAirTicks() <= 1 && data.getVelocityTicks() > 15) {
            fail(data, "B (MicroHop)", 1.0, "Impossible vertical delta for critical jump: " + String.format("%.4f", deltaY));
        }
    }
}

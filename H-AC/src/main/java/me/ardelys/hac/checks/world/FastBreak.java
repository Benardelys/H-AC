package me.ardelys.hac.checks.world;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.Check;
import me.ardelys.hac.checks.CheckInfo;
import me.ardelys.hac.checks.CheckType;
import me.ardelys.hac.data.PlayerData;
import me.ardelys.hac.utils.PlayerUtil;
import me.ardelys.hac.utils.TimeUtil;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

@CheckInfo(name = "FastBreak", type = CheckType.WORLD, description = "Detects accelerated block breaking and instabreak hacks", defaultThreshold = 12.0)
public class FastBreak extends Check {

    public FastBreak(HAC plugin) {
        super(plugin);
    }

    public void check(PlayerData data, Block block) {
        if (data == null || block == null) return;
        Player player = data.getPlayer();
        if (player == null || !player.isOnline() || player.getGameMode() == GameMode.CREATIVE) return;

        long now = TimeUtil.now();
        long elapsed = now - data.getLastBlockBreakTime();
        data.setLastBlockBreakTime(now);

        float hardness = block.getType().getHardness();
        if (hardness <= 0.0f) {
            return;
        }

        int haste = PlayerUtil.getPotionLevel(player, PotionEffectType.HASTE);
        ItemStack tool = player.getInventory().getItemInMainHand();
        int efficiency = tool.getEnchantmentLevel(Enchantment.EFFICIENCY);

        if (haste >= 2 && efficiency >= 5 && hardness <= 1.5f) {
            return;
        }

        if (efficiency >= 5 && hardness <= 0.4f) {
            return;
        }

        if (hardness > 1.5f && elapsed < 40 && elapsed > 0) {
            fail(data, "A", 2.0, "Instantly broke " + block.getType().name() + " (hardness=" + hardness + ") in " + elapsed + "ms");
        }
    }
}

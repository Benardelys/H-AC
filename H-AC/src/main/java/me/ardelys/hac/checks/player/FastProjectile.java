package me.ardelys.hac.checks.player;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.Check;
import me.ardelys.hac.checks.CheckInfo;
import me.ardelys.hac.checks.CheckType;
import me.ardelys.hac.data.PlayerData;
import me.ardelys.hac.utils.TimeUtil;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@CheckInfo(name = "FastProjectile", type = CheckType.PLAYER, description = "Detects rapid-firing or projectile spamming hacks", defaultThreshold = 10.0)
public class FastProjectile extends Check {

    private long minThrowDelayMs;

    public FastProjectile(HAC plugin) {
        super(plugin);
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        this.minThrowDelayMs = plugin.getConfigManager().getInt("checks.fastprojectile.min-throw-delay-ms", 75);
    }

    public void check(PlayerData data) {
        if (data == null) return;

        Player player = data.getPlayer();
        if (player != null) {
            ItemStack main = player.getInventory().getItemInMainHand();
            ItemStack off = player.getInventory().getItemInOffHand();
            if ((main.getType() == Material.CROSSBOW && main.getEnchantmentLevel(Enchantment.MULTISHOT) > 0)
                    || (off.getType() == Material.CROSSBOW && off.getEnchantmentLevel(Enchantment.MULTISHOT) > 0)) {
                return;
            }
        }

        long now = TimeUtil.now();
        long elapsed = now - data.getLastProjectileTime();
        data.setLastProjectileTime(now);

        if (elapsed < minThrowDelayMs && elapsed > 0) {
            fail(data, "A", 1.0, "Fired projectile with delay of " + elapsed + "ms (min: " + minThrowDelayMs + "ms)");
        }
    }
}

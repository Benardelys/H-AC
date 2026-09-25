package me.ardelys.hac.checks.player;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.Check;
import me.ardelys.hac.checks.CheckInfo;
import me.ardelys.hac.checks.CheckType;
import me.ardelys.hac.data.PlayerData;
import me.ardelys.hac.utils.TimeUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@CheckInfo(name = "FastEat", type = CheckType.PLAYER, description = "Detects accelerated food and potion consumption", defaultThreshold = 10.0)
public class FastEat extends Check {

    private int minEatTicks;

    public FastEat(HAC plugin) {
        super(plugin);
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        this.minEatTicks = plugin.getConfigManager().getInt("checks.fasteat.min-eat-ticks", 24);
    }

    public void check(PlayerData data, ItemStack item) {
        if (data == null || item == null) return;

        long now = TimeUtil.now();
        long minMillis = (item.getType() == Material.DRIED_KELP ? 14 : minEatTicks) * 50L;

        long startTime = data.getConsumeStartTime();
        if (startTime > 0) {
            long duration = now - startTime;
            data.setConsumeStartTime(0);
            if (duration < minMillis && duration > 0) {
                fail(data, "A (Duration)", 1.5, "Consumed " + item.getType().name() + " in " + duration + "ms (min: " + minMillis + "ms)");
                data.setLastItemConsumeTime(now);
                return;
            }
        }

        long lastConsume = data.getLastItemConsumeTime();
        if (lastConsume > 0) {
            long gap = now - lastConsume;
            if (gap < minMillis && gap > 0) {
                fail(data, "B (Consecutive)", 1.5, "Consecutive consumption in " + gap + "ms (min: " + minMillis + "ms)");
            }
        }

        data.setLastItemConsumeTime(now);
    }
}

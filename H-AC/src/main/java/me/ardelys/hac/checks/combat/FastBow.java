package me.ardelys.hac.checks.combat;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.Check;
import me.ardelys.hac.checks.CheckInfo;
import me.ardelys.hac.checks.CheckType;
import me.ardelys.hac.data.PlayerData;
import me.ardelys.hac.utils.TimeUtil;

@CheckInfo(name = "FastBow", type = CheckType.COMBAT, description = "Detects rapid-firing or instant-draw bow hacks", defaultThreshold = 8.0)
public class FastBow extends Check {

    private int minDrawTicks;

    public FastBow(HAC plugin) {
        super(plugin);
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        this.minDrawTicks = plugin.getConfigManager().getInt("checks.fastbow.min-draw-ticks", 3);
    }

    public void check(PlayerData data, float force) {
        if (data == null) return;

        long now = TimeUtil.now();
        long lastShoot = data.getLastBowShootTime();
        long drawStart = data.getBowDrawStartTime();
        data.setLastBowShootTime(now);

        long minMillis = minDrawTicks * 50L;

        if (drawStart > 0) {
            long drawDuration = now - drawStart;
            if (force > 0.5f && drawDuration < minMillis) {
                fail(data, "A", 1.5, "Bow released too fast: force " + String.format("%.2f", force) + " in " + drawDuration + "ms (min: " + minMillis + "ms)");
            }
            data.setBowDrawStartTime(0);
        } else if (lastShoot > 0) {
            long elapsed = now - lastShoot;
            if (force > 0.5f && elapsed < minMillis && elapsed > 0) {
                fail(data, "B", 1.5, "Consecutive shot arrow with force " + String.format("%.2f", force) + " in " + elapsed + "ms (min: " + minMillis + "ms)");
            }
        }
    }
}

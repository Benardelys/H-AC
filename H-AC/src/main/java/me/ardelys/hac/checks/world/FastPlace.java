package me.ardelys.hac.checks.world;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.Check;
import me.ardelys.hac.checks.CheckInfo;
import me.ardelys.hac.checks.CheckType;
import me.ardelys.hac.data.PlayerData;
import me.ardelys.hac.utils.TimeUtil;

@CheckInfo(name = "FastPlace", type = CheckType.WORLD, description = "Detects accelerated block placement delays", defaultThreshold = 12.0)
public class FastPlace extends Check {

    private long minPlaceDelayMs;

    public FastPlace(HAC plugin) {
        super(plugin);
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        this.minPlaceDelayMs = plugin.getConfigManager().getInt("checks.fastplace.min-place-delay-ms", 45);
    }

    public void check(PlayerData data) {
        if (data == null) return;

        long now = TimeUtil.now();
        long elapsed = now - data.getLastBlockPlaceTime();
        data.setLastBlockPlaceTime(now);

        if (elapsed < minPlaceDelayMs && elapsed > 0) {
            int buf = data.getFastPlaceBuffer() + 1;
            data.setFastPlaceBuffer(buf);
            if (buf >= 2) {
                fail(data, "A", 1.0, "Rapid block placement: " + elapsed + "ms (min: " + minPlaceDelayMs + "ms, streak=" + buf + ")");
                data.setFastPlaceBuffer(1);
            }
        } else if (elapsed > 250) {
            data.setFastPlaceBuffer(0);
        }
    }
}

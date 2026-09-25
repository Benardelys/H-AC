package me.ardelys.hac.checks.combat;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.Check;
import me.ardelys.hac.checks.CheckInfo;
import me.ardelys.hac.checks.CheckType;
import me.ardelys.hac.data.PlayerData;
import me.ardelys.hac.utils.MathUtil;

import java.util.List;

@CheckInfo(name = "AutoClicker", type = CheckType.COMBAT, description = "Detects impossible clicking speeds and automated click patterns/macros", defaultThreshold = 16.0)
public class AutoClicker extends Check {

    private int maxCps;
    private double minStdDev;

    public AutoClicker(HAC plugin) {
        super(plugin);
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        this.maxCps = plugin.getConfigManager().getInt("checks.autoclicker.max-cps", 20);
        this.minStdDev = plugin.getConfigManager().getDouble("checks.autoclicker.min-std-dev", 4.5);
    }

    public void check(PlayerData data) {
        if (data == null) return;

        int currentCps = data.getCpsLastSecond();

        if (currentCps > maxCps) {
            fail(data, "A (CPS)", 1.5, "CPS: " + currentCps + " (max allowed: " + maxCps + ")");
        }

        if (currentCps > 10) {
            double stdDev = data.getClickStdDev();
            if (stdDev < minStdDev) {
                fail(data, "B (Consistency)", 1.0, "Suspicious click regularity: stdDev=" + String.format("%.2f", stdDev));
                data.clearClickSamples();
            } else {
                double kurt = data.getClickKurtosis();
                if (kurt < -1.45 && currentCps > 12) {
                    fail(data, "C (RandomizedMacro)", 1.0, "Unnatural platykurtic click distribution: kurtosis=" + String.format("%.2f", kurt));
                    data.clearClickSamples();
                }
            }
        }
    }
}

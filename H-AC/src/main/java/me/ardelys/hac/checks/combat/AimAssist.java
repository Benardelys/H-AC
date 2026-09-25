package me.ardelys.hac.checks.combat;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.Check;
import me.ardelys.hac.checks.CheckInfo;
import me.ardelys.hac.checks.CheckType;
import me.ardelys.hac.data.PlayerData;
import me.ardelys.hac.utils.TimeUtil;

@CheckInfo(name = "AimAssist", type = CheckType.COMBAT, description = "Detects artificial smoothing, aim-locking, and aim assistance", defaultThreshold = 18.0)
public class AimAssist extends Check {

    public AimAssist(HAC plugin) {
        super(plugin);
    }

    public void check(PlayerData data) {
        if (data == null) return;

        if (TimeUtil.now() - data.getLastAttackTime() > 1200) {
            data.setPitchLockTicks(0);
            return;
        }

        float deltaYaw = data.getDeltaYaw();
        float deltaPitch = data.getDeltaPitch();

        if (deltaYaw > 3.5f && deltaPitch == 0.0f) {
            int currentTicks = data.getPitchLockTicks() + 1;
            data.setPitchLockTicks(currentTicks);
            if (currentTicks >= 6) {
                fail(data, "A (PitchLock)", 1.0, "Sustained pitch lock for " + currentTicks + " ticks during yaw rotation");
                data.setPitchLockTicks(2);
            }
        } else if (deltaPitch > 0.1f) {
            int currentTicks = data.getPitchLockTicks();
            if (currentTicks > 0) {
                data.setPitchLockTicks(currentTicks - 1);
            }
        }

        float lastDeltaYaw = data.getLastDeltaYaw();
        float lastDeltaPitch = data.getLastDeltaPitch();

        if (deltaYaw > 2.5f && Math.abs(deltaYaw - lastDeltaYaw) < 0.001f && Math.abs(deltaPitch - lastDeltaPitch) < 0.001f) {
            int linearTicks = data.getLinearAimTicks() + 1;
            data.setLinearAimTicks(linearTicks);
            if (linearTicks >= 4) {
                fail(data, "B (LinearSmooth)", 1.5, "Constant angular step detected across " + linearTicks + " ticks (deltaYaw=" + String.format("%.3f", deltaYaw) + ")");
                data.setLinearAimTicks(2);
            }
        } else {
            int linearTicks = data.getLinearAimTicks();
            if (linearTicks > 0) {
                data.setLinearAimTicks(linearTicks - 1);
            }
        }
    }
}

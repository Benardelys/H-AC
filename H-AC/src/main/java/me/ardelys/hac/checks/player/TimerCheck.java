package me.ardelys.hac.checks.player;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.Check;
import me.ardelys.hac.checks.CheckInfo;
import me.ardelys.hac.checks.CheckType;
import me.ardelys.hac.data.PlayerData;

@CheckInfo(name = "Timer", type = CheckType.PLAYER, description = "Detects client game tick speed acceleration", defaultThreshold = 15.0)
public class TimerCheck extends Check {

    public TimerCheck(HAC plugin) {
        super(plugin);
    }

    public void check(PlayerData data) {
        if (data == null || data.isExemptMovement()) {
            return;
        }

        long packetDelta = data.getLastMovePacketDelta();

        if (packetDelta > 1500L || packetDelta <= 0L) {
            data.setTimerBalance(0.0);
            return;
        }

        double balance = data.getTimerBalance();
        balance += (50.0 - packetDelta);

        double maxDeficit = -Math.max(800.0, data.getPing() * 2.5);
        if (balance < maxDeficit) {
            balance = maxDeficit;
        }

        data.setTimerBalance(balance);

        double maxAllowedBalance = 240.0;
        if (balance > maxAllowedBalance) {
            fail(data, "A", 1.5, "Clock balance drift: +" + String.format("%.1f", balance) + "ms");
            data.setTimerBalance(60.0);
        }
    }
}

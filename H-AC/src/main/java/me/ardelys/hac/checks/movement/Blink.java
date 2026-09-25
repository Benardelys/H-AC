package me.ardelys.hac.checks.movement;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.Check;
import me.ardelys.hac.checks.CheckInfo;
import me.ardelys.hac.checks.CheckType;
import me.ardelys.hac.data.PlayerData;

@CheckInfo(name = "Blink", type = CheckType.MOVEMENT, description = "Detects packet withholding and sudden burst distance leaps", defaultThreshold = 15.0)
public class Blink extends Check {

    private long maxPulseDelayMs;

    public Blink(HAC plugin) {
        super(plugin);
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        this.maxPulseDelayMs = plugin.getConfigManager().getInt("checks.blink.max-pulse-delay-ms", 1200);
    }

    public void check(PlayerData data) {
        if (data == null || data.isExemptMovement()) {
            return;
        }

        long packetDelta = data.getLastMovePacketDelta();

        if (packetDelta > maxPulseDelayMs && data.getTeleportGraceTicks() > 25 && data.getRespawnGraceTicks() > 30) {
            double distance = data.getDeltaXZ();
            if (distance > 2.8) {
                fail(data, "A", 2.0, "Packet pause " + packetDelta + "ms followed by sudden leap of " + String.format("%.2f", distance) + " blocks");
            }
        }
    }
}

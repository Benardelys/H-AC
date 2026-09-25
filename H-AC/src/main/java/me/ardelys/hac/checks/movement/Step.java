package me.ardelys.hac.checks.movement;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.Check;
import me.ardelys.hac.checks.CheckInfo;
import me.ardelys.hac.checks.CheckType;
import me.ardelys.hac.data.PlayerData;
import me.ardelys.hac.utils.PlayerUtil;
import org.bukkit.entity.Player;

@CheckInfo(name = "Step", type = CheckType.MOVEMENT, description = "Detects instant step-up height exceeding vanilla limits", defaultThreshold = 12.0)
public class Step extends Check {

    private double maxStepHeight;

    public Step(HAC plugin) {
        super(plugin);
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        this.maxStepHeight = plugin.getConfigManager().getDouble("checks.step.max-step-height", 0.62);
    }

    public void check(PlayerData data) {
        if (data == null || data.isExemptMovement()) {
            return;
        }

        Player player = data.getPlayer();
        if (player == null || !player.isOnline()) return;

        if (data.getSlimeTicks() > 0 || data.getClimbableTicks() > 0 || data.getVelocityTicks() < 10) {
            return;
        }

        if (data.getAirTicks() > 0) {
            return;
        }

        double deltaY = data.getDeltaY();
        if (deltaY <= 0.0) {
            return;
        }

        double attributeStep = PlayerUtil.getStepHeight(player);
        double allowedStep = Math.max(maxStepHeight, attributeStep + 0.05);

        if (deltaY > allowedStep && deltaY < 4.0 && data.isOnGround() && data.isLastOnGround()) {
            fail(data, "A", 1.5, "Step height " + String.format("%.2f", deltaY) + " exceeded limit (" + String.format("%.2f", allowedStep) + ")");
        }
    }
}

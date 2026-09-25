package me.ardelys.hac.checks.player;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.Check;
import me.ardelys.hac.checks.CheckInfo;
import me.ardelys.hac.checks.CheckType;
import me.ardelys.hac.data.PlayerData;
import org.bukkit.entity.Player;

@CheckInfo(name = "ImpossibleActions", type = CheckType.PLAYER, description = "Detects impossible simultaneous player state combinations", defaultThreshold = 8.0)
public class ImpossibleActions extends Check {

    public ImpossibleActions(HAC plugin) {
        super(plugin);
    }

    public void check(PlayerData data) {
        if (data == null) return;
        Player player = data.getPlayer();
        if (player == null || !player.isOnline()) return;

        if (data.isExemptMovement()) {
            return;
        }

        boolean isCrawlingOrSwimming = player.isSwimming() || player.getPose() == org.bukkit.entity.Pose.SWIMMING;
        if (!isCrawlingOrSwimming && player.isSprinting() && player.isSneaking()) {
            int ticks = data.getConsecutiveSprintSneakTicks() + 1;
            data.setConsecutiveSprintSneakTicks(ticks);
            if (ticks >= 4) {
                fail(data, "A (SprintSneak)", 1.5, "Simultaneous sprinting and sneaking sustained for " + ticks + " ticks");
            }
        } else {
            data.setConsecutiveSprintSneakTicks(0);
        }

        if (player.isSprinting() && player.getFoodLevel() < 6) {
            int hungerTicks = data.getConsecutiveLowHungerSprintTicks() + 1;
            data.setConsecutiveLowHungerSprintTicks(hungerTicks);
            if (hungerTicks > 25) {
                fail(data, "B (SprintLowHunger)", 1.0, "Sprinting sustained for " + hungerTicks + " ticks with hunger level: " + player.getFoodLevel());
            }
        } else {
            data.setConsecutiveLowHungerSprintTicks(0);
        }

        if (player.isSleeping() && (player.isSprinting() || data.getDeltaXZ() > 0.05)) {
            fail(data, "C (SleepSprint)", 2.0, "Moving or sprinting while sleeping");
        }
    }
}

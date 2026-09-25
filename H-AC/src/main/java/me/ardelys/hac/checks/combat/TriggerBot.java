package me.ardelys.hac.checks.combat;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.Check;
import me.ardelys.hac.checks.CheckInfo;
import me.ardelys.hac.checks.CheckType;
import me.ardelys.hac.data.PlayerData;
import me.ardelys.hac.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.UUID;

@CheckInfo(name = "TriggerBot", type = CheckType.COMBAT, description = "Detects automated attack reaction upon target crosshair intersection", defaultThreshold = 12.0)
public class TriggerBot extends Check {

    private long minReactionMs;

    public TriggerBot(HAC plugin) {
        super(plugin);
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        this.minReactionMs = plugin.getConfigManager().getInt("checks.triggerbot.min-reaction-ms", 40);
    }

    public void checkRotation(PlayerData data) {
        if (data == null) return;
        Player player = data.getPlayer();
        if (player == null || !player.isOnline()) return;

        UUID targetUuid = data.getLastTargetUuid();
        if (targetUuid == null) return;

        long now = TimeUtil.now();
        if (now - data.getLastAttackTime() > 3000L) return;

        Entity target = Bukkit.getEntity(targetUuid);
        if (target == null || target.isDead() || !target.isValid()) return;
        if (target.getWorld() == null || !target.getWorld().equals(player.getWorld())) return;

        Location eyeLoc = player.getEyeLocation();
        BoundingBox box = target.getBoundingBox();
        Vector eye = eyeLoc.toVector();
        Vector dir = eyeLoc.getDirection();

        if (dir.lengthSquared() < 1.0E-6) return;

        RayTraceResult hit = box.rayTrace(eye, dir, 5.0);
        if (hit != null) {
            if (data.getLastTargetCrosshairTime() == 0) {
                data.setLastTargetCrosshairTime(now);
            }
        } else {
            data.setLastTargetCrosshairTime(0);
        }
    }

    public void checkAttack(PlayerData data, Entity target) {
        if (data == null || target == null) return;
        Player player = data.getPlayer();
        if (player == null || !player.isOnline()) return;

        long now = TimeUtil.now();
        long crosshairTime = data.getLastTargetCrosshairTime();

        if (crosshairTime > 0) {
            long reaction = now - crosshairTime;
            long lastSwing = data.getLastSwingTime();

            if (reaction >= 0 && reaction < minReactionMs && lastSwing <= crosshairTime) {
                int buffer = data.getTriggerBotBuffer() + 1;
                data.setTriggerBotBuffer(buffer);

                if (buffer >= 3) {
                    fail(data, "A (InstantReaction)", 1.5, "Inhuman attack reaction time: " + reaction + "ms (min: " + minReactionMs + "ms, buf: " + buffer + ")");
                }
            } else {
                int buffer = data.getTriggerBotBuffer();
                if (buffer > 0) {
                    data.setTriggerBotBuffer(buffer - 1);
                }
            }
        }

        float deltaYaw = data.getDeltaYaw();
        float deltaPitch = data.getDeltaPitch();
        long moveDelta = now - data.getLastMovePacketTime();

        if (moveDelta < 15L && (deltaYaw > 6.0f || deltaPitch > 6.0f)) {
            Location eyeLoc = player.getEyeLocation();
            RayTraceResult hit = target.getBoundingBox().rayTrace(eyeLoc.toVector(), eyeLoc.getDirection(), 4.5);
            if (hit != null) {
                int buffer = data.getTriggerBotBuffer() + 1;
                data.setTriggerBotBuffer(buffer);
                if (buffer >= 4) {
                    fail(data, "B (SnapAttack)", 1.0, "Attacked immediately after high angular step: " + String.format("%.2f", deltaYaw) + " deg in " + moveDelta + "ms");
                }
            }
        }
    }
}

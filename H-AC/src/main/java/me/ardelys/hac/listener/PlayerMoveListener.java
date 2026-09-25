package me.ardelys.hac.listener;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.CheckManager;
import me.ardelys.hac.checks.combat.AimAssist;
import me.ardelys.hac.checks.combat.KillAura;
import me.ardelys.hac.checks.combat.VelocityCheck;
import me.ardelys.hac.checks.movement.*;
import me.ardelys.hac.checks.player.BadPackets;
import me.ardelys.hac.checks.player.ImpossibleActions;
import me.ardelys.hac.checks.player.TimerCheck;
import me.ardelys.hac.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerMoveListener implements Listener {

    private final HAC plugin;
    private final BadPackets badPackets;
    private final TimerCheck timer;
    private final ImpossibleActions actions;
    private final Speed speed;
    private final Fly fly;
    private final Glide glide;
    private final Jesus jesus;
    private final Step step;
    private final HighJump highJump;
    private final LongJump longJump;
    private final NoFall noFall;
    private final NoSlow noSlow;
    private final InventoryMove invMove;
    private final Phase phase;
    private final Blink blink;
    private final VelocityCheck velocity;
    private final AimAssist aimAssist;
    private final KillAura aura;
    private final me.ardelys.hac.checks.combat.TriggerBot triggerBot;

    public PlayerMoveListener(HAC plugin) {
        this.plugin = plugin;
        CheckManager cm = plugin.getCheckManager();
        this.badPackets = cm.getCheck(BadPackets.class);
        this.timer = cm.getCheck(TimerCheck.class);
        this.actions = cm.getCheck(ImpossibleActions.class);
        this.speed = cm.getCheck(Speed.class);
        this.fly = cm.getCheck(Fly.class);
        this.glide = cm.getCheck(Glide.class);
        this.jesus = cm.getCheck(Jesus.class);
        this.step = cm.getCheck(Step.class);
        this.highJump = cm.getCheck(HighJump.class);
        this.longJump = cm.getCheck(LongJump.class);
        this.noFall = cm.getCheck(NoFall.class);
        this.noSlow = cm.getCheck(NoSlow.class);
        this.invMove = cm.getCheck(InventoryMove.class);
        this.phase = cm.getCheck(Phase.class);
        this.blink = cm.getCheck(Blink.class);
        this.velocity = cm.getCheck(VelocityCheck.class);
        this.aimAssist = cm.getCheck(AimAssist.class);
        this.aura = cm.getCheck(KillAura.class);
        this.triggerBot = cm.getCheck(me.ardelys.hac.checks.combat.TriggerBot.class);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null) return;

        boolean positionChanged = from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ();
        boolean rotationChanged = from.getYaw() != to.getYaw() || from.getPitch() != to.getPitch();

        if (!positionChanged && !rotationChanged) {
            return;
        }

        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player);
        if (data == null) return;

        data.processMove(from, to, positionChanged, rotationChanged);

        if (badPackets != null) badPackets.check(data);
        if (timer != null) timer.check(data);
        if (actions != null) actions.check(data);

        if (positionChanged) {
            if (speed != null) speed.check(data);
            if (fly != null) fly.check(data);
            if (glide != null) glide.check(data);
            if (jesus != null) jesus.check(data);
            if (step != null) step.check(data);
            if (highJump != null) highJump.check(data);
            if (longJump != null) longJump.check(data);
            if (noFall != null) noFall.check(data);
            if (noSlow != null) noSlow.check(data);
            if (invMove != null) invMove.check(data);
            if (phase != null) phase.check(data);
            if (blink != null) blink.check(data);
            if (velocity != null) velocity.check(data);
        }

        if (rotationChanged) {
            if (aimAssist != null) aimAssist.check(data);
            if (aura != null) aura.checkRotation(data);
            if (triggerBot != null) triggerBot.checkRotation(data);
        }
    }
}

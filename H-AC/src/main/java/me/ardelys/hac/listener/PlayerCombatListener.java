package me.ardelys.hac.listener;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.CheckManager;
import me.ardelys.hac.checks.combat.Criticals;
import me.ardelys.hac.checks.combat.FastBow;
import me.ardelys.hac.checks.combat.KillAura;
import me.ardelys.hac.checks.combat.Reach;
import me.ardelys.hac.data.PlayerData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerVelocityEvent;

public class PlayerCombatListener implements Listener {

    private final HAC plugin;
    private final Reach reach;
    private final KillAura aura;
    private final Criticals criticals;
    private final FastBow fastBow;
    private final me.ardelys.hac.checks.combat.TriggerBot triggerBot;

    public PlayerCombatListener(HAC plugin) {
        this.plugin = plugin;
        CheckManager cm = plugin.getCheckManager();
        this.reach = cm.getCheck(Reach.class);
        this.aura = cm.getCheck(KillAura.class);
        this.criticals = cm.getCheck(Criticals.class);
        this.fastBow = cm.getCheck(FastBow.class);
        this.triggerBot = cm.getCheck(me.ardelys.hac.checks.combat.TriggerBot.class);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            return;
        }

        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().get(attacker);
        if (data == null) return;

        Entity target = event.getEntity();

        if (reach != null) reach.check(data, target);
        if (aura != null) aura.checkAttack(data, target);
        if (triggerBot != null) triggerBot.checkAttack(data, target);
        if (criticals != null) criticals.check(data);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVelocity(PlayerVelocityEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player);
        if (data != null) {
            data.setExpectedVelocity(event.getVelocity());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().get(player);
        if (data == null) return;

        if (fastBow != null) {
            fastBow.check(data, event.getForce());
        }
    }
}

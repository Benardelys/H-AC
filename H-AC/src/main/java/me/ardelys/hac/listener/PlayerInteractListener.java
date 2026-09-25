package me.ardelys.hac.listener;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.CheckManager;
import me.ardelys.hac.checks.combat.AutoClicker;
import me.ardelys.hac.checks.player.FastEat;
import me.ardelys.hac.checks.player.FastProjectile;
import me.ardelys.hac.checks.world.InvalidBlockInteraction;
import me.ardelys.hac.data.PlayerData;
import me.ardelys.hac.utils.PlayerUtil;
import me.ardelys.hac.utils.TimeUtil;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerInteractListener implements Listener {

    private final HAC plugin;
    private final AutoClicker clicker;
    private final FastEat fastEat;
    private final FastProjectile projectile;
    private final InvalidBlockInteraction blockInteraction;

    public PlayerInteractListener(HAC plugin) {
        this.plugin = plugin;
        CheckManager cm = plugin.getCheckManager();
        this.clicker = cm.getCheck(AutoClicker.class);
        this.fastEat = cm.getCheck(FastEat.class);
        this.projectile = cm.getCheck(FastProjectile.class);
        this.blockInteraction = cm.getCheck(InvalidBlockInteraction.class);
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) {
            return;
        }

        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player);
        if (data == null) return;

        data.addClickSample();

        if (clicker != null) {
            clicker.check(data);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onStartUse(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item != null) {
                PlayerData data = plugin.getPlayerDataManager().get(event.getPlayer());
                if (data != null) {
                    if (PlayerUtil.isMovementSlowingItem(item) && data.getConsumeStartTime() == 0) {
                        data.setConsumeStartTime(TimeUtil.now());
                    }
                    if (item.getType() == org.bukkit.Material.BOW || item.getType() == org.bukkit.Material.CROSSBOW) {
                        data.setBowDrawStartTime(TimeUtil.now());
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player);
        if (data == null) return;

        if (fastEat != null) {
            fastEat.check(data, event.getItem());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectile(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) {
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().get(player);
        if (data == null) return;

        if (projectile != null) {
            projectile.check(data);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player);
        if (data == null) return;

        if (blockInteraction != null) {
            blockInteraction.check(data, clicked);
        }
    }
}

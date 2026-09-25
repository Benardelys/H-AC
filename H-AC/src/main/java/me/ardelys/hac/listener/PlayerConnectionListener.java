package me.ardelys.hac.listener;

import me.ardelys.hac.HAC;
import me.ardelys.hac.data.PlayerData;
import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

import java.util.UUID;

public class PlayerConnectionListener implements Listener {

    private final HAC plugin;

    public PlayerConnectionListener(HAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player);
        if (data != null) {
            data.markJoin();
            data.markTeleport();
            plugin.getViolationManager().restorePlayerViolations(data);
            try {
                String brand = player.getClientBrandName();
                if (brand != null && !brand.isEmpty()) {
                    data.setClientBrand(brand);
                    if (plugin.getClientDetector() != null) {
                        plugin.getClientDetector().onBrandReceived(player, brand);
                    }
                }
            } catch (Throwable ignored) {
            }
            if (plugin.getInjectorDetector() != null) {
                plugin.getInjectorDetector().handlePlayerJoin(player, data);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        PlayerData data = plugin.getPlayerDataManager().getIfPresent(uuid);
        if (data != null) {
            if (plugin.getInjectorDetector() != null) {
                plugin.getInjectorDetector().handlePlayerQuit(event.getPlayer(), data);
            }
            plugin.getViolationManager().preservePlayerViolations(data);
        }
        plugin.getPunishmentManager().remove(uuid);
        plugin.getPlayerDataManager().remove(uuid);
        plugin.getCheckManager().cleanupPlayer(uuid);
        plugin.getAlertManager().cleanupPlayer(uuid);
        if (plugin.getDiscordWebhookManager() != null) {
            plugin.getDiscordWebhookManager().cleanupPlayer(uuid);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        PlayerData data = plugin.getPlayerDataManager().getIfPresent(event.getEntity().getUniqueId());
        if (data != null) {
            data.markDeath();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        PlayerData data = plugin.getPlayerDataManager().getIfPresent(event.getPlayer().getUniqueId());
        if (data != null) {
            data.markGameModeChange();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        PlayerData data = plugin.getPlayerDataManager().getIfPresent(event.getPlayer().getUniqueId());
        if (data != null) {
            data.markTeleport();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        PlayerData data = plugin.getPlayerDataManager().getIfPresent(event.getPlayer().getUniqueId());
        if (data != null) {
            data.markRespawn();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        PlayerData data = plugin.getPlayerDataManager().getIfPresent(event.getPlayer().getUniqueId());
        if (data != null) {
            data.markTeleport();
            data.cleanReferences();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (event.getEntered() instanceof Player player) {
            PlayerData data = plugin.getPlayerDataManager().getIfPresent(player.getUniqueId());
            if (data != null) {
                data.markVehicle();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleExit(VehicleExitEvent event) {
        if (event.getExited() instanceof Player player) {
            PlayerData data = plugin.getPlayerDataManager().getIfPresent(player.getUniqueId());
            if (data != null) {
                data.markVehicle();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityMount(EntityMountEvent event) {
        if (event.getEntity() instanceof Player player) {
            PlayerData data = plugin.getPlayerDataManager().getIfPresent(player.getUniqueId());
            if (data != null) {
                data.markVehicle();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player player) {
            PlayerData data = plugin.getPlayerDataManager().getIfPresent(player.getUniqueId());
            if (data != null) {
                data.markVehicle();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRiptide(PlayerRiptideEvent event) {
        PlayerData data = plugin.getPlayerDataManager().getIfPresent(event.getPlayer().getUniqueId());
        if (data != null) {
            data.markRiptide();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onElytraBoost(PlayerElytraBoostEvent event) {
        PlayerData data = plugin.getPlayerDataManager().getIfPresent(event.getPlayer().getUniqueId());
        if (data != null) {
            data.markElytraBoost();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onToggleGlide(EntityToggleGlideEvent event) {
        if (event.getEntity() instanceof Player player) {
            PlayerData data = plugin.getPlayerDataManager().getIfPresent(player.getUniqueId());
            if (data != null) {
                data.markElytraBoost();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            PlayerData data = plugin.getPlayerDataManager().getIfPresent(player.getUniqueId());
            if (data != null) {
                data.markInventoryOpen();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            PlayerData data = plugin.getPlayerDataManager().getIfPresent(player.getUniqueId());
            if (data != null) {
                data.markInventoryClose();
            }
        }
    }
}

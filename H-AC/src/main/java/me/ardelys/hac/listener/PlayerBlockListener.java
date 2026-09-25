package me.ardelys.hac.listener;

import me.ardelys.hac.HAC;
import me.ardelys.hac.checks.CheckManager;
import me.ardelys.hac.checks.world.FastBreak;
import me.ardelys.hac.checks.world.FastPlace;
import me.ardelys.hac.checks.world.Scaffold;
import me.ardelys.hac.data.PlayerData;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class PlayerBlockListener implements Listener {

    private final HAC plugin;
    private final FastPlace fastPlace;
    private final Scaffold scaffold;
    private final FastBreak fastBreak;

    public PlayerBlockListener(HAC plugin) {
        this.plugin = plugin;
        CheckManager cm = plugin.getCheckManager();
        this.fastPlace = cm.getCheck(FastPlace.class);
        this.scaffold = cm.getCheck(Scaffold.class);
        this.fastBreak = cm.getCheck(FastBreak.class);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player);
        if (data == null) return;

        Block placed = event.getBlockPlaced();
        Block against = event.getBlockAgainst();
        BlockFace face = against.getFace(placed);

        if (fastPlace != null) {
            fastPlace.check(data);
        }

        if (scaffold != null) {
            scaffold.check(data, placed, face != null ? face : BlockFace.SELF);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player);
        if (data == null) return;

        if (fastBreak != null) {
            fastBreak.check(data, event.getBlock());
        }
    }
}

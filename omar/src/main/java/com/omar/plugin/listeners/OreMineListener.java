package com.omar.plugin.listeners;

import com.omar.plugin.OmarPlugin;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Set;

public class OreMineListener implements Listener {

    private final OmarPlugin plugin;

    private static final Set<Material> TARGET_ORES = Set.of(
            Material.DIAMOND_ORE,
            Material.DEEPSLATE_DIAMOND_ORE,
            Material.ANCIENT_DEBRIS
    );

    public OreMineListener(OmarPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material blockType = block.getType();

        if (!TARGET_ORES.contains(blockType)) {
            return;
        }

        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE ||
            player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            return;
        }

        plugin.getReportManager().onPlayerMineOre(player, blockType, block.getLocation());
    }
}

package com.omar.plugin.gui;

import com.omar.plugin.managers.ConfigManager;
import com.omar.plugin.managers.ReportManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MainPanelGUI implements InventoryHolder {

    private static final int GUI_SIZE = 54;
    private static final int SLOT_LOG = 19;
    private static final int SLOT_IMLOG = 20;
    private static final int SLOT_BYPASS = 21;
    private static final int SLOT_CHECK = 22;
    private static final int SLOT_STATS = 23;
    private static final int SLOT_RELOAD = 24;
    private static final int SLOT_HELP = 25;

    private final Inventory inventory;
    private final ConfigManager configManager;
    private final ReportManager reportManager;

    public MainPanelGUI(ConfigManager configManager, ReportManager reportManager) {
        this.configManager = configManager;
        this.reportManager = reportManager;

        String title = ConfigManager.translateColorCodes(
                configManager.getRawMessageWithoutPrefix("gui-panel-title"));
        this.inventory = Bukkit.createInventory(this, GUI_SIZE, Component.text(title));
        initializeItems();
    }

    private void initializeItems() {
        ItemStack filler = createFiller();
        for (int i = 0; i < GUI_SIZE; i++) {
            inventory.setItem(i, filler.clone());
        }

        inventory.setItem(SLOT_LOG, createItem(Material.DIAMOND_ORE, "gui-panel-log", "gui-panel-log-lore"));
        inventory.setItem(SLOT_IMLOG, createItem(Material.WITHER_SKELETON_SKULL, "gui-panel-imlog", "gui-panel-imlog-lore"));
        inventory.setItem(SLOT_BYPASS, createItem(Material.SHIELD, "gui-panel-bypass", "gui-panel-bypass-lore"));
        inventory.setItem(SLOT_CHECK, createItem(Material.COMPASS, "gui-panel-check", "gui-panel-check-lore"));
        inventory.setItem(SLOT_STATS, createItem(Material.PAPER, "gui-panel-stats", "gui-panel-stats-lore"));
        inventory.setItem(SLOT_RELOAD, createItem(Material.CLOCK, "gui-panel-reload", "gui-panel-reload-lore"));
        inventory.setItem(SLOT_HELP, createItem(Material.BOOK, "gui-panel-help", "gui-panel-help-lore"));
    }

    private ItemStack createItem(Material material, String nameKey, String loreKey) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(configManager.colorize(
                    configManager.getRawMessageWithoutPrefix(nameKey)));
            List<String> rawLore = configManager.getRawMessageList(loreKey);
            List<Component> lore = new ArrayList<>();
            for (String line : rawLore) lore.add(configManager.colorize(line));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createFiller() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.displayName(Component.text("")); item.setItemMeta(meta); }
        return item;
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        switch (slot) {
            case SLOT_LOG -> player.performCommand("omar log");
            case SLOT_IMLOG -> player.performCommand("omar imlog");
            case SLOT_BYPASS -> player.performCommand("omar bypass list");
            case SLOT_CHECK -> {
                player.closeInventory();
                player.sendMessage(configManager.colorize(
                        "&#f9e2af请在聊天栏输入: &#a6e3a1/omar check <玩家名>"));
            }
            case SLOT_STATS -> player.performCommand("omar stats");
            case SLOT_RELOAD -> player.performCommand("omar reload");
            case SLOT_HELP -> player.performCommand("omar help");
        }
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public static void open(Player player, ConfigManager configManager, ReportManager reportManager) {
        MainPanelGUI gui = new MainPanelGUI(configManager, reportManager);
        player.openInventory(gui.getInventory());
    }
}

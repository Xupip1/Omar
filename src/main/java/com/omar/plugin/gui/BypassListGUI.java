package com.omar.plugin.gui;

import com.omar.plugin.managers.ConfigManager;
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

public class BypassListGUI implements InventoryHolder {

    private static final int GUI_SIZE = 54;
    private static final int ITEMS_PER_PAGE = 45;
    private static final int SLOT_ADD = 53;
    private static final int SLOT_PREVIOUS = 48;
    private static final int SLOT_PAGE_INFO = 49;
    private static final int SLOT_NEXT = 50;

    private final Inventory inventory;
    private final ConfigManager configManager;
    private final List<String> bypassPlayers;
    private final int page;
    private final int totalPages;

    public BypassListGUI(ConfigManager configManager, List<String> bypassPlayers, int page) {
        this.configManager = configManager;
        this.bypassPlayers = bypassPlayers;
        this.page = page;
        this.totalPages = Math.max(1, (int) Math.ceil((double) bypassPlayers.size() / ITEMS_PER_PAGE));

        String title = ConfigManager.translateColorCodes(
                configManager.getRawMessageWithoutPrefix("gui-bypass-title"));
        this.inventory = Bukkit.createInventory(this, GUI_SIZE, Component.text(title));
        initializeItems();
    }

    private void initializeItems() {
        ItemStack filler = createFiller();
        for (int i = 0; i < GUI_SIZE; i++) {
            inventory.setItem(i, filler.clone());
        }

        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, bypassPlayers.size());

        for (int i = startIndex; i < endIndex; i++) {
            String playerName = bypassPlayers.get(i);
            int slot = i - startIndex;
            inventory.setItem(slot, createPlayerItem(playerName));
        }

        if (bypassPlayers.isEmpty()) {
            ItemStack empty = new ItemStack(Material.BARRIER, 1);
            ItemMeta meta = empty.getItemMeta();
            if (meta != null) {
                meta.displayName(configManager.colorize(
                        configManager.getRawMessageWithoutPrefix("gui-bypass-empty")));
                empty.setItemMeta(meta);
            }
            inventory.setItem(22, empty);
        }

        inventory.setItem(SLOT_ADD, createAddItem());

        if (page > 0) {
            inventory.setItem(SLOT_PREVIOUS, createNavItem("gui-bypass-previous-page"));
        }
        if (page < totalPages - 1) {
            inventory.setItem(SLOT_NEXT, createNavItem("gui-bypass-next-page"));
        }

        if (!bypassPlayers.isEmpty()) {
            ItemStack info = new ItemStack(Material.PAPER, Math.max(1, page + 1));
            ItemMeta meta = info.getItemMeta();
            if (meta != null) {
                String pageInfo = configManager.getRawMessageWithoutPrefix("gui-bypass-page-info")
                        .replace("%page%", String.valueOf(page + 1))
                        .replace("%total%", String.valueOf(totalPages));
                meta.displayName(configManager.colorize(pageInfo));
                info.setItemMeta(meta);
            }
            inventory.setItem(SLOT_PAGE_INFO, info);
        }
    }

    private ItemStack createPlayerItem(String playerName) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(configManager.colorize("&#f9e2af" + playerName));
            List<Component> lore = new ArrayList<>();
            lore.add(configManager.colorize(
                    configManager.getRawMessageWithoutPrefix("gui-bypass-remove-lore")));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createAddItem() {
        ItemStack item = new ItemStack(Material.LIME_WOOL, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(configManager.colorize(
                    configManager.getRawMessageWithoutPrefix("gui-bypass-add")));
            List<String> rawLore = configManager.getRawMessageList("gui-bypass-add-lore");
            List<Component> lore = new ArrayList<>();
            for (String line : rawLore) lore.add(configManager.colorize(line));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createNavItem(String key) {
        ItemStack item = new ItemStack(Material.ARROW, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(configManager.colorize(
                    configManager.getRawMessageWithoutPrefix(key)));
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

        if (slot == SLOT_ADD) {
            player.sendMessage(configManager.colorize(
                    "&#f9e2af请在聊天栏输入: &#a6e3a1/omar bypass add <玩家名>"));
            player.closeInventory();
            return;
        }

        if (slot >= 0 && slot < ITEMS_PER_PAGE) {
            int index = page * ITEMS_PER_PAGE + slot;
            if (index < bypassPlayers.size()) {
                String playerName = bypassPlayers.get(index);
                player.performCommand("omar bypass remove " + playerName);
                player.closeInventory();
                return;
            }
        }

        if (slot == SLOT_PREVIOUS && page > 0) {
            open(player, configManager, bypassPlayers, page - 1);
        } else if (slot == SLOT_NEXT && page < totalPages - 1) {
            open(player, configManager, bypassPlayers, page + 1);
        }
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public static void open(Player player, ConfigManager configManager, List<String> bypassPlayers) {
        open(player, configManager, bypassPlayers, 0);
    }

    public static void open(Player player, ConfigManager configManager, List<String> bypassPlayers, int page) {
        BypassListGUI gui = new BypassListGUI(configManager, bypassPlayers, page);
        player.openInventory(gui.getInventory());
    }
}

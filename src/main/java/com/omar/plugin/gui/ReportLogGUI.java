package com.omar.plugin.gui;

import com.omar.plugin.managers.ConfigManager;
import com.omar.plugin.managers.ReportManager;
import com.omar.plugin.models.OreReport;
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

public class ReportLogGUI implements InventoryHolder {

    private static final int GUI_SIZE = 54;
    private static final int REPORTS_PER_PAGE = 45;
    private static final int SLOT_PREVIOUS = 48;
    private static final int SLOT_PAGE_INFO = 49;
    private static final int SLOT_NEXT = 50;

    private final Inventory inventory;
    private final ConfigManager configManager;
    private final List<OreReport> allReports;
    private final int page;
    private final int totalPages;

    public ReportLogGUI(ConfigManager configManager, List<OreReport> allReports, int page) {
        this.configManager = configManager;
        this.allReports = allReports;
        this.page = page;
        this.totalPages = Math.max(1, (int) Math.ceil((double) allReports.size() / REPORTS_PER_PAGE));

        String title = configManager.getGuiTitle();
        this.inventory = Bukkit.createInventory(this, GUI_SIZE, Component.text(
                ConfigManager.translateColorCodes(title)
        ));

        initializeItems();
    }

    private void initializeItems() {
        ItemStack filler = createFillerItem();
        for (int i = 0; i < GUI_SIZE; i++) {
            inventory.setItem(i, filler.clone());
        }

        int startIndex = page * REPORTS_PER_PAGE;
        int endIndex = Math.min(startIndex + REPORTS_PER_PAGE, allReports.size());

        for (int i = startIndex; i < endIndex; i++) {
            OreReport report = allReports.get(i);
            int slot = i - startIndex;
            inventory.setItem(slot, createReportItem(report));
        }

        if (allReports.isEmpty()) {
            ItemStack noReportItem = createNoReportItem();
            inventory.setItem(22, noReportItem);
        }

        if (page > 0) {
            inventory.setItem(SLOT_PREVIOUS, createPreviousPageItem());
        }
        if (page < totalPages - 1) {
            inventory.setItem(SLOT_NEXT, createNextPageItem());
        }

        inventory.setItem(SLOT_PAGE_INFO, createPageInfoItem());
    }

    private ItemStack createReportItem(OreReport report) {
        Material iconMaterial = report.getOreType();
        ItemStack item = new ItemStack(iconMaterial, 1);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(configManager.colorize(
                    "&6" + report.getOreTypeName() + " &8#&7" + report.getId()
            ));

            List<String> rawLore = configManager.getRawMessageList("gui-report-lore");
            List<Component> loreComponents = new ArrayList<>();

            for (String line : rawLore) {
                String formattedLine = line
                        .replace("%player%", report.getPlayerName())
                        .replace("%ore%", report.getOreTypeName())
                        .replace("%count%", String.valueOf(report.getCount()))
                        .replace("%time%", report.getFormattedTime())
                        .replace("%x%", String.format("%.0f", report.getX()))
                        .replace("%y%", String.format("%.0f", report.getY()))
                        .replace("%z%", String.format("%.0f", report.getZ()));
                loreComponents.add(configManager.colorize(formattedLine));
            }

            meta.lore(loreComponents);
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createNoReportItem() {
        ItemStack item = new ItemStack(Material.BARRIER, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(configManager.colorize(
                    configManager.getRawMessageWithoutPrefix("gui-no-reports")
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPreviousPageItem() {
        ItemStack item = new ItemStack(Material.ARROW, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(configManager.colorize(
                    configManager.getRawMessageWithoutPrefix("gui-previous-page")
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createNextPageItem() {
        ItemStack item = new ItemStack(Material.ARROW, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(configManager.colorize(
                    configManager.getRawMessageWithoutPrefix("gui-next-page")
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPageInfoItem() {
        ItemStack item = new ItemStack(Material.PAPER, Math.max(1, page + 1));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String pageInfo = configManager.getRawMessageWithoutPrefix("gui-page-info")
                    .replace("%page%", String.valueOf(page + 1))
                    .replace("%total%", String.valueOf(totalPages));
            meta.displayName(configManager.colorize(pageInfo));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createFillerItem() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(""));
            item.setItemMeta(meta);
        }
        return item;
    }

    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        int slot = event.getRawSlot();
        event.setCancelled(true);

        if (slot >= 0 && slot < REPORTS_PER_PAGE) {
            int reportIndex = page * REPORTS_PER_PAGE + slot;
            if (reportIndex < allReports.size()) {
                OreReport report = allReports.get(reportIndex);
                teleportToReport(player, report);
            }
            return;
        }

        if (slot == SLOT_PREVIOUS && page > 0) {
            openNewPage(player, page - 1);
        } else if (slot == SLOT_NEXT && page < totalPages - 1) {
            openNewPage(player, page + 1);
        }
    }

    private void teleportToReport(Player player, OreReport report) {
        org.bukkit.World world = Bukkit.getWorld(report.getWorldName());
        if (world == null) {
            player.sendMessage(configManager.getMessage("teleport-failed"));
            return;
        }

        org.bukkit.Location location = report.getLocation(world);
        player.teleport(location);

        player.sendMessage(configManager.getFormattedMessage("teleported",
                "player", report.getPlayerName(),
                "x", String.format("%.0f", report.getX()),
                "y", String.format("%.0f", report.getY()),
                "z", String.format("%.0f", report.getZ())
        ));

        player.closeInventory();
    }

    private void openNewPage(Player player, int newPage) {
        ReportLogGUI newGui = new ReportLogGUI(configManager, allReports, newPage);
        player.openInventory(newGui.getInventory());
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public static void open(Player player, ConfigManager configManager,
                            ReportManager reportManager) {
        List<OreReport> reports = reportManager.getReports();
        ReportLogGUI gui = new ReportLogGUI(configManager, reports, 0);
        player.openInventory(gui.getInventory());
    }
}

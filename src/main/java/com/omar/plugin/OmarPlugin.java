package com.omar.plugin;

import com.omar.plugin.commands.OmarCommand;
import com.omar.plugin.gui.BypassListGUI;
import com.omar.plugin.gui.MainPanelGUI;
import com.omar.plugin.gui.PriorityReportLogGUI;
import com.omar.plugin.gui.ReportLogGUI;
import com.omar.plugin.listeners.OreMineListener;
import com.omar.plugin.managers.ConfigManager;
import com.omar.plugin.managers.ReportManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class OmarPlugin extends JavaPlugin implements Listener {

    private ConfigManager configManager;
    private ReportManager reportManager;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.reportManager = new ReportManager(this, configManager);

        OmarCommand omarCommand = new OmarCommand(this);
        getCommand("omar").setExecutor(omarCommand);
        getCommand("omar").setTabCompleter(omarCommand);

        getServer().getPluginManager().registerEvents(new OreMineListener(this), this);
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("Omar 反矿透插件已启用！");
        getLogger().info("当前配置 - 时间窗口: " + configManager.getTimeWindow() + "秒, 阈值: " + configManager.getOreThreshold() + "个");
    }

    @Override
    public void onDisable() {
        if (reportManager != null) {
            reportManager.shutdown();
        }
        getLogger().info("Omar 反矿透插件已禁用！");
    }

    public void reloadPluginConfig() {
        configManager.reload();
        reportManager.loadReports();
        reportManager.loadPriorityReports();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof ReportLogGUI gui) {
            gui.handleClick(event);
        } else if (event.getInventory().getHolder() instanceof PriorityReportLogGUI pgui) {
            pgui.handleClick(event);
        } else if (event.getInventory().getHolder() instanceof MainPanelGUI mpGUI) {
            mpGUI.handleClick(event);
        } else if (event.getInventory().getHolder() instanceof BypassListGUI bpGUI) {
            bpGUI.handleClick(event);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        reportManager.clearPlayerRecords(player);
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ReportManager getReportManager() {
        return reportManager;
    }
}

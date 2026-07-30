package com.omar.plugin.commands;

import com.omar.plugin.OmarPlugin;
import com.omar.plugin.gui.BypassListGUI;
import com.omar.plugin.gui.MainPanelGUI;
import com.omar.plugin.gui.PriorityReportLogGUI;
import com.omar.plugin.gui.ReportLogGUI;
import com.omar.plugin.managers.ConfigManager;
import com.omar.plugin.managers.ReportManager;
import com.omar.plugin.models.OreReport;
import com.omar.plugin.models.PriorityReport;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OmarCommand implements CommandExecutor, TabCompleter {

    private final OmarPlugin plugin;
    private final ConfigManager configManager;
    private final ReportManager reportManager;

    public OmarCommand(OmarPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.reportManager = plugin.getReportManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                if (sender.hasPermission("omar.command.panel")) {
                    MainPanelGUI.open(player, configManager, reportManager);
                } else {
                    sender.sendMessage(configManager.getMessage("no-permission"));
                }
                return true;
            }
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "add":     return handleAdd(sender, args);
            case "remove":  return handleRemove(sender, args);
            case "log":     return handleLog(sender);
            case "imlog":   return handleImLog(sender);
            case "check":   return handleCheck(sender, args);
            case "bypass":  return handleBypass(sender, args);
            case "stats":   return handleStats(sender);
            case "panel":   return handlePanel(sender);
            case "delete":  return handleDelete(sender);
            case "reload":  return handleReload(sender);
            case "help":    sendHelp(sender); return true;
            case "tp":      return handleTeleport(sender, args);
            case "prioritytp": return handlePriorityTeleport(sender, args);
            default:
                sender.sendMessage(configManager.getMessage("invalid-args"));
                return true;
        }
    }

    private boolean handleAdd(CommandSender sender, String[] args) {
        if (!sender.hasPermission("omar.command.add")) {
            sender.sendMessage(configManager.getMessage("no-permission")); return true;
        }
        if (args.length < 2) { sender.sendMessage(configManager.getMessage("invalid-args")); return true; }

        String targetName = args[1];
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) { sender.sendMessage(configManager.getMessage("player-not-found")); return true; }
        if (configManager.isAdminName(target.getName())) { sender.sendMessage(configManager.getMessage("already-admin")); return true; }

        configManager.addAdminName(target.getName());
        sender.sendMessage(configManager.getFormattedMessage("admin-added", "player", target.getName()));
        if (!sender.equals(target))
            target.sendMessage(configManager.getFormattedMessage("admin-added", "player", target.getName()));
        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("omar.command.remove")) {
            sender.sendMessage(configManager.getMessage("no-permission")); return true;
        }
        if (args.length < 2) { sender.sendMessage(configManager.getMessage("invalid-args")); return true; }

        String inputName = args[1];
        String targetName = configManager.getAdminNames().stream()
                .filter(name -> name.equalsIgnoreCase(inputName))
                .findFirst().orElse(null);
        if (targetName == null) {
            Player target = Bukkit.getPlayerExact(inputName);
            if (target != null) {
                targetName = configManager.getAdminNames().stream()
                        .filter(name -> name.equalsIgnoreCase(target.getName()))
                        .findFirst().orElse(null);
            }
            if (targetName == null) {
                sender.sendMessage(configManager.getMessage("not-admin"));
                return true;
            }
        }

        configManager.removeAdminName(targetName);
        sender.sendMessage(configManager.getFormattedMessage("admin-removed", "player", targetName));
        Player target = Bukkit.getPlayerExact(targetName);
        if (target != null && !sender.equals(target)) target.sendMessage(configManager.getMessage("admin-removed"));
        return true;
    }

    private boolean handleLog(CommandSender sender) {
        if (!sender.hasPermission("omar.command.log")) {
            sender.sendMessage(configManager.getMessage("no-permission")); return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(configManager.colorize("&#f38ba8只有玩家才能打开汇报记录面板！")); return true;
        }
        if (!reportManager.canReceiveReports(player)) {
            sender.sendMessage(configManager.getMessage("no-permission")); return true;
        }
        if (reportManager.getReports().isEmpty()) {
            sender.sendMessage(configManager.getFormattedMessage("no-reports")); return true;
        }
        ReportLogGUI.open(player, configManager, reportManager);
        return true;
    }

    private boolean handleImLog(CommandSender sender) {
        if (!sender.hasPermission("omar.command.imlog")) {
            sender.sendMessage(configManager.getMessage("no-permission")); return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(configManager.colorize("&#f38ba8只有玩家才能打开重点汇报记录面板！")); return true;
        }
        if (!reportManager.canReceiveReports(player)) {
            sender.sendMessage(configManager.getMessage("no-permission")); return true;
        }
        if (reportManager.getPriorityReports().isEmpty()) {
            sender.sendMessage(configManager.colorize(
                    configManager.getRawMessageWithoutPrefix("gui-imlog-no-reports")));
            return true;
        }
        PriorityReportLogGUI.open(player, configManager, reportManager);
        return true;
    }

    private boolean handleCheck(CommandSender sender, String[] args) {
        if (!sender.hasPermission("omar.command.check")) {
            sender.sendMessage(configManager.getMessage("no-permission")); return true;
        }
        if (args.length < 2) { sender.sendMessage(configManager.getMessage("invalid-args")); return true; }

        String targetName = args[1];
        final String searchName = targetName;

        List<OreReport> playerReports = reportManager.getReports().stream()
                .filter(r -> r.getPlayerName().equalsIgnoreCase(searchName))
                .collect(Collectors.toList());
        List<PriorityReport> playerPriorityReports = reportManager.getPriorityReports().stream()
                .filter(r -> r.getPlayerName().equalsIgnoreCase(searchName))
                .collect(Collectors.toList());

        boolean isBypassed = configManager.isBypassPlayer(searchName);

        // Try to find exact name from reports
        if (!playerReports.isEmpty()) targetName = playerReports.get(0).getPlayerName();
        else if (!playerPriorityReports.isEmpty()) targetName = playerPriorityReports.get(0).getPlayerName();
        else {
            Player onlinePlayer = Bukkit.getPlayerExact(searchName);
            if (onlinePlayer != null) targetName = onlinePlayer.getName();
        }

        sender.sendMessage(configManager.getFormattedMessageWithoutPrefix("check-header", "player", targetName));

        if (playerReports.isEmpty() && playerPriorityReports.isEmpty()) {
            sender.sendMessage(configManager.getMessageWithoutPrefix("check-no-data"));
        } else {
            sender.sendMessage(configManager.getFormattedMessageWithoutPrefix("check-reports",
                    "count", String.valueOf(playerReports.size())));
            sender.sendMessage(configManager.getFormattedMessageWithoutPrefix("check-priority-reports",
                    "count", String.valueOf(playerPriorityReports.size())));

            if (!playerReports.isEmpty()) {
                OreReport last = playerReports.get(0);
                sender.sendMessage(configManager.getFormattedMessageWithoutPrefix("check-last-report",
                        "time", last.getFormattedTime()));
            }
        }

        if (isBypassed) {
            sender.sendMessage(configManager.getMessageWithoutPrefix("check-is-bypassed"));
            sender.sendMessage(configManager.getMessageWithoutPrefix("check-bypass-hint"));
        } else {
            sender.sendMessage(configManager.getMessageWithoutPrefix("check-not-bypassed"));
        }

        return true;
    }

    private boolean handleBypass(CommandSender sender, String[] args) {
        if (!sender.hasPermission("omar.command.bypass")) {
            sender.sendMessage(configManager.getMessage("no-permission")); return true;
        }

        if (args.length < 2) { sender.sendMessage(configManager.getMessage("invalid-args")); return true; }

        String action = args[1].toLowerCase();

        switch (action) {
            case "add" -> {
                if (args.length < 3) { sender.sendMessage(configManager.getMessage("invalid-args")); return true; }
                String targetName = args[2];
                if (configManager.isBypassPlayer(targetName)) {
                    sender.sendMessage(configManager.getMessage("bypass-already"));
                    return true;
                }
                configManager.addBypassPlayer(targetName);
                sender.sendMessage(configManager.getFormattedMessage("bypass-added", "player", targetName));
            }
            case "remove" -> {
                if (args.length < 3) { sender.sendMessage(configManager.getMessage("invalid-args")); return true; }
                String targetName = args[2];
                if (!configManager.isBypassPlayer(targetName)) {
                    sender.sendMessage(configManager.getMessage("bypass-not-found"));
                    return true;
                }
                configManager.removeBypassPlayer(targetName);
                sender.sendMessage(configManager.getFormattedMessage("bypass-removed", "player", targetName));
            }
            case "list" -> {
                if (sender instanceof Player player) {
                    BypassListGUI.open(player, configManager, configManager.getBypassPlayers());
                    return true;
                }
                List<String> bypassed = configManager.getBypassPlayers();
                if (bypassed.isEmpty()) {
                    sender.sendMessage(configManager.getMessageWithoutPrefix("bypass-list-empty"));
                } else {
                    sender.sendMessage(configManager.getMessageWithoutPrefix("bypass-list-header"));
                    for (String name : bypassed) {
                        sender.sendMessage(configManager.getFormattedMessageWithoutPrefix("bypass-list-entry", "player", name));
                    }
                }
            }
            default -> sender.sendMessage(configManager.getMessage("invalid-args"));
        }
        return true;
    }

    private boolean handleStats(CommandSender sender) {
        if (!sender.hasPermission("omar.command.stats")) {
            sender.sendMessage(configManager.getMessage("no-permission")); return true;
        }

        String autoStatus = configManager.isAutoActionEnabled()
                ? "&#a6e3a1开启 &#9399b2(" + configManager.getAutoActionType() + ")"
                : "&#9399b2关闭";

        sender.sendMessage(configManager.getMessageWithoutPrefix("stats-header"));
        sender.sendMessage(configManager.getFormattedMessageWithoutPrefix("stats-total-ores",
                "count", String.valueOf(reportManager.getTotalOresDetected())));
        sender.sendMessage(configManager.getFormattedMessageWithoutPrefix("stats-total-reports",
                "count", String.valueOf(reportManager.getTotalReportsCreated())));
        sender.sendMessage(configManager.getFormattedMessageWithoutPrefix("stats-total-priority",
                "count", String.valueOf(reportManager.getTotalPriorityReportsCreated())));
        sender.sendMessage(configManager.getFormattedMessageWithoutPrefix("stats-bypass-count",
                "count", String.valueOf(configManager.getBypassPlayers().size())));
        sender.sendMessage(configManager.getFormattedMessageWithoutPrefix("stats-admin-count",
                "count", String.valueOf(configManager.getAdminNames().size())));
        sender.sendMessage(configManager.getFormattedMessageWithoutPrefix("stats-time-window",
                "count", String.valueOf(configManager.getTimeWindow())));
        sender.sendMessage(configManager.getFormattedMessageWithoutPrefix("stats-threshold",
                "count", String.valueOf(configManager.getOreThreshold())));
        sender.sendMessage(configManager.getFormattedMessageWithoutPrefix("stats-auto-action",
                "status", autoStatus));

        return true;
    }

    private boolean handlePanel(CommandSender sender) {
        if (!sender.hasPermission("omar.command.panel")) {
            sender.sendMessage(configManager.getMessage("no-permission")); return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(configManager.colorize("&#f38ba8只有玩家才能打开管理面板！")); return true;
        }
        MainPanelGUI.open(player, configManager, reportManager);
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("omar.command.reload")) {
            sender.sendMessage(configManager.getMessage("no-permission")); return true;
        }
        try {
            plugin.reloadPluginConfig();
            sender.sendMessage(configManager.getMessage("reload-success"));
            plugin.getLogger().info("配置文件已由 " + sender.getName() + " 重载");
        } catch (Exception e) {
            sender.sendMessage(configManager.getMessage("reload-failed"));
            plugin.getLogger().severe("重载配置文件失败: " + e.getMessage());
        }
        return true;
    }

    private boolean handleDelete(CommandSender sender) {
        if (!sender.hasPermission("omar.command.delete")) {
            sender.sendMessage(configManager.getMessage("no-permission")); return true;
        }
        try {
            reportManager.deleteAllReports();
            sender.sendMessage(configManager.getMessage("delete-success"));
            plugin.getLogger().info(sender.getName() + " 已删除所有汇报记录");
        } catch (Exception e) {
            sender.sendMessage(configManager.getMessage("delete-failed"));
            plugin.getLogger().severe("删除汇报记录失败: " + e.getMessage());
        }
        return true;
    }

    private boolean handleTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(configManager.colorize("&#f38ba8只有玩家才能使用传送功能！")); return true;
        }
        if (!sender.hasPermission("omar.command.tp") && !reportManager.canReceiveReports(player)) {
            sender.sendMessage(configManager.getMessage("no-permission")); return true;
        }
        if (args.length < 2) { sender.sendMessage(configManager.getMessage("invalid-args")); return true; }

        try {
            int reportId = Integer.parseInt(args[1]);
            OreReport report = reportManager.getReportById(reportId);
            if (report == null) { sender.sendMessage(configManager.getMessage("report-not-found")); return true; }

            World world = Bukkit.getWorld(report.getWorldName());
            if (world == null) { sender.sendMessage(configManager.getMessage("teleport-failed")); return true; }

            Location loc = report.getLocation(world);
            player.teleport(loc);
            player.sendMessage(configManager.getFormattedMessage("teleported",
                    "player", report.getPlayerName(),
                    "x", String.format("%.0f", report.getX()),
                    "y", String.format("%.0f", report.getY()),
                    "z", String.format("%.0f", report.getZ())));
        } catch (NumberFormatException e) {
            sender.sendMessage(configManager.getMessage("invalid-args"));
        }
        return true;
    }

    private boolean handlePriorityTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(configManager.colorize("&#f38ba8只有玩家才能使用传送功能！")); return true;
        }
        if (!sender.hasPermission("omar.command.tp") && !reportManager.canReceiveReports(player)) {
            sender.sendMessage(configManager.getMessage("no-permission")); return true;
        }
        if (args.length < 2) { sender.sendMessage(configManager.getMessage("invalid-args")); return true; }

        try {
            int reportId = Integer.parseInt(args[1]);
            PriorityReport report = reportManager.getPriorityReportById(reportId);
            if (report == null) { sender.sendMessage(configManager.getMessage("report-not-found")); return true; }

            World world = Bukkit.getWorld(report.getWorldName());
            if (world == null) { sender.sendMessage(configManager.getMessage("teleport-failed")); return true; }

            Location loc = report.getLocation(world);
            player.teleport(loc);
            player.sendMessage(configManager.getFormattedMessage("teleported",
                    "player", report.getPlayerName(),
                    "x", String.format("%.0f", report.getX()),
                    "y", String.format("%.0f", report.getY()),
                    "z", String.format("%.0f", report.getZ())));
        } catch (NumberFormatException e) {
            sender.sendMessage(configManager.getMessage("invalid-args"));
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        for (Component line : configManager.getFormattedMessageList("help-header")) {
            sender.sendMessage(line);
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String[] subs = {"add","remove","log","imlog","check","bypass","stats","panel","delete","reload","help"};
            for (String s : subs) {
                if (sender.hasPermission("omar.command." + s)) {
                    completions.add(s);
                }
            }
            return filter(completions, args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("add") || sub.equals("remove")) {
                return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), args[1]);
            }
            if (sub.equals("check")) {
                List<String> reportedPlayers = new ArrayList<>();
                for (OreReport r : reportManager.getReports()) {
                    if (!reportedPlayers.contains(r.getPlayerName())) reportedPlayers.add(r.getPlayerName());
                }
                return filter(reportedPlayers, args[1]);
            }
            if (sub.equals("bypass")) {
                return filter(List.of("add", "remove", "list"), args[1]);
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("bypass")) {
            String action = args[1].toLowerCase();
            if (action.equals("add") || action.equals("remove")) {
                return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), args[2]);
            }
        }

        return List.of();
    }

    private List<String> filter(List<String> list, String prefix) {
        if (prefix.isEmpty()) return list;
        return list.stream().filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase())).collect(Collectors.toList());
    }
}

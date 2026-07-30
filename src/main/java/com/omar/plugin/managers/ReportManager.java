package com.omar.plugin.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.omar.plugin.OmarPlugin;
import com.omar.plugin.models.MinedOre;
import com.omar.plugin.models.OreReport;
import com.omar.plugin.models.PriorityReport;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class ReportManager {

    private static final int MAX_REPORTS = 1000;
    private static final int MAX_PRIORITY_REPORTS = 500;
    private static final String REPORTS_FILE_NAME = "reports.json";
    private static final String PRIORITY_REPORTS_FILE_NAME = "priority_reports.json";

    private final OmarPlugin plugin;
    private final ConfigManager configManager;

    private final Map<UUID, List<MinedOre>> playerMiningRecords = new ConcurrentHashMap<>();
    private final List<OreReport> reports = new ArrayList<>();
    private int nextReportId = 1;

    private final Map<UUID, List<Long>> playerReportTimestamps = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastPriorityReportTime = new ConcurrentHashMap<>();
    private final List<PriorityReport> priorityReports = new ArrayList<>();
    private int nextPriorityReportId = 1;

    private final AtomicLong totalOresDetected = new AtomicLong(0);
    private final AtomicInteger totalReportsCreated = new AtomicInteger(0);
    private final AtomicInteger totalPriorityReportsCreated = new AtomicInteger(0);

    private final Gson gson;

    private static class ReportData {
        int nextId;
        List<SerializedReport> reports;

        static class SerializedReport {
            int id; String playerName; String playerUuid; String oreTypeName;
            int count; String worldName; double x; double y; double z; long timestamp;
        }
    }

    private static class PriorityReportData {
        int nextId;
        List<SerializedPriorityReport> reports;

        static class SerializedPriorityReport {
            int id; String playerName; String playerUuid; int reportCount;
            String worldName; double x; double y; double z; long timestamp;
        }
    }

    private static class StatsData {
        long totalOresDetected;
        int totalReportsCreated;
        int totalPriorityReportsCreated;
    }

    private static final String STATS_FILE_NAME = "stats.json";

    public ReportManager(OmarPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        loadStats();
        loadReports();
        loadPriorityReports();
    }

    public void onPlayerMineOre(Player player, Material oreType, Location location) {
        if (configManager.isBypassPlayer(player.getName())) return;
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE ||
            player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            return;
        }

        totalOresDetected.incrementAndGet();

        UUID playerUuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        int timeWindow = configManager.getTimeWindow();

        int threshold = getThresholdForOre(oreType);
        if (threshold <= 0) threshold = configManager.getOreThreshold();

        List<MinedOre> records = playerMiningRecords.computeIfAbsent(playerUuid, k -> new ArrayList<>());
        records.add(new MinedOre(oreType, location, now));
        records.removeIf(record -> !record.isWithinWindow(timeWindow, now));

        if (records.size() >= threshold) {
            MinedOre lastOre = records.get(records.size() - 1);
            createReport(player, lastOre.getOreType(), records.size(),
                    lastOre.getWorldName(), lastOre.getX(), lastOre.getY(), lastOre.getZ(), now);
            records.clear();
        }
    }

    private int getThresholdForOre(Material oreType) {
        return switch (oreType) {
            case DIAMOND_ORE -> configManager.getDiamondOreThreshold();
            case DEEPSLATE_DIAMOND_ORE -> configManager.getDeepslateDiamondOreThreshold();
            case ANCIENT_DEBRIS -> configManager.getAncientDebrisThreshold();
            default -> 0;
        };
    }

    private void createReport(Player player, Material oreType, int count,
                              String worldName, double x, double y, double z, long timestamp) {
        int reportId = nextReportId++;
        Location loc = new Location(
                Bukkit.getWorld(worldName) != null ? Bukkit.getWorld(worldName) : Bukkit.getWorlds().get(0),
                x, y, z);

        OreReport report = new OreReport(reportId, player.getName(), player.getUniqueId(),
                oreType, count, loc, timestamp);

        reports.add(0, report);
        while (reports.size() > MAX_REPORTS) reports.remove(reports.size() - 1);

        totalReportsCreated.incrementAndGet();
        saveReports();
        saveStats();
        notifyAdmins(report);
        checkPriorityEscalation(player, loc, timestamp);
    }

    private void checkPriorityEscalation(Player player, Location location, long now) {
        UUID uuid = player.getUniqueId();
        int priorityWindow = configManager.getPriorityTimeWindow();
        int priorityThreshold = configManager.getPriorityReportThreshold();
        if (priorityThreshold <= 0) return;

        List<Long> timestamps = playerReportTimestamps.computeIfAbsent(uuid, k -> new ArrayList<>());
        timestamps.add(now);
        timestamps.removeIf(t -> (now - t) > priorityWindow * 1000L);

        if (timestamps.size() >= priorityThreshold) {
            long lastPriority = lastPriorityReportTime.getOrDefault(uuid, 0L);
            if (now - lastPriority > priorityWindow * 1000L) {
                createPriorityReport(player, timestamps.size(), location, now);
                lastPriorityReportTime.put(uuid, now);
                timestamps.clear();
            }
        }
    }

    private void createPriorityReport(Player player, int reportCount, Location location, long timestamp) {
        int reportId = nextPriorityReportId++;
        PriorityReport report = new PriorityReport(reportId, player.getName(), player.getUniqueId(),
                reportCount, location, timestamp);

        priorityReports.add(0, report);
        while (priorityReports.size() > MAX_PRIORITY_REPORTS) priorityReports.remove(priorityReports.size() - 1);

        totalPriorityReportsCreated.incrementAndGet();
        savePriorityReports();
        saveStats();
        notifyPriorityAdmins(report);

        if (configManager.isWarnSuspectedPlayer()) {
            warnPlayer(player);
        }

        executeAutoAction(player);
    }

    private void executeAutoAction(Player player) {
        if (!configManager.isAutoActionEnabled()) return;

        String action = configManager.getAutoActionType();
        switch (action) {
            case "kick" -> {
                String msg = ConfigManager.translateColorCodes(configManager.getRawAutoActionMessage());
                player.kickPlayer(msg);
            }
            case "ban" -> {
                String msg = ConfigManager.translateColorCodes(configManager.getRawAutoActionMessage());
                player.banPlayer(msg);
            }
            case "command" -> {
                String cmd = configManager.getAutoActionCommand()
                        .replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
        }
    }

    private void notifyPriorityAdmins(PriorityReport report) {
        Component alertMessage = buildPriorityAlertComponent(report);
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (canReceiveReports(onlinePlayer)) {
                onlinePlayer.sendMessage(alertMessage);
                playAlertSound(onlinePlayer);
            }
        }
        plugin.getLogger().info(String.format(
                "[重点警告] %s 已被多次汇报！累计 %d 次，坐标: %s (%.0f, %.0f, %.0f)",
                report.getPlayerName(), report.getReportCount(),
                report.getWorldName(), report.getX(), report.getY(), report.getZ()));
    }

    private Component buildPriorityAlertComponent(PriorityReport report) {
        String hoverText = configManager.getRawMessageWithoutPrefix("priority-report-hover-text");
        return configManager.colorize(
                configManager.getRawMessageWithoutPrefix("priority-report-alert")
                        .replace("%player%", report.getPlayerName())
                        .replace("%count%", String.valueOf(report.getReportCount()))
        ).clickEvent(ClickEvent.runCommand("/omar prioritytp " + report.getId()))
         .hoverEvent(HoverEvent.showText(configManager.colorize(hoverText)));
    }

    private void warnPlayer(Player player) {
        String warnMessage = configManager.getRawMessageWithoutPrefix("warn-suspected-message");
        player.sendMessage(configManager.colorize(warnMessage));
    }

    private void notifyAdmins(OreReport report) {
        Component alertMessage = buildAlertComponent(report);
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (canReceiveReports(onlinePlayer)) {
                onlinePlayer.sendMessage(alertMessage);
                playAlertSound(onlinePlayer);
            }
        }
        plugin.getLogger().info(String.format(
                "[矿透警报] %s 有开矿透嫌疑！挖掘了 %d 个 %s，坐标: %s (%.0f, %.0f, %.0f)",
                report.getPlayerName(), report.getCount(), report.getOreTypeName(),
                report.getWorldName(), report.getX(), report.getY(), report.getZ()));
    }

    private Component buildAlertComponent(OreReport report) {
        String hoverText = configManager.getRawMessageWithoutPrefix("report-hover-text");
        return configManager.colorize(
                configManager.getRawMessageWithoutPrefix("report-alert")
                        .replace("%player%", report.getPlayerName())
        ).clickEvent(ClickEvent.runCommand("/omar tp " + report.getId()))
         .hoverEvent(HoverEvent.showText(configManager.colorize(hoverText)));
    }

    private void playAlertSound(Player player) {
        if (!configManager.isSoundEnabled()) return;
        try {
            Sound sound = Sound.valueOf(configManager.getSoundName());
            player.playSound(player.getLocation(), sound,
                    configManager.getSoundVolume(), configManager.getSoundPitch());
        } catch (IllegalArgumentException e) {
            try {
                player.playSound(player.getLocation(),
                        org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING,
                        configManager.getSoundVolume(), configManager.getSoundPitch());
            } catch (Exception ignored) {}
        }
    }

    public boolean canReceiveReports(Player player) {
        return player.hasPermission("omar.report.receive")
                || configManager.isAdminName(player.getName());
    }

    // ===== 普通汇报 =====

    public List<OreReport> getReports() { return new ArrayList<>(reports); }

    public OreReport getReportById(int id) {
        return reports.stream().filter(r -> r.getId() == id).findFirst().orElse(null);
    }

    // ===== 重点汇报 =====

    public List<PriorityReport> getPriorityReports() { return new ArrayList<>(priorityReports); }

    public PriorityReport getPriorityReportById(int id) {
        return priorityReports.stream().filter(r -> r.getId() == id).findFirst().orElse(null);
    }

    // ===== 统计 =====

    public long getTotalOresDetected() { return totalOresDetected.get(); }
    public int getTotalReportsCreated() { return totalReportsCreated.get(); }
    public int getTotalPriorityReportsCreated() { return totalPriorityReportsCreated.get(); }

    // ===== 持久化 =====

    public void saveReports() {
        File dataFile = new File(plugin.getDataFolder(), REPORTS_FILE_NAME);
        try {
            if (!dataFile.getParentFile().exists()) dataFile.getParentFile().mkdirs();
            ReportData data = new ReportData();
            data.nextId = nextReportId;
            data.reports = reports.stream().map(r -> {
                ReportData.SerializedReport sr = new ReportData.SerializedReport();
                sr.id = r.getId(); sr.playerName = r.getPlayerName();
                sr.playerUuid = r.getPlayerUuid().toString();
                sr.oreTypeName = r.getOreType().name(); sr.count = r.getCount();
                sr.worldName = r.getWorldName(); sr.x = r.getX(); sr.y = r.getY();
                sr.z = r.getZ(); sr.timestamp = r.getTimestamp();
                return sr;
            }).collect(Collectors.toList());
            try (Writer w = new OutputStreamWriter(new FileOutputStream(dataFile), StandardCharsets.UTF_8)) {
                gson.toJson(data, w);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存汇报记录: " + e.getMessage());
        }
    }

    public void loadReports() {
        File dataFile = new File(plugin.getDataFolder(), REPORTS_FILE_NAME);
        if (!dataFile.exists()) return;
        try (Reader r = new InputStreamReader(new FileInputStream(dataFile), StandardCharsets.UTF_8)) {
            ReportData data = gson.fromJson(r, ReportData.class);
            if (data == null) return;
            this.nextReportId = data.nextId;
            this.reports.clear();
            for (ReportData.SerializedReport sr : data.reports) {
                this.reports.add(new OreReport(sr.id, sr.playerName, UUID.fromString(sr.playerUuid),
                        sr.oreTypeName, sr.count, sr.worldName, sr.x, sr.y, sr.z, sr.timestamp));
            }
        } catch (IOException e) {
            plugin.getLogger().severe("无法加载汇报记录: " + e.getMessage());
        }
    }

    public void savePriorityReports() {
        File dataFile = new File(plugin.getDataFolder(), PRIORITY_REPORTS_FILE_NAME);
        try {
            if (!dataFile.getParentFile().exists()) dataFile.getParentFile().mkdirs();
            PriorityReportData data = new PriorityReportData();
            data.nextId = nextPriorityReportId;
            data.reports = priorityReports.stream().map(r -> {
                PriorityReportData.SerializedPriorityReport sr = new PriorityReportData.SerializedPriorityReport();
                sr.id = r.getId(); sr.playerName = r.getPlayerName();
                sr.playerUuid = r.getPlayerUuid().toString(); sr.reportCount = r.getReportCount();
                sr.worldName = r.getWorldName(); sr.x = r.getX(); sr.y = r.getY();
                sr.z = r.getZ(); sr.timestamp = r.getTimestamp();
                return sr;
            }).collect(Collectors.toList());
            try (Writer w = new OutputStreamWriter(new FileOutputStream(dataFile), StandardCharsets.UTF_8)) {
                gson.toJson(data, w);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存重点汇报记录: " + e.getMessage());
        }
    }

    public void loadPriorityReports() {
        File dataFile = new File(plugin.getDataFolder(), PRIORITY_REPORTS_FILE_NAME);
        if (!dataFile.exists()) return;
        try (Reader r = new InputStreamReader(new FileInputStream(dataFile), StandardCharsets.UTF_8)) {
            PriorityReportData data = gson.fromJson(r, PriorityReportData.class);
            if (data == null) return;
            this.nextPriorityReportId = data.nextId;
            this.priorityReports.clear();
            for (PriorityReportData.SerializedPriorityReport sr : data.reports) {
                this.priorityReports.add(new PriorityReport(sr.id, sr.playerName,
                        UUID.fromString(sr.playerUuid), sr.reportCount,
                        sr.worldName, sr.x, sr.y, sr.z, sr.timestamp));
            }
        } catch (IOException e) {
            plugin.getLogger().severe("无法加载重点汇报记录: " + e.getMessage());
        }
    }

    private void saveStats() {
        File dataFile = new File(plugin.getDataFolder(), STATS_FILE_NAME);
        try {
            if (!dataFile.getParentFile().exists()) dataFile.getParentFile().mkdirs();
            StatsData data = new StatsData();
            data.totalOresDetected = totalOresDetected.get();
            data.totalReportsCreated = totalReportsCreated.get();
            data.totalPriorityReportsCreated = totalPriorityReportsCreated.get();
            try (Writer w = new OutputStreamWriter(new FileOutputStream(dataFile), StandardCharsets.UTF_8)) {
                gson.toJson(data, w);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存统计信息: " + e.getMessage());
        }
    }

    private void loadStats() {
        File dataFile = new File(plugin.getDataFolder(), STATS_FILE_NAME);
        if (!dataFile.exists()) return;
        try (Reader r = new InputStreamReader(new FileInputStream(dataFile), StandardCharsets.UTF_8)) {
            StatsData data = gson.fromJson(r, StatsData.class);
            if (data == null) return;
            totalOresDetected.set(data.totalOresDetected);
            totalReportsCreated.set(data.totalReportsCreated);
            totalPriorityReportsCreated.set(data.totalPriorityReportsCreated);
        } catch (IOException e) {
            plugin.getLogger().severe("无法加载统计信息: " + e.getMessage());
        }
    }

    // ===== 清理 =====

    public void clearPlayerRecords(Player player) {
        playerMiningRecords.remove(player.getUniqueId());
    }

    public void deleteAllReports() {
        reports.clear(); nextReportId = 1; saveReports();
        priorityReports.clear(); nextPriorityReportId = 1; savePriorityReports();
        playerReportTimestamps.clear();
        lastPriorityReportTime.clear();
    }

    public void deleteAllPriorityReports() {
        priorityReports.clear(); nextPriorityReportId = 1; savePriorityReports();
    }

    public void resetStats() {
        totalOresDetected.set(0);
        totalReportsCreated.set(0);
        totalPriorityReportsCreated.set(0);
        saveStats();
    }

    public void shutdown() {
        saveReports(); savePriorityReports(); saveStats();
        playerMiningRecords.clear();
        playerReportTimestamps.clear();
        lastPriorityReportTime.clear();
        reports.clear(); priorityReports.clear();
    }
}

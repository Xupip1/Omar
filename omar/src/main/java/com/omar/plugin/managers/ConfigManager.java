package com.omar.plugin.managers;

import com.omar.plugin.OmarPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ConfigManager {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");

    private final OmarPlugin plugin;
    private FileConfiguration config;

    private int timeWindow;
    private int oreThreshold;
    private int priorityTimeWindow;
    private int priorityReportThreshold;
    private boolean warnSuspectedPlayer;

    private int diamondOreThreshold;
    private int deepslateDiamondOreThreshold;
    private int ancientDebrisThreshold;

    private boolean autoActionEnabled;
    private String autoActionType;
    private String autoActionCommand;
    private String autoActionMessage;

    private boolean soundEnabled;
    private String soundName;
    private float soundVolume;
    private float soundPitch;

    private List<String> adminNames;
    private List<String> bypassPlayers;

    public ConfigManager(OmarPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        this.timeWindow = config.getInt("time-window", 120);
        this.oreThreshold = config.getInt("ore-threshold", 5);
        this.priorityTimeWindow = config.getInt("priority-time-window", 300);
        this.priorityReportThreshold = config.getInt("priority-report-threshold", 3);
        this.warnSuspectedPlayer = config.getBoolean("warn-suspected-player", true);

        this.diamondOreThreshold = config.getInt("ore-thresholds.diamond_ore", 0);
        this.deepslateDiamondOreThreshold = config.getInt("ore-thresholds.deepslate_diamond_ore", 0);
        this.ancientDebrisThreshold = config.getInt("ore-thresholds.ancient_debris", 0);

        this.autoActionEnabled = config.getBoolean("auto-action.enabled", false);
        this.autoActionType = config.getString("auto-action.action", "none");
        this.autoActionCommand = config.getString("auto-action.command", "");
        this.autoActionMessage = config.getString("auto-action.message", "&#f38ba8您因矿透嫌疑已被移出服务器");

        this.soundEnabled = config.getBoolean("sound-notification.enabled", true);
        this.soundName = config.getString("sound-notification.sound", "BLOCK_NOTE_BLOCK_PLING");
        this.soundVolume = (float) config.getDouble("sound-notification.volume", 1.0);
        this.soundPitch = (float) config.getDouble("sound-notification.pitch", 1.0);

        this.adminNames = config.getStringList("admins");
        if (this.adminNames == null) this.adminNames = new ArrayList<>();

        this.bypassPlayers = config.getStringList("bypass-players");
        if (this.bypassPlayers == null) this.bypassPlayers = new ArrayList<>();

        config.options().copyDefaults(true);
        plugin.saveConfig();
    }

    public void reload() {
        loadConfig();
    }

    // ===== 基础检测配置 =====

    public int getTimeWindow() {
        return timeWindow;
    }

    public int getOreThreshold() {
        return oreThreshold;
    }

    public int getPriorityTimeWindow() {
        return priorityTimeWindow;
    }

    public int getPriorityReportThreshold() {
        return priorityReportThreshold;
    }

    public boolean isWarnSuspectedPlayer() {
        return warnSuspectedPlayer;
    }

    // ===== 矿石独立阈值 =====

    public int getDiamondOreThreshold() {
        return diamondOreThreshold;
    }

    public int getDeepslateDiamondOreThreshold() {
        return deepslateDiamondOreThreshold;
    }

    public int getAncientDebrisThreshold() {
        return ancientDebrisThreshold;
    }

    // ===== 自动处理措施 =====

    public boolean isAutoActionEnabled() {
        return autoActionEnabled;
    }

    public String getAutoActionType() {
        return autoActionType;
    }

    public String getAutoActionCommand() {
        return autoActionCommand;
    }

    public String getRawAutoActionMessage() {
        return autoActionMessage;
    }

    // ===== 声音通知 =====

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public String getSoundName() {
        return soundName;
    }

    public float getSoundVolume() {
        return soundVolume;
    }

    public float getSoundPitch() {
        return soundPitch;
    }

    // ===== 管理员列表 =====

    public List<String> getAdminNames() {
        return adminNames;
    }

    public void addAdminName(String playerName) {
        if (adminNames.stream().noneMatch(name -> name.equalsIgnoreCase(playerName))) {
            adminNames.add(playerName);
            config.set("admins", adminNames);
            plugin.saveConfig();
        }
    }

    public void removeAdminName(String playerName) {
        adminNames.removeIf(name -> name.equalsIgnoreCase(playerName));
        config.set("admins", adminNames);
        plugin.saveConfig();
    }

    public boolean isAdminName(String playerName) {
        return adminNames.stream().anyMatch(name -> name.equalsIgnoreCase(playerName));
    }

    // ===== 绕过白名单 =====

    public List<String> getBypassPlayers() {
        return bypassPlayers;
    }

    public void addBypassPlayer(String playerName) {
        if (bypassPlayers.stream().noneMatch(name -> name.equalsIgnoreCase(playerName))) {
            bypassPlayers.add(playerName);
            config.set("bypass-players", bypassPlayers);
            plugin.saveConfig();
        }
    }

    public void removeBypassPlayer(String playerName) {
        bypassPlayers.removeIf(name -> name.equalsIgnoreCase(playerName));
        config.set("bypass-players", bypassPlayers);
        plugin.saveConfig();
    }

    public boolean isBypassPlayer(String playerName) {
        return bypassPlayers.stream().anyMatch(name -> name.equalsIgnoreCase(playerName));
    }

    // ===== 消息获取 =====

    public String getRawMessage(String key) {
        String prefix = config.getString("messages.prefix", "&#9399b2[&#fab387Omar&#9399b2]");
        String message = config.getString("messages." + key);
        if (message == null) return "";
        return translateColorCodes(prefix + " " + message);
    }

    public String getRawMessageWithoutPrefix(String key) {
        String message = config.getString("messages." + key);
        if (message == null) return "";
        return translateColorCodes(message);
    }

    public List<String> getRawMessageList(String key) {
        List<String> list = config.getStringList("messages." + key);
        return list.stream()
                .map(ConfigManager::translateColorCodes)
                .collect(Collectors.toList());
    }

    public Component getMessage(String key) {
        String prefix = config.getString("messages.prefix", "&#9399b2[&#fab387Omar&#9399b2]");
        String message = config.getString("messages." + key);
        if (message == null) return Component.empty();
        return colorize(prefix + " " + message);
    }

    public Component getMessageWithoutPrefix(String key) {
        String message = config.getString("messages." + key);
        if (message == null) return Component.empty();
        return colorize(message);
    }

    public Component getFormattedMessage(String key, String... replacements) {
        String prefix = config.getString("messages.prefix", "&#9399b2[&#fab387Omar&#9399b2]");
        String message = config.getString("messages." + key);
        if (message == null) return Component.empty();

        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace("%" + replacements[i] + "%", replacements[i + 1]);
            }
        }
        return colorize(prefix + " " + message);
    }

    public Component getFormattedMessageWithoutPrefix(String key, String... replacements) {
        String message = config.getString("messages." + key);
        if (message == null) return Component.empty();

        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace("%" + replacements[i] + "%", replacements[i + 1]);
            }
        }
        return colorize(message);
    }

    public List<Component> getFormattedMessageList(String key, String... replacements) {
        List<String> rawList = config.getStringList("messages." + key);
        List<Component> result = new ArrayList<>();
        for (String line : rawList) {
            for (int i = 0; i < replacements.length; i += 2) {
                if (i + 1 < replacements.length) {
                    line = line.replace("%" + replacements[i] + "%", replacements[i + 1]);
                }
            }
            result.add(colorize(line));
        }
        return result;
    }

    public String getGuiTitle() {
        return translateColorCodes(config.getString("messages.gui-title", "&#9399b2矿石检测汇报记录"));
    }

    // ===== 工具方法 =====

    private static String translateHexCodes(String message) {
        if (message == null) return null;
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("&x");
            for (char c : hex.toCharArray()) {
                replacement.append('&').append(c);
            }
            matcher.appendReplacement(sb, replacement.toString());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static String translateColorCodes(String message) {
        if (message == null) return "";
        message = translateHexCodes(message);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static Component colorize(String message) {
        if (message == null || message.isEmpty()) return Component.empty();
        message = translateHexCodes(message);
        return LegacyComponentSerializer.legacySection().deserialize(
                ChatColor.translateAlternateColorCodes('&', message)
        );
    }
}

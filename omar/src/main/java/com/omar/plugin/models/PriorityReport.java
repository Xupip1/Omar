package com.omar.plugin.models;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public class PriorityReport {

    private final int id;
    private final String playerName;
    private final UUID playerUuid;
    private final int reportCount;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final long timestamp;

    public PriorityReport(int id, String playerName, UUID playerUuid, int reportCount,
                          Location location, long timestamp) {
        this.id = id;
        this.playerName = playerName;
        this.playerUuid = playerUuid;
        this.reportCount = reportCount;
        this.worldName = location.getWorld() != null ? location.getWorld().getName() : "unknown";
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.timestamp = timestamp;
    }

    public PriorityReport(int id, String playerName, UUID playerUuid, int reportCount,
                          String worldName, double x, double y, double z, long timestamp) {
        this.id = id;
        this.playerName = playerName;
        this.playerUuid = playerUuid;
        this.reportCount = reportCount;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public String getPlayerName() {
        return playerName;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public int getReportCount() {
        return reportCount;
    }

    public String getWorldName() {
        return worldName;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Location getLocation(World world) {
        return new Location(world, x, y, z);
    }

    public String getFormattedTime() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date(timestamp));
    }
}

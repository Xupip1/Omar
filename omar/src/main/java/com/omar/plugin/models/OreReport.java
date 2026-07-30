package com.omar.plugin.models;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.UUID;

public class OreReport {

    private final int id;
    private final String playerName;
    private final UUID playerUuid;
    private final String oreTypeName;
    private final int count;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final long timestamp;

    public OreReport(int id, String playerName, UUID playerUuid, Material oreType,
                     int count, Location location, long timestamp) {
        this.id = id;
        this.playerName = playerName;
        this.playerUuid = playerUuid;
        this.oreTypeName = oreType.name();
        this.count = count;
        this.worldName = location.getWorld() != null ? location.getWorld().getName() : "unknown";
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.timestamp = timestamp;
    }

    public OreReport(int id, String playerName, UUID playerUuid, String oreTypeName,
                     int count, String worldName, double x, double y, double z, long timestamp) {
        this.id = id;
        this.playerName = playerName;
        this.playerUuid = playerUuid;
        this.oreTypeName = oreTypeName;
        this.count = count;
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

    public Material getOreType() {
        try {
            return Material.valueOf(oreTypeName);
        } catch (IllegalArgumentException e) {
            return Material.DIAMOND_ORE;
        }
    }

    public String getOreTypeName() {
        String type = oreTypeName;
        switch (type) {
            case "DIAMOND_ORE":
                return "钻石矿石";
            case "DEEPSLATE_DIAMOND_ORE":
                return "深层钻石矿石";
            case "ANCIENT_DEBRIS":
                return "远古残骸";
            default:
                return type;
        }
    }

    public int getCount() {
        return count;
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

    public String getFormattedLocation() {
        return String.format("%.0f, %.0f, %.0f", x, y, z);
    }
}

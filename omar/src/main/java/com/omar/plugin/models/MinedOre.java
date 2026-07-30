package com.omar.plugin.models;

import org.bukkit.Location;
import org.bukkit.Material;

public class MinedOre {

    private final Material oreType;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final long timestamp;

    public MinedOre(Material oreType, Location location, long timestamp) {
        this.oreType = oreType;
        this.worldName = location.getWorld() != null ? location.getWorld().getName() : "unknown";
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.timestamp = timestamp;
    }

    public Material getOreType() {
        return oreType;
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

    public boolean isWithinWindow(long windowSeconds, long now) {
        return (now - timestamp) <= windowSeconds * 1000L;
    }
}

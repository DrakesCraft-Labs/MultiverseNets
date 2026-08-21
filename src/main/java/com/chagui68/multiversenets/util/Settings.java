package com.chagui68.multiversenets.util;

import com.chagui68.multiversenets.MultiverseNets;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public final class Settings {

    private static FileConfiguration cfg;

    private Settings() {
    }

    public static void refresh(MultiverseNets plugin) {
        cfg = plugin.getConfig();
    }

    public static int scanIntervalTicks() {
        return Math.max(5, cfg.getInt("network.scan-interval-ticks", 20));
    }

    public static int maxNodes() {
        return Math.max(16, cfg.getInt("network.max-nodes", 4096));
    }

    public static int transferIntervalTicks() {
        return Math.max(1, cfg.getInt("network.op-interval-ticks.transfer", 5));
    }

    public static int vacuumIntervalTicks() {
        return Math.max(1, cfg.getInt("network.op-interval-ticks.vacuum", 10));
    }

    public static int craftIntervalTicks() {
        return Math.max(1, cfg.getInt("network.op-interval-ticks.craft", 20));
    }

    public static int itemsPerOp() {
        return Math.max(1, cfg.getInt("transfer.items-per-op", 64));
    }

    public static int htMultiplier() {
        return Math.max(1, cfg.getInt("transfer.ht-multiplier", 8));
    }

    public static long greedyCapacity() {
        return Math.max(1, cfg.getLong("greedy.capacity", 262144L));
    }

    public static int maxBlueprints() {
        return Math.max(1, cfg.getInt("crafter.max-recipes", 26));
    }

    public static double vacuumRadius() {
        return Math.max(1.0, cfg.getDouble("vacuum.radius", 4.0));
    }

    public static long cellCapacity(int tier1to6) {
        List<Integer> caps = cfg.getIntegerList("cells.capacities");
        int idx = Math.clamp(tier1to6 - 1, 0, caps.size() - 1);
        return idx >= 0 && !caps.isEmpty() ? caps.get(idx) : 65536L * (1L << idx);
    }

    public static boolean debug() {
        return cfg.getBoolean("debug", false);
    }
}

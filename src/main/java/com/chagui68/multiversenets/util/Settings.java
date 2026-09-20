package com.chagui68.multiversenets.util;

import com.chagui68.multiversenets.MultiverseNets;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * [EN] Configuration & Settings Provider
 * Central access point for cached and validated configuration values from {@code config.yml}.
 *
 * [ES] Proveedor de Configuración y Ajustes
 * Punto central de acceso para los valores de configuración validados desde {@code config.yml}.
 */
public final class Settings {

    private static FileConfiguration cfg;
    private static final Set<Integer> WARNED_TIERS = new HashSet<>();

    private Settings() {
    }

    /**
     * EN: Refreshes the cached configuration instance.
 *
     * ES: Recarga la instancia en caché del archivo de configuración.
     *
     * @param plugin The plugin instance / ES: La instancia del plugin.
     */
    public static void refresh(MultiverseNets plugin) {
        cfg = plugin.getConfig();
    }

    /**
     * EN: Returns the interval (in ticks) between topology rescans.
     *
     * ES: Devuelve el intervalo (en ticks) entre reescaneos de topología de red.
     */
    public static int scanIntervalTicks() {
        return cfg != null ? Math.max(5, cfg.getInt("network.scan-interval-ticks", 20)) : 20;
    }

    /**
     * EN: Returns the maximum number of connected nodes allowed per network.
     *
     * ES: Devuelve el número máximo de nodos conectados permitidos por red.
     */
    public static int maxNodes() {
        return cfg != null ? Math.max(16, cfg.getInt("network.max-nodes", 16384)) : 16384;
    }

    /**
     * EN: Returns the interval (in ticks) between transfer operations (grabbers/pushers).
     *
     * ES: Devuelve el intervalo (en ticks) entre operaciones de transferencia (grabbers/pushers).
     */
    public static int transferIntervalTicks() {
        return cfg != null ? Math.max(1, cfg.getInt("network.op-interval-ticks.transfer", 5)) : 5;
    }

    /**
     * EN: Returns the interval (in ticks) between vacuum pickup cycles.
     *
     * ES: Devuelve el intervalo (en ticks) entre ciclos de recolección de los vacuums.
     */
    public static int vacuumIntervalTicks() {
        return cfg != null ? Math.max(1, cfg.getInt("network.op-interval-ticks.vacuum", 10)) : 10;
    }

    /**
     * EN: Returns the interval (in ticks) between auto-crafting attempts.
     *
     * ES: Devuelve el intervalo (en ticks) entre intentos de autocrafteo.
     */
    public static int craftIntervalTicks() {
        return cfg != null ? Math.max(1, cfg.getInt("network.op-interval-ticks.craft", 20)) : 20;
    }

    /**
     * EN: Base amount of items transferred per tick operation.
     *
     * ES: Cantidad base de ítems transferidos por operación de tick.
     */
    public static int itemsPerOp() {
        return cfg != null ? Math.max(1, cfg.getInt("transfer.items-per-op", 64)) : 64;
    }

    /**
     * EN: Multiplier for high-throughput (HT) importers and exporters.
     *
     * ES: Multiplicador para importadores y exportadores de alto rendimiento (HT).
     */
    public static int htMultiplier() {
        return cfg != null ? Math.max(1, cfg.getInt("transfer.ht-multiplier", 8)) : 8;
    }

    /**
     * EN: Buffer capacity of a Greedy Cell.
     *
     * ES: Capacidad de almacenamiento de una Greedy Cell.
     */
    public static long greedyCapacity() {
        return cfg != null ? Math.max(1L, cfg.getLong("greedy.capacity", 262144L)) : 262144L;
    }

    /**
     * EN: Storage capacity of an Infinity Barrel.
     *
     * ES: Capacidad de almacenamiento de un Infinity Barrel.
     */
    public static long barrelCapacity() {
        return Math.max(1L, cfg != null ? cfg.getLong("barrel.capacity", 2_000_000_000L) : 2_000_000_000L);
    }

    /**
     * EN: Maximum number of blueprints/recipes installed in an Auto-Crafter.
     *
     * ES: Número máximo de blueprints/recetas instalables en un Auto-Crafter.
     */
    public static int maxBlueprints() {
        int raw = cfg != null ? cfg.getInt("crafter.max-recipes", 18) : 18;
        return Math.min(18, Math.max(1, raw));
    }

    /**
     * EN: Pickup radius in blocks for Network Vacuums.
     *
     * ES: Radio de recolección en bloques para los Network Vacuums.
     */
    public static double vacuumRadius() {
        return cfg != null ? Math.max(1.0, cfg.getDouble("vacuum.radius", 4.0)) : 4.0;
    }

    /**
     * EN: Storage capacity for a cell of a given tier (1..6), with automatic fallback.
     *
     * ES: Capacidad de una celda por nivel (1..6), con respaldo automático si no está declarada.
     *
     * @param tier1to6 Cell tier / ES: Nivel de la celda.
     * @return Max item capacity / ES: Capacidad máxima de ítems.
     */
    public static long cellCapacity(int tier1to6) {
        int tier = Math.max(1, tier1to6);
        long fallback = 65536L * (1L << (tier - 1));

        if (cfg == null) {
            return fallback;
        }

        List<Long> caps = cfg.getLongList("cells.capacities");
        if (caps.isEmpty()) {
            return fallback;
        }
        if (tier > caps.size()) {
            warnMissingTierCapacity(tier, caps.size());
            return caps.get(caps.size() - 1);
        }
        return caps.get(tier - 1);
    }

    private static void warnMissingTierCapacity(int tier, int declared) {
        if (!WARNED_TIERS.add(tier)) {
            return;
        }
        String message = "[MultiverseNets] Quantum Cell tier " + tier
                + " has no capacity in cells.capacities (only " + declared
                + " declared). Using the last tier capacity.";
        if (org.bukkit.Bukkit.getServer() == null) {
            Logger.getLogger("MultiverseNets").warning(message);
        } else {
            org.bukkit.Bukkit.getLogger().warning(message);
        }
    }

    /**
     * EN: Returns true if Slimefun integration is enabled in config.
 *
     * ES: Devuelve true si la integración con Slimefun está habilitada en config.
     */
    /**
     * EN: True if network devices are forbidden in this world (config blocked-worlds).
     * ES: True si los dispositivos de red están prohibidos en ese mundo (blocked-worlds).
     */
    public static boolean blockedWorld(org.bukkit.World world) {
        if (cfg == null || world == null) return false;
        String name = world.getName().toLowerCase(java.util.Locale.ROOT);
        for (String w : cfg.getStringList("blocked-worlds")) {
            if (w != null && w.toLowerCase(java.util.Locale.ROOT).equals(name)) return true;
        }
        return false;
    }

    public static boolean compatSlimefun() {
        return cfg == null || cfg.getBoolean("compat.slimefun", true);
    }

    /**
     * EN: Returns true if debug logging mode is enabled.
 *
     * ES: Devuelve true si el modo de registro de depuración está activo.
     */
    public static boolean debug() {
        return cfg != null && cfg.getBoolean("debug", false);
    }

    /**
     * EN: Maximum durability uses for a freshly crafted Network Rake.
 *
     * ES: Usos máximos de durabilidad para un Network Rake recién crafteado.
     */
    public static int rakeUses() {
        return cfg != null ? Math.max(1, cfg.getInt("rake.uses", 250)) : 250;
    }
}

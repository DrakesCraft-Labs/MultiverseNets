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

    /**
     * Capacidad de una celda por nivel, con respaldo si el config no la define.
     *
     * El respaldo se calcula ANTES de tocar la lista a proposito. Con la lista vacia,
     * caps.size() - 1 vale -1 y Math.clamp(valor, 0, -1) lanza IllegalArgumentException por
     * recibir un minimo mayor que el maximo: el return de respaldo que habia debajo no llegaba
     * a ejecutarse nunca. Bastaba con que alguien borrara cells.capacities del config para que
     * cada operacion sobre una celda reventara.
     *
     * Si la lista existe pero es mas corta que los niveles declarados, se usa la ultima entrada
     * y se avisa una sola vez: es mejor una celda con capacidad de menos que una excepcion, pero
     * conviene que se sepa.
     */
    public static long cellCapacity(int tier1to6) {
        int tier = Math.max(1, tier1to6);
        long respaldo = 65536L * (1L << (tier - 1));

        List<Integer> caps = cfg.getIntegerList("cells.capacities");
        if (caps.isEmpty()) {
            return respaldo;
        }
        if (tier > caps.size()) {
            avisarNivelSinCapacidad(tier, caps.size());
            return caps.get(caps.size() - 1);
        }
        return caps.get(tier - 1);
    }

    private static final java.util.Set<Integer> NIVELES_AVISADOS = new java.util.HashSet<>();

    private static void avisarNivelSinCapacidad(int tier, int declaradas) {
        if (!NIVELES_AVISADOS.add(tier)) {
            return;
        }
        String mensaje = "[MultiverseNets] La celda de nivel " + tier
                + " no tiene capacidad en cells.capacities, que solo declara " + declaradas
                + ". Se usa la del ultimo nivel.";
        // Bukkit.getLogger() explota sin servidor arrancado, y este metodo tambien corre en las
        // pruebas. Se cae a un logger normal en vez de arrastrar el fallo hasta el test.
        if (org.bukkit.Bukkit.getServer() == null) {
            java.util.logging.Logger.getLogger("MultiverseNets").warning(mensaje);
        } else {
            org.bukkit.Bukkit.getLogger().warning(mensaje);
        }
    }

    /** Si se permite a la red hablar con maquinas de Slimefun. Ver compat.slimefun en el config. */
    public static boolean compatSlimefun() {
        return cfg == null || cfg.getBoolean("compat.slimefun", true);
    }

    public static boolean debug() {
        return cfg.getBoolean("debug", false);
    }

    /** Usos de un Network Rake recien crafteado (NetworksV6 ofrecia 250/1000/9999; aqui, uno). */
    public static int rakeUses() {
        return Math.max(1, cfg.getInt("rake.uses", 250));
    }
}

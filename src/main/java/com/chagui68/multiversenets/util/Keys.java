package com.chagui68.multiversenets.util;

import com.chagui68.multiversenets.MultiverseNets;
import org.bukkit.NamespacedKey;

public final class Keys {

    public static NamespacedKey DEVICE_TYPE;
    public static NamespacedKey WIRELESS_BIND;
    public static NamespacedKey RECEIVER_BIND;
    public static NamespacedKey BLUEPRINT_RECIPE;
    public static NamespacedKey CHUNK_HAS_NODES;
    public static NamespacedKey TERMINAL_DISPLAY;
    public static NamespacedKey CELL_CARGO;
    /** Blueprint codificado (matriz 3x3 + salida) serializado en Base64 dentro del item. */
    public static NamespacedKey BLUEPRINT_DATA;
    /** Filtro + modo copiados dentro del Configuration Wrench. */
    public static NamespacedKey CONFIG_DATA;
    /** Usos restantes del Network Rake. */
    public static NamespacedKey RAKE_USES;

    private Keys() {
    }

    public static void init(MultiverseNets plugin) {
        DEVICE_TYPE = new NamespacedKey(plugin, "device_type");
        WIRELESS_BIND = new NamespacedKey(plugin, "wireless_bind");
        RECEIVER_BIND = new NamespacedKey(plugin, "receiver_bind");
        BLUEPRINT_RECIPE = new NamespacedKey(plugin, "blueprint_recipe");
        CHUNK_HAS_NODES = new NamespacedKey(plugin, "chunk_has_nodes");
        TERMINAL_DISPLAY = new NamespacedKey(plugin, "terminal_display");
        CELL_CARGO = new NamespacedKey(plugin, "cell_cargo");
        BLUEPRINT_DATA = new NamespacedKey(plugin, "blueprint_data");
        CONFIG_DATA = new NamespacedKey(plugin, "config_data");
        RAKE_USES = new NamespacedKey(plugin, "rake_uses");
    }
}

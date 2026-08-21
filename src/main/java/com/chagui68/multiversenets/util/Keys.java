package com.chagui68.multiversenets.util;

import com.chagui68.multiversenets.MultiverseNets;
import org.bukkit.NamespacedKey;

public final class Keys {

    public static NamespacedKey DEVICE_TYPE;
    public static NamespacedKey WIRELESS_BIND;
    public static NamespacedKey RECEIVER_BIND;
    public static NamespacedKey BLUEPRINT_RECIPE;
    public static NamespacedKey CHUNK_HAS_NODES;

    private Keys() {
    }

    public static void init(MultiverseNets plugin) {
        DEVICE_TYPE = new NamespacedKey(plugin, "device_type");
        WIRELESS_BIND = new NamespacedKey(plugin, "wireless_bind");
        RECEIVER_BIND = new NamespacedKey(plugin, "receiver_bind");
        BLUEPRINT_RECIPE = new NamespacedKey(plugin, "blueprint_recipe");
        CHUNK_HAS_NODES = new NamespacedKey(plugin, "chunk_has_nodes");
    }
}

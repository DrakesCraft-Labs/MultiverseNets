package com.chagui68.multiversenets.util;

import com.chagui68.multiversenets.MultiverseNets;
import org.bukkit.NamespacedKey;

/**
 * [EN] Persistent NamespacedKeys Registry
 * Registry for all NamespacedKey identifiers used in PersistentDataContainers (PDC)
 * across chunks, items, and tile entities.
 *
 * [ES] Registro de Claves NamespacedKey
 * Registro central de todas las claves NamespacedKey empleadas en los PersistentDataContainers (PDC)
 * de chunks, ítems y bloques.
 */
public final class Keys {

    /** EN: Custom DeviceType tag / ES: Tipo de dispositivo MultiverseNets. */
    public static NamespacedKey DEVICE_TYPE;
    /** EN: Network Controller binding coordinates / ES: Coordenadas de vinculación al controlador. */
    public static NamespacedKey WIRELESS_BIND;
    /** EN: Transmitter linked receiver coordinates / ES: Coordenadas del receptor vinculado. */
    public static NamespacedKey RECEIVER_BIND;
    /** EN: Legacy recipe identifier / ES: Identificador de receta antigua. */
    public static NamespacedKey BLUEPRINT_RECIPE;
    /** EN: Chunk marker flag indicating presence of nodes / ES: Marca rápida de chunk con nodos. */
    public static NamespacedKey CHUNK_HAS_NODES;
    /** EN: Terminal search/sort display settings / ES: Ajustes de visualización y búsqueda de terminal. */
    public static NamespacedKey TERMINAL_DISPLAY;
    /** EN: Cell/Node stored cargo serialized state / ES: Estado serializado de carga de celda/nodo. */
    public static NamespacedKey CELL_CARGO;
    /** EN: Encoded recipe data serialized in Base64 / ES: Datos de receta codificados en Base64 en el Blueprint. */
    public static NamespacedKey BLUEPRINT_DATA;
    /** EN: Copied filter and direction settings in Configuration Wrench / ES: Configuración copiada en la llave inglesa. */
    public static NamespacedKey CONFIG_DATA;
    /** EN: Remaining durability uses of Network Rake / ES: Usos restantes de durabilidad del Network Rake. */
    public static NamespacedKey RAKE_USES;

    private Keys() {
    }

    /**
     * EN: Initializes all NamespacedKey instances using the plugin namespace.
 *
     * ES: Inicializa todas las instancias de NamespacedKey usando el namespace del plugin.
     *
     * @param plugin The MultiverseNets plugin instance / ES: La instancia del plugin MultiverseNets.
     */
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

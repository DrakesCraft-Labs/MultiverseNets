package com.chagui68.multiversenets.item;

import org.bukkit.Material;

/**
 * Enumeration of all MultiverseNets network devices, containers, and tools.
 *
 * Enumeración de todos los dispositivos de red, contenedores y herramientas de MultiverseNets.
 */
public enum DeviceType {

    /** Network central controller / Controlador central de la red */
    CONTROLLER(Material.LODESTONE, "Network Controller", true, -1),
    /** Network connection cable / Cable de conexión de la red */
    CABLE(Material.GLASS, "Network Cable", true, -1),
    /** Network storage terminal / Terminal de almacenamiento de la red */
    TERMINAL(Material.BEACON, "Network Terminal", true, -1),
    /** Network item count monitor / Monitor de conteo de ítems de la red */
    MONITOR(Material.RESPAWN_ANCHOR, "Network Monitor", true, -1),
    
    /*
     * Quantum Cells (Terracotta color-scaled by tier)
     * Celdas cuánticas (Terracota escalada por color según nivel)
     */
    CELL_T1(Material.TERRACOTTA, "Quantum Cell T1", true, 1),
    CELL_T2(Material.ORANGE_TERRACOTTA, "Quantum Cell T2", true, 2),
    CELL_T3(Material.YELLOW_TERRACOTTA, "Quantum Cell T3", true, 3),
    CELL_T4(Material.LIME_TERRACOTTA, "Quantum Cell T4", true, 4),
    CELL_T5(Material.CYAN_TERRACOTTA, "Quantum Cell T5", true, 5),
    CELL_T6(Material.PURPLE_TERRACOTTA, "Quantum Cell T6", true, 6),
    
    /** Greedy storage cell (keeps extracting target items) / Celda codiciosa (extrae ítems continuamente) */
    GREEDY_CELL(Material.SLIME_BLOCK, "Greedy Cell", true, -1),
    /** Infinite single-type item storage barrel / Barril de almacenamiento infinito para un solo tipo de ítem */
    INFINITY_BARREL(Material.BARREL, "Infinity Barrel", true, -1),
    
    /** Simple item importer (all sides) / Importador simple de ítems (todos los lados) */
    GRABBER(Material.OBSERVER, "Simple Grabber", true, -1),
    /** Advanced directional item importer / Importador avanzado direccional de ítems */
    GRABBER_HT(Material.STICKY_PISTON, "Advanced Grabber", true, -1),
    /** Simple item exporter (all sides) / Exportador simple de ítems (todos los lados) */
    PUSHER(Material.TARGET, "Simple Pusher", true, -1),
    /** Advanced directional item exporter / Exportador avanzado direccional de ítems */
    PUSHER_HT(Material.PISTON, "Advanced Pusher", true, -1),
    /** World item entity vacuum / Aspiradora de entidades de ítems del mundo */
    VACUUM(Material.SPONGE, "Network Vacuum", true, -1),

    /** Filtered item void/purger / Purgador y destructor de ítems filtrados */
    PURGER(Material.MAGMA_BLOCK, "Network Purger", true, -1),

    /** Diagnostic probe tool / Sonda de diagnóstico de red */
    PROBE(Material.SPYGLASS, "Network Probe", false, -1),
    /** Automated recipe crafter / Crafteador automático de recetas */
    CRAFTER(Material.CRAFTING_TABLE, "Auto-Crafter", true, -1),
    /** Recipe blueprint encoder / Codificador de planos de recetas */
    ENCODER(Material.SMITHING_TABLE, "Recipe Encoder", true, -1),
    /** Interactive network crafting grid / Mesa de crafteo integrada con la red */
    CRAFTING_GRID(Material.CARTOGRAPHY_TABLE, "Network Crafting Grid", true, -1),
    /** Item disassembly and duplication workbench / Mesa de desensamblaje cuántico */
    QUANTUM_WORKBENCH(Material.BRAIN_CORAL_BLOCK, "Quantum Workbench", true, -1),
    /** Wireless cross-network transmitter / Transmisor inalámbrico entre redes */
    TRANSMITTER(Material.CONDUIT, "Network Wireless Transmitter", true, -1),
    /** Wireless cross-network receiver / Receptor inalámbrico entre redes */
    RECEIVER(Material.REDSTONE_LAMP, "Network Wireless Receiver", true, -1),
    /** Handheld wireless terminal / Terminal inalámbrica de mano */
    WIRELESS_TERMINAL(Material.NETHER_STAR, "Wireless Terminal", false, -1),
    /** Recipe pattern blueprint / Plano de patrón de receta */
    BLUEPRINT(Material.BOOK, "Blueprint", false, -1),

    /** Direction / side configuration wrench / Llave de configuración de caras */
    CONFIGURATOR(Material.COMPARATOR, "Configuration Wrench", false, -1),
    /** Instant network pickup tool / Rastrillo de recolección instantánea de red */
    RAKE(Material.DEAD_BUSH, "Network Rake", false, -1),
    /** Visual network coloring crayon / Crayón de coloreado visual de red */
    CRAYON(Material.CYAN_DYE, "Network Crayon", false, -1);

    private final Material material;
    private final String display;
    private final boolean placeable;
    private final int cellTier;

    DeviceType(Material material, String display, boolean placeable, int cellTier) {
        this.material = material;
        this.display = display;
        this.placeable = placeable;
        this.cellTier = cellTier;
    }

    /**
     * @return Underlying Bukkit block/item material / Material Bukkit subyacente
     */
    public Material material() {
        return material;
    }

    /**
     * @return Default display name / Nombre legible por defecto
     */
    public String display() {
        return display;
    }

    /**
     * @return true if device can be placed in world as block / true si puede colocarse como bloque
     */
    public boolean placeable() {
        return placeable;
    }

    /**
     * @return true if device is a quantum cell / true si es una celda cuántica
     */
    public boolean isCell() {
        return cellTier > 0;
    }

    /**
     * @return true if device is an infinity barrel / true si es un barril infinito
     */
    public boolean isBarrel() {
        return this == INFINITY_BARREL;
    }

    /**
     * @return Cell tier (1-6) or -1 if not a cell / Nivel de celda (1-6) o -1 si no es celda
     */
    public int cellTier() {
        return cellTier;
    }

    /**
     * @return true if device supports item filter configuration / true si soporta filtros de ítems
     */
    public boolean filterable() {
        return switch (this) {
            case GRABBER, GRABBER_HT, PUSHER, PUSHER_HT, VACUUM, GREEDY_CELL, PURGER, RECEIVER -> true;
            default -> false;
        };
    }

    /**
     * @return true if device imports items into network / true si importa ítems a la red
     */
    public boolean isImporter() {
        return this == GRABBER || this == GRABBER_HT;
    }

    /**
     * @return true if device exports items from network / true si exporta ítems de la red
     */
    public boolean isExporter() {
        return this == PUSHER || this == PUSHER_HT || this == GREEDY_CELL;
    }

    /**
     * @return true if device has directional target side / true si tiene cara objetivo direccional
     */
    public boolean isDirectional() {
        return this == GRABBER_HT || this == PUSHER_HT;
    }

    /**
     * Parses a string into a DeviceType safely.
 *
     * Parsea una cadena de texto a DeviceType de forma segura.
     *
     * @param name Device enum name / Nombre de la constante enum
     * @return Matching DeviceType or null / DeviceType coincidente o null
     */
    public static DeviceType parse(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * @return Lowercase enum identifier / Identificador enum en minúsculas
     */
    public String id() {
        return name().toLowerCase();
    }
}

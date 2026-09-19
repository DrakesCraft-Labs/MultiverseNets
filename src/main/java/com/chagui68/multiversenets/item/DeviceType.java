package com.chagui68.multiversenets.item;

import org.bukkit.Material;

/**
 * Enumeration of all MultiverseNets network devices, containers, and tools.
 *
 * Enumeración de todos los dispositivos de red, contenedores y herramientas de MultiverseNets.
 */
public enum DeviceType {

    /** Network central controller / Controlador central de la red */
    MVN_CONTROLLER(Material.LODESTONE, "Network Controller", true, -1),
    /** Network connection cable / Cable de conexión de la red */
    MVN_CABLE(Material.GLASS, "Network Cable", true, -1),
    /** Network storage terminal / Terminal de almacenamiento de la red */
    MVN_TERMINAL(Material.BEACON, "Network Terminal", true, -1),
    /** Network item count monitor / Monitor de conteo de ítems de la red */
    MVN_MONITOR(Material.RESPAWN_ANCHOR, "Network Monitor", true, -1),
    
    /*
     * Quantum Cells (Terracotta color-scaled by tier)
     * Celdas cuánticas (Terracota escalada por color según nivel)
     */
    MVN_CELL_T1(Material.TERRACOTTA, "Quantum Cell T1", true, 1),
    MVN_CELL_T2(Material.ORANGE_TERRACOTTA, "Quantum Cell T2", true, 2),
    MVN_CELL_T3(Material.YELLOW_TERRACOTTA, "Quantum Cell T3", true, 3),
    MVN_CELL_T4(Material.LIME_TERRACOTTA, "Quantum Cell T4", true, 4),
    MVN_CELL_T5(Material.CYAN_TERRACOTTA, "Quantum Cell T5", true, 5),
    MVN_CELL_T6(Material.PURPLE_TERRACOTTA, "Quantum Cell T6", true, 6),
    
    /** Greedy storage cell (keeps extracting target items) / Celda codiciosa (extrae ítems continuamente) */
    MVN_GREEDY_CELL(Material.SLIME_BLOCK, "Greedy Cell", true, -1),
    /** Infinite single-type item storage barrel / Barril de almacenamiento infinito para un solo tipo de ítem */
    MVN_INFINITY_BARREL(Material.BARREL, "Infinity Barrel", true, -1),
    
    /** Simple item importer (all sides) / Importador simple de ítems (todos los lados) */
    MVN_GRABBER(Material.OBSERVER, "Simple Grabber", true, -1),
    /** Advanced directional item importer / Importador avanzado direccional de ítems */
    MVN_GRABBER_HT(Material.STICKY_PISTON, "Advanced Grabber", true, -1),
    /** Simple item exporter (all sides) / Exportador simple de ítems (todos los lados) */
    MVN_PUSHER(Material.TARGET, "Simple Pusher", true, -1),
    /** Advanced directional item exporter / Exportador avanzado direccional de ítems */
    MVN_PUSHER_HT(Material.PISTON, "Advanced Pusher", true, -1),
    /** World item entity vacuum / Aspiradora de entidades de ítems del mundo */
    MVN_VACUUM(Material.SPONGE, "Network Vacuum", true, -1),

    /** Filtered item void/purger / Purgador y destructor de ítems filtrados */
    MVN_PURGER(Material.MAGMA_BLOCK, "Network Purger", true, -1),

    /** Diagnostic probe tool / Sonda de diagnóstico de red */
    MVN_PROBE(Material.SPYGLASS, "Network Probe", false, -1),
    /** Automated recipe crafter / Crafteador automático de recetas */
    MVN_CRAFTER(Material.CRAFTING_TABLE, "Auto-Crafter", true, -1),
    /** Recipe blueprint encoder / Codificador de planos de recetas */
    MVN_ENCODER(Material.SMITHING_TABLE, "Recipe Encoder", true, -1),
    /** Interactive network crafting grid / Mesa de crafteo integrada con la red */
    MVN_CRAFTING_GRID(Material.CARTOGRAPHY_TABLE, "Network Crafting Grid", true, -1),
    /** Item disassembly and duplication workbench / Mesa de desensamblaje cuántico */
    MVN_QUANTUM_WORKBENCH(Material.BRAIN_CORAL_BLOCK, "Quantum Workbench", true, -1),
    /** Wireless cross-network transmitter / Transmisor inalámbrico entre redes */
    MVN_TRANSMITTER(Material.CONDUIT, "Network Wireless Transmitter", true, -1),
    /** Wireless cross-network receiver / Receptor inalámbrico entre redes */
    MVN_RECEIVER(Material.REDSTONE_LAMP, "Network Wireless Receiver", true, -1),
    /** Handheld wireless terminal / Terminal inalámbrica de mano */
    MVN_WIRELESS_TERMINAL(Material.NETHER_STAR, "Wireless Terminal", false, -1),
    /** Recipe pattern blueprint / Plano de patrón de receta */
    MVN_BLUEPRINT(Material.BOOK, "Blueprint", false, -1),

    /** Direction / side configuration wrench / Llave de configuración de caras */
    MVN_CONFIGURATOR(Material.COMPARATOR, "Configuration Wrench", false, -1),
    /** Instant network pickup tool / Rastrillo de recolección instantánea de red */
    MVN_RAKE(Material.DEAD_BUSH, "Network Rake", false, -1),
    /** Visual network coloring crayon / Crayón de coloreado visual de red */
    MVN_CRAYON(Material.CYAN_DYE, "Network Crayon", false, -1);

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
        return this == MVN_INFINITY_BARREL;
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
            case MVN_GRABBER, MVN_GRABBER_HT, MVN_PUSHER, MVN_PUSHER_HT, MVN_VACUUM, MVN_GREEDY_CELL, MVN_PURGER, MVN_RECEIVER -> true;
            default -> false;
        };
    }

    /**
     * @return true if device imports items into network / true si importa ítems a la red
     */
    public boolean isImporter() {
        return this == MVN_GRABBER || this == MVN_GRABBER_HT;
    }

    /**
     * @return true if device exports items from network / true si exporta ítems de la red
     */
    public boolean isExporter() {
        return this == MVN_PUSHER || this == MVN_PUSHER_HT || this == MVN_GREEDY_CELL;
    }

    /**
     * @return true if device has directional target side / true si tiene cara objetivo direccional
     */
    public boolean isDirectional() {
        return this == MVN_GRABBER_HT || this == MVN_PUSHER_HT;
    }

    /**
     * Parses a string into a DeviceType safely with bidirectional prefix support.
     * Supports both modern 'MVN_CONTROLLER' and legacy 'CONTROLLER' or 'wireless'.
     *
     * Parsea una cadena de texto a DeviceType de forma segura con soporte de prefijos bidireccional.
     *
     * @param name Device enum name / Nombre de la constante enum
     * @return Matching DeviceType or null / DeviceType coincidente o null
     */
    public static DeviceType parse(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String clean = name.trim().toUpperCase(java.util.Locale.ROOT);
        if ("WIRELESS".equals(clean)) {
            return MVN_WIRELESS_TERMINAL;
        }
        try {
            return valueOf(clean);
        } catch (IllegalArgumentException e) {
            if (!clean.startsWith("MVN_")) {
                try {
                    return valueOf("MVN_" + clean);
                } catch (IllegalArgumentException ignored) {
                }
            } else {
                try {
                    return valueOf(clean.substring(4));
                } catch (IllegalArgumentException ignored) {
                }
            }
            return null;
        }
    }

    /**
     * @return Lowercase enum identifier / Identificador enum en minúsculas
     */
    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}

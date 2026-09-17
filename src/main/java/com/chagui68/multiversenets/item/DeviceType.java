package com.chagui68.multiversenets.item;

import org.bukkit.Material;

public enum DeviceType {

    CONTROLLER(Material.LODESTONE, "Network Controller", true, -1),
    CABLE(Material.GLASS, "Network Cable", true, -1),
    TERMINAL(Material.BEACON, "Network Terminal", true, -1),
    MONITOR(Material.RESPAWN_ANCHOR, "Network Monitor", true, -1),
    /*
     * Las celdas van en terracota, escalando de color por nivel: se distinguen de un vistazo
     * entre si y no compiten con bloques luminosos que ya usan otras maquinas.
     */
    CELL_T1(Material.TERRACOTTA, "Quantum Cell T1", true, 1),
    CELL_T2(Material.ORANGE_TERRACOTTA, "Quantum Cell T2", true, 2),
    CELL_T3(Material.YELLOW_TERRACOTTA, "Quantum Cell T3", true, 3),
    CELL_T4(Material.LIME_TERRACOTTA, "Quantum Cell T4", true, 4),
    CELL_T5(Material.CYAN_TERRACOTTA, "Quantum Cell T5", true, 5),
    CELL_T6(Material.PURPLE_TERRACOTTA, "Quantum Cell T6", true, 6),
    GREEDY_CELL(Material.SLIME_BLOCK, "Greedy Cell", true, -1),
    INFINITY_BARREL(Material.BARREL, "Infinity Barrel", true, -1),
    GRABBER(Material.OBSERVER, "Network Grabber", true, -1),
    GRABBER_HT(Material.STICKY_PISTON, "Advanced Grabber", true, -1),
    PUSHER(Material.TARGET, "Network Pusher", true, -1),
    PUSHER_HT(Material.PISTON, "Advanced Pusher", true, -1),
    VACUUM(Material.SPONGE, "Network Vacuum", true, -1),

    /*
     * Purgador. Descarta de la red lo que case con su filtro.
     */
    PURGER(Material.MAGMA_BLOCK, "Network Purger", true, -1),

    /*
     * Sonda. Clic derecho a un bloque y dice de que red es y que tipo tiene.
     */
    PROBE(Material.SPYGLASS, "Network Probe", false, -1),
    CRAFTER(Material.CRAFTING_TABLE, "Auto-Crafter", true, -1),
    ENCODER(Material.SMITHING_TABLE, "Recipe Encoder", true, -1),
    CRAFTING_GRID(Material.CARTOGRAPHY_TABLE, "Network Crafting Grid", true, -1),
    QUANTUM_WORKBENCH(Material.BRAIN_CORAL_BLOCK, "Quantum Workbench", true, -1),
    TRANSMITTER(Material.CONDUIT, "Network Wireless Transmitter", true, -1),
    RECEIVER(Material.REDSTONE_LAMP, "Network Wireless Receiver", true, -1),
    WIRELESS_TERMINAL(Material.NETHER_STAR, "Wireless Terminal", false, -1),
    BLUEPRINT(Material.BOOK, "Blueprint", false, -1),
    /*
     * Herramientas de mano traidas de NetworksV6. No se colocan: existen en el inventario.
     */
    CONFIGURATOR(Material.COMPARATOR, "Configuration Wrench", false, -1),
    RAKE(Material.DEAD_BUSH, "Network Rake", false, -1),
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

    public Material material() {
        return material;
    }

    public String display() {
        return display;
    }

    public boolean placeable() {
        return placeable;
    }

    public boolean isCell() {
        return cellTier > 0;
    }

    public boolean isBarrel() {
        return this == INFINITY_BARREL;
    }

    public int cellTier() {
        return cellTier;
    }

    public boolean filterable() {
        return switch (this) {
            case GRABBER, GRABBER_HT, PUSHER, PUSHER_HT, VACUUM, GREEDY_CELL, PURGER, RECEIVER -> true;
            default -> false;
        };
    }

    public boolean isImporter() {
        return this == GRABBER || this == GRABBER_HT;
    }

    public boolean isExporter() {
        return this == PUSHER || this == PUSHER_HT || this == GREEDY_CELL;
    }

    public static DeviceType parse(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String id() {
        return name().toLowerCase();
    }
}

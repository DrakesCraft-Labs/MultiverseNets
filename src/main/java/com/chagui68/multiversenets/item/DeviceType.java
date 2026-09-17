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
    GRABBER(Material.OBSERVER, "Importer (Grabber)", true, -1),
    GRABBER_HT(Material.STICKY_PISTON, "HT Importer (Grabber)", true, -1),
    PUSHER(Material.TARGET, "Exporter (Pusher)", true, -1),
    PUSHER_HT(Material.PISTON, "HT Exporter (Pusher)", true, -1),
    VACUUM(Material.SPONGE, "Vacuum Catcher", true, -1),

    /*
     * Purgador. Descarta de la red lo que case con su filtro.
     *
     * Sin algo asi una red se atasca sola: cualquier maquina que genere un residuo --grava del
     * cuarzo, semillas de una cosechadora-- acaba llenando las celdas y bloqueando lo que si
     * interesa. Networks lo resolvia con dos items distintos, TRASH y PURGER; aqui basta uno,
     * porque el filtro ya decide que se va.
     *
     * Es bloque y no item: nacio como LAVA_BUCKET y colocarlo vertia lava de verdad.
     */
    PURGER(Material.MAGMA_BLOCK, "Network Purger", true, -1),

    /*
     * Sonda. Clic derecho a un bloque y dice de que red es y que tipo tiene.
     *
     * /mvnets doctor resume la salud de todas las redes, pero no responde la pregunta que uno se
     * hace de pie delante de una maquina que no trabaja: "esta esto conectado a algo?". Eso es lo
     * que contesta la sonda, y es la diferencia entre diagnosticar y adivinar.
     */
    PROBE(Material.SPYGLASS, "Network Probe", false, -1),
    CRAFTER(Material.CRAFTING_TABLE, "Auto-Crafter", true, -1),
    ENCODER(Material.SMITHING_TABLE, "Recipe Encoder", true, -1),
    CRAFTING_GRID(Material.CARTOGRAPHY_TABLE, "Crafting Grid", true, -1),
    TRANSMITTER(Material.CONDUIT, "Wireless Transmitter", true, -1),
    RECEIVER(Material.REDSTONE_LAMP, "Wireless Receiver", true, -1),
    WIRELESS_TERMINAL(Material.NETHER_STAR, "Wireless Terminal", false, -1),
    BLUEPRINT(Material.BOOK, "Blueprint", false, -1),
    /*
     * Herramientas de mano traidas de NetworksV6. No se colocan: existen en el inventario.
     *
     * CONFIGURATOR copia y pega configuraciones entre dispositivos con filtro.
     * RAKE desmonta nodos al instante con usos limitados (no controladores ni celdas cargadas).
     * CRAYON marca una red para que sus maquinas echen particulas al trabajar.
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

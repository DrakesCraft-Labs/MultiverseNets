package com.chagui68.multiversenets.item;

import org.bukkit.Material;

public enum DeviceType {

    CONTROLLER(Material.LODESTONE, "Network Controller", true, -1),
    CABLE(Material.LIGHT_BLUE_WOOL, "Network Cable", true, -1),
    TERMINAL(Material.BEACON, "Network Terminal", true, -1),
    MONITOR(Material.RESPAWN_ANCHOR, "Network Monitor", true, -1),
    CELL_T1(Material.GLOWSTONE, "Quantum Cell T1", true, 1),
    CELL_T2(Material.SEA_LANTERN, "Quantum Cell T2", true, 2),
    CELL_T3(Material.SHROOMLIGHT, "Quantum Cell T3", true, 3),
    CELL_T4(Material.OCHRE_FROGLIGHT, "Quantum Cell T4", true, 4),
    CELL_T5(Material.VERDANT_FROGLIGHT, "Quantum Cell T5", true, 5),
    CELL_T6(Material.PEARLESCENT_FROGLIGHT, "Quantum Cell T6", true, 6),
    GREEDY_CELL(Material.SLIME_BLOCK, "Greedy Cell", true, -1),
    GRABBER(Material.OBSERVER, "Importer (Grabber)", true, -1),
    GRABBER_HT(Material.STICKY_PISTON, "HT Importer (Grabber)", true, -1),
    PUSHER(Material.TARGET, "Exporter (Pusher)", true, -1),
    PUSHER_HT(Material.PISTON, "HT Exporter (Pusher)", true, -1),
    VACUUM(Material.SPONGE, "Vacuum Catcher", true, -1),
    CRAFTER(Material.CRAFTING_TABLE, "Auto-Crafter", true, -1),
    ENCODER(Material.SMITHING_TABLE, "Recipe Encoder", true, -1),
    CRAFTING_GRID(Material.CARTOGRAPHY_TABLE, "Crafting Grid", true, -1),
    TRANSMITTER(Material.CONDUIT, "Wireless Transmitter", true, -1),
    RECEIVER(Material.REDSTONE_LAMP, "Wireless Receiver", true, -1),
    WIRELESS_TERMINAL(Material.NETHER_STAR, "Wireless Terminal", false, -1),
    BLUEPRINT(Material.BOOK, "Blueprint", false, -1);

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
            case GRABBER, GRABBER_HT, PUSHER, PUSHER_HT, VACUUM, GREEDY_CELL -> true;
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

package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.compat.ChickenGenetics;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * [EN] Genetic Chicken Sorter menu. Up to 18 accepted products (taken from a pocket chicken on the
 * cursor or shift-clicked from the inventory) plus gene rules: tier range, minimum DNA strength,
 * pure genes only, known/unknown DNA and age. The sorter pushes matching chickens out of the
 * network or pulls them in from the block it faces.
 *
 * [ES] Menú del Genetic Chicken Sorter. Hasta 18 productos aceptados (tomados de un pollo de
 * bolsillo en el cursor o con shift-clic desde el inventario) más reglas de genes: rango de nivel,
 * fuerza de ADN mínima, solo genes puros, ADN conocido/desconocido y edad. El clasificador saca de
 * la red los pollos que cumplen o los mete desde el bloque al que mira.
 */
public class ChickenSorterMenu extends MenuHolder {

    public static final int MAX_PRODUCTS = 18;
    public static final int ACTIVE_SLOT = 27;
    public static final int MODE_SLOT = 28;
    public static final int FACE_SLOT = 29;
    public static final int MIN_TIER_SLOT = 31;
    public static final int MAX_TIER_SLOT = 32;
    public static final int STRENGTH_SLOT = 33;
    public static final int KNOWN_SLOT = 34;
    public static final int AGE_SLOT = 35;
    public static final int PURE_SLOT = 40;
    public static final int CLEAR_SLOT = 44;
    public static final int HELP_SLOT = 49;

    private static final String[] FACES = {"ALL", "NORTH", "SOUTH", "EAST", "WEST", "UP", "DOWN"};
    private static final int MAX_TIER = 9;

    private final Block block;

    public ChickenSorterMenu(MultiverseNets plugin, Player player, Block block) {
        super(plugin, player);
        this.block = block;
    }

    public void openMenu() {
        open(54, Component.text("Genetic Chicken Sorter", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
    }

    private NodeBlob blob() {
        NodeBlob blob = NodeStore.get(block);
        if (blob == null) {
            blob = NodeBlob.create(DeviceType.MVN_CHICKEN_SORTER.name());
        }
        if (blob.chickenProducts == null) {
            blob.chickenProducts = new ArrayList<>();
        }
        return blob;
    }

    @Override
    protected void draw() {
        NodeBlob blob = blob();
        ItemStack background = icon(Material.GRAY_STAINED_GLASS_PANE, " ", NamedTextColor.DARK_GRAY);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, background);
        }
        for (int i = 0; i < MAX_PRODUCTS; i++) {
            if (i < blob.chickenProducts.size()) {
                inv.setItem(i, icon(Material.EGG, ChickenGenetics.productName(blob.chickenProducts.get(i)),
                        NamedTextColor.YELLOW, "Click: remove from the list"));
            } else {
                inv.setItem(i, icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "Any product", NamedTextColor.GRAY,
                        "Click with a pocket chicken on the cursor,",
                        "or Shift-Click one in your inventory,",
                        "to accept only its product.",
                        "Empty list = every product."));
            }
        }
        inv.setItem(ACTIVE_SLOT, icon(blob.chickenActive ? Material.LIME_DYE : Material.GRAY_DYE,
                blob.chickenActive ? "Running" : "Stopped", blob.chickenActive ? NamedTextColor.GREEN : NamedTextColor.RED,
                "Click to " + (blob.chickenActive ? "stop" : "start") + " the sorter."));
        inv.setItem(MODE_SLOT, icon(blob.chickenPull ? Material.STICKY_PISTON : Material.PISTON,
                blob.chickenPull ? "Mode: Pull into network" : "Mode: Push out of network", NamedTextColor.AQUA,
                blob.chickenPull ? "Takes matching chickens from the faced block." : "Sends matching chickens to the faced block.",
                "Click to switch."));
        inv.setItem(FACE_SLOT, icon(Material.COMPASS, "Side: " + face(blob), NamedTextColor.AQUA,
                "Which neighbour the sorter works with.", "Click to cycle."));
        inv.setItem(MIN_TIER_SLOT, icon(Material.IRON_INGOT, "Min tier: " + minTier(blob), NamedTextColor.WHITE,
                "Left: +1  Right: -1", "Tier = recessive genes (special species: 7-9)."));
        inv.setItem(MAX_TIER_SLOT, icon(Material.GOLD_INGOT, "Max tier: " + (blob.chickenMaxTier == null ? "no limit" : blob.chickenMaxTier),
                NamedTextColor.WHITE, "Left: +1  Right: -1"));
        inv.setItem(STRENGTH_SLOT, icon(Material.REDSTONE, "Min DNA strength: " + blob.chickenMinStrength, NamedTextColor.WHITE,
                "Left: +1  Right: -1 (0-6)", "Strength drops with recessive and mixed genes."));
        inv.setItem(KNOWN_SLOT, icon(Material.BOOK, "DNA: " + orAny(blob.chickenKnown), NamedTextColor.WHITE,
                "ANY / KNOWN (sequenced) / UNKNOWN", "Click to cycle."));
        inv.setItem(AGE_SLOT, icon(Material.EGG, "Age: " + orAny(blob.chickenAge), NamedTextColor.WHITE,
                "ANY / ADULT / BABY", "Click to cycle."));
        inv.setItem(PURE_SLOT, icon(blob.chickenPureOnly ? Material.DIAMOND : Material.COAL,
                "Pure genes only: " + (blob.chickenPureOnly ? "ON" : "OFF"), NamedTextColor.WHITE,
                "ON: rejects chickens with any mixed (Aa) gene.", "Click to toggle."));
        inv.setItem(CLEAR_SLOT, icon(Material.BARRIER, "Clear product list", NamedTextColor.RED));
        inv.setItem(HELP_SLOT, icon(Material.KNOWLEDGE_BOOK, "How it works", NamedTextColor.GOLD,
                "Only GeneticChickengineering pocket chickens.",
                "A chicken passes when it meets EVERY rule.",
                "Push: network -> faced block (e.g. a Roost).",
                "Pull: faced block -> network.",
                "Starts stopped so it never empties a network by accident."));
    }

    @Override
    protected void click(InventoryClickEvent event) {
        int raw = event.getRawSlot();
        NodeBlob blob = blob();
        boolean right = event.isRightClick();
        if (raw >= inv.getSize()) {
            addProduct(blob, event.getCurrentItem());
            return;
        }
        if (raw < MAX_PRODUCTS) {
            if (raw < blob.chickenProducts.size()) {
                blob.chickenProducts.remove(raw);
                save(blob);
            } else {
                addProduct(blob, event.getView().getCursor());
            }
            return;
        }
        switch (raw) {
            case ACTIVE_SLOT -> blob.chickenActive = !blob.chickenActive;
            case MODE_SLOT -> blob.chickenPull = !blob.chickenPull;
            case FACE_SLOT -> blob.targetFace = nextFace(face(blob));
            case MIN_TIER_SLOT -> blob.chickenMinTier = clamp(minTier(blob) + (right ? -1 : 1), 0, MAX_TIER);
            case MAX_TIER_SLOT -> {
                int current = blob.chickenMaxTier == null ? MAX_TIER + 1 : blob.chickenMaxTier;
                int next = clamp(current + (right ? -1 : 1), 0, MAX_TIER + 1);
                blob.chickenMaxTier = next > MAX_TIER ? null : next;
            }
            case STRENGTH_SLOT -> blob.chickenMinStrength = clamp(blob.chickenMinStrength + (right ? -1 : 1), 0, 6);
            case KNOWN_SLOT -> blob.chickenKnown = cycle(blob.chickenKnown, "KNOWN", "UNKNOWN");
            case AGE_SLOT -> blob.chickenAge = cycle(blob.chickenAge, "ADULT", "BABY");
            case PURE_SLOT -> blob.chickenPureOnly = !blob.chickenPureOnly;
            case CLEAR_SLOT -> blob.chickenProducts.clear();
            default -> {
                return;
            }
        }
        save(blob);
    }

    private void addProduct(NodeBlob blob, ItemStack item) {
        ChickenGenetics.Chicken chicken = ChickenGenetics.read(item);
        if (chicken == null) {
            if (item != null && !item.getType().isAir()) {
                player.sendMessage(Text.msg("Only GeneticChickengineering pocket chickens can be added.", NamedTextColor.RED));
            }
            return;
        }
        if (blob.chickenProducts.contains(chicken.product())) {
            player.sendMessage(Text.msg("That product is already in the list.", NamedTextColor.YELLOW));
            return;
        }
        if (blob.chickenProducts.size() >= MAX_PRODUCTS) {
            player.sendMessage(Text.msg("The product list is full (" + MAX_PRODUCTS + ").", NamedTextColor.RED));
            return;
        }
        blob.chickenProducts.add(chicken.product());
        player.sendMessage(Text.msg("Accepting: " + ChickenGenetics.productName(chicken.product()), NamedTextColor.GREEN));
        save(blob);
    }

    private void save(NodeBlob blob) {
        NodeStore.put(block, blob);
        draw();
    }

    private static String face(NodeBlob blob) {
        return blob.targetFace == null ? "ALL" : blob.targetFace.toUpperCase(Locale.ROOT);
    }

    private static String nextFace(String current) {
        for (int i = 0; i < FACES.length; i++) {
            if (FACES[i].equals(current)) {
                return FACES[(i + 1) % FACES.length];
            }
        }
        return "ALL";
    }

    private static int minTier(NodeBlob blob) {
        return blob.chickenMinTier == null ? 0 : blob.chickenMinTier;
    }

    private static String orAny(String value) {
        return value == null ? "ANY" : value;
    }

    private static String cycle(String current, String first, String second) {
        if (current == null) {
            return first;
        }
        return current.equals(first) ? second : null;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static ItemStack icon(Material material, String name, NamedTextColor color, String... lore) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        List<Component> lines = new ArrayList<>();
        for (String line : lore) {
            lines.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lines);
        item.setItemMeta(meta);
        return item;
    }
}

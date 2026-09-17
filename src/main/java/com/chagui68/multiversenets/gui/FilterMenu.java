package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
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

/**
 * Menú de configuración de filtro intuitivo y amigable:
 *
 *   [Filtro 0..16] .................................................... [Modo: 17]
 *   [Fondo 18..24] ................................... [Clear: 25] [Help: 26]
 *
 *   - Huecos 0..16: casillas de filtro con placeholders claros si están vacías.
 *   - Hueco 17: botón de alternar modo Whitelist (Permitir solo) / Blacklist (Bloquear lista).
 *   - Hueco 25: botón para limpiar todos los filtros.
 *   - Hueco 26: guía explicativa.
 *   - Shift-click desde el inventario del jugador registra el ítem sin consumirlo.
 */
public class FilterMenu extends MenuHolder {

    public static final int MODE_SLOT = 17;
    public static final int CLEAR_SLOT = 25;
    public static final int HELP_SLOT = 26;
    public static final int MAX_FILTER_SLOTS = 17;

    private static final int[] BOTTOM_BORDER_SLOTS = {18, 19, 20, 21, 22, 23, 24};

    private final Block block;
    private final DeviceType type;

    public FilterMenu(MultiverseNets plugin, Player player, Block block, DeviceType type) {
        super(plugin, player);
        this.block = block;
        this.type = type;
    }

    public void openMenu() {
        open(27, Component.text(type.display() + " - Filter", NamedTextColor.DARK_AQUA)
                .decoration(TextDecoration.ITALIC, false));
    }

    private NodeBlob blob() {
        NodeBlob blob = NodeStore.get(block);
        return blob == null ? NodeBlob.create(type.name()) : blob;
    }

    @Override
    protected void draw() {
        NodeBlob blob = blob();

        // 1) Dibujar casillas de filtro (0..16)
        for (int i = 0; i < MAX_FILTER_SLOTS; i++) {
            if (i < blob.filterMaterials.size()) {
                String matName = blob.filterMaterials.get(i);
                Material mat = Material.matchMaterial(matName);
                if (mat != null && mat.isItem()) {
                    ItemStack icon = new ItemStack(mat);
                    var meta = icon.getItemMeta();
                    meta.displayName(Component.text(mat.name(), NamedTextColor.AQUA)
                            .decoration(TextDecoration.ITALIC, false));
                    meta.lore(List.of(
                            Component.empty(),
                            Component.text("Left / Right Click: Remove from filter", NamedTextColor.RED)
                                    .decoration(TextDecoration.ITALIC, false)));
                    icon.setItemMeta(meta);
                    inv.setItem(i, icon);
                    continue;
                }
            }
            // Hueco de filtro vacío
            ItemStack emptySlot = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
            var metaEmpty = emptySlot.getItemMeta();
            metaEmpty.displayName(Component.text("Empty Filter Slot", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            metaEmpty.lore(List.of(
                    Component.text("Click with item on cursor or", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Shift-Click an item from your inventory to add.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            emptySlot.setItemMeta(metaEmpty);
            inv.setItem(i, emptySlot);
        }

        // 2) Botón de Modo Whitelist / Blacklist (Slot 17)
        boolean isBlacklist = blob.filterBlacklist;
        ItemStack mode = new ItemStack(isBlacklist ? Material.RED_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE);
        var metaMode = mode.getItemMeta();
        metaMode.displayName(Component.text(isBlacklist ? "Mode: Blacklist (Block Listed)" : "Mode: Whitelist (Allow Only)",
                isBlacklist ? NamedTextColor.RED : NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        metaMode.lore(List.of(
                Component.text(isBlacklist
                        ? "All items EXCEPT those listed will be transferred."
                        : "Only items in the filter list will be transferred.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Click to toggle mode", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)));
        mode.setItemMeta(metaMode);
        inv.setItem(MODE_SLOT, mode);

        // 3) Paneles de fondo inferiores (18..24)
        ItemStack border = panel(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot : BOTTOM_BORDER_SLOTS) {
            inv.setItem(slot, border);
        }

        // 4) Botón de limpiar filtro (Slot 25)
        ItemStack clear = new ItemStack(Material.BARRIER);
        var metaClear = clear.getItemMeta();
        metaClear.displayName(Component.text("Clear Filter", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        metaClear.lore(List.of(
                Component.text("Click to remove all items from this filter.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
        clear.setItemMeta(metaClear);
        inv.setItem(CLEAR_SLOT, clear);

        // 5) Botón de ayuda (Slot 26)
        ItemStack help = new ItemStack(Material.BOOK);
        var metaHelp = help.getItemMeta();
        metaHelp.displayName(Component.text("How Filter Works", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        metaHelp.lore(List.of(
                Component.text("• Shift-Click an item in your inventory to register it.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("• Click any registered item above to remove it.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("• Toggle Whitelist / Blacklist with the mode button.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("• If filter is empty, Whitelist transfers everything.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        help.setItemMeta(metaHelp);
        inv.setItem(HELP_SLOT, help);
    }

    @Override
    protected void click(InventoryClickEvent event) {
        int raw = event.getRawSlot();
        NodeBlob blob = blob();

        // 1) Shift-click desde el inventario del jugador: añade el material al filtro sin consumirlo
        if (raw >= inv.getSize()) {
            ItemStack mover = event.getCurrentItem();
            if (mover == null || mover.getType().isAir()) {
                return;
            }
            String name = mover.getType().name();
            if (!blob.filterMaterials.contains(name)) {
                if (blob.filterMaterials.size() >= MAX_FILTER_SLOTS) {
                    player.sendMessage(Text.msg("Filter is full (max " + MAX_FILTER_SLOTS + " items).", NamedTextColor.RED));
                } else {
                    blob.filterMaterials.add(name);
                    NodeStore.put(block, blob);
                    player.sendMessage(Text.msg("Added to filter: " + name, NamedTextColor.GREEN));
                    draw();
                }
            }
            return;
        }

        // 2) Botón de Modo Whitelist / Blacklist (Slot 17)
        if (raw == MODE_SLOT) {
            blob.filterBlacklist = !blob.filterBlacklist;
            NodeStore.put(block, blob);
            player.sendMessage(Text.msg(blob.filterBlacklist
                    ? "Filter mode set to: Blacklist" : "Filter mode set to: Whitelist", NamedTextColor.GREEN));
            draw();
            return;
        }

        // 3) Botón de Limpiar Filtro (Slot 25)
        if (raw == CLEAR_SLOT) {
            blob.filterMaterials.clear();
            NodeStore.put(block, blob);
            player.sendMessage(Text.msg("Filter cleared.", NamedTextColor.YELLOW));
            draw();
            return;
        }

        // 4) Botón de Ayuda (Slot 26)
        if (raw == HELP_SLOT) {
            draw();
            return;
        }

        // 5) Clic en casillas de filtro (0..16)
        if (raw >= 0 && raw < MAX_FILTER_SLOTS) {
            if (raw < blob.filterMaterials.size()) {
                ItemStack cursor = event.getView().getCursor();
                if (cursor != null && !cursor.getType().isAir()) {
                    // Si tiene un ítem en cursor, reemplazar o registrar
                    String name = cursor.getType().name();
                    if (!blob.filterMaterials.contains(name)) {
                        blob.filterMaterials.set(raw, name);
                        NodeStore.put(block, blob);
                        player.sendMessage(Text.msg("Updated filter slot to: " + name, NamedTextColor.GREEN));
                        draw();
                    }
                } else {
                    // Sin cursor: quitar el ítem
                    String removed = blob.filterMaterials.remove(raw);
                    NodeStore.put(block, blob);
                    player.sendMessage(Text.msg("Removed from filter: " + removed, NamedTextColor.YELLOW));
                    draw();
                }
            } else {
                // Casilla vacía: si el jugador tiene un ítem en el cursor, registrarlo
                ItemStack cursor = event.getView().getCursor();
                if (cursor != null && !cursor.getType().isAir()) {
                    String name = cursor.getType().name();
                    if (!blob.filterMaterials.contains(name)) {
                        blob.filterMaterials.add(name);
                        NodeStore.put(block, blob);
                        player.sendMessage(Text.msg("Added to filter: " + name, NamedTextColor.GREEN));
                        draw();
                    }
                }
            }
        }
    }

    private ItemStack panel(Material material, String nombre) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(nombre, NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}

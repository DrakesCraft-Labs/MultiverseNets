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
 * Filtro de un dispositivo (importador, exportador, vacuum, purgador, greedy, receptor...).
 *
 * Click con item en mano sobre un hueco vacio: se añade su material. Click sobre un icono: se
 * quita. El boton de la derecha alterna whitelist/blacklist (los modos de los dispositivos
 * "Advanced" de NetworksV6, aqui disponibles en todos). Sin materiales: pasa todo.
 */
public class FilterMenu extends MenuHolder {

    private static final int HINT_SLOT = 26;
    private static final int MODE_SLOT = 17;

    private final Block block;
    private final DeviceType type;

    public FilterMenu(MultiverseNets plugin, Player player, Block block, DeviceType type) {
        super(plugin, player);
        this.block = block;
        this.type = type;
    }

    public void openMenu() {
        open(27, Component.text("Filter - " + type.display()
                        + " (" + block.getX() + "," + block.getY() + "," + block.getZ() + ")",
                NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false));
    }

    private NodeBlob blob() {
        NodeBlob blob = NodeStore.get(block);
        return blob == null ? NodeBlob.create(type.name()) : blob;
    }

    @Override
    protected void draw() {
        NodeBlob blob = blob();
        int slot = 0;
        for (String matName : blob.filterMaterials) {
            Material mat = Material.matchMaterial(matName);
            if (mat == null || slot >= 26) {
                continue;
            }
            if (slot == MODE_SLOT) {
                slot++; // ese hueco es del boton de modo
            }
            ItemStack icon = new ItemStack(mat);
            var meta = icon.getItemMeta();
            meta.displayName(Component.text(mat.name(), NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text("Click to remove", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false)));
            icon.setItemMeta(meta);
            inv.setItem(slot++, icon);
        }

        ItemStack mode = new ItemStack(blob.filterBlacklist ? Material.BLACK_DYE : Material.WHITE_DYE);
        var modeMeta = mode.getItemMeta();
        modeMeta.displayName(Component.text(
                        blob.filterBlacklist ? "Mode: Blacklist (blocks listed)" : "Mode: Whitelist (only listed)",
                        blob.filterBlacklist ? NamedTextColor.RED : NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        modeMeta.lore(List.of(Component.text("Click to toggle mode", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)));
        mode.setItemMeta(modeMeta);
        inv.setItem(MODE_SLOT, mode);

        ItemStack hint = new ItemStack(Material.PAPER);
        var meta = hint.getItemMeta();
        meta.displayName(Component.text("Filter (empty = everything)", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Click with item in hand: add material", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("Click icon without item: remove material", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        hint.setItemMeta(meta);
        inv.setItem(HINT_SLOT, hint);
    }

    @Override
    protected void click(InventoryClickEvent event) {
        // Shift sobre un stack propio: lo anade al filtro SIN moverlo de donde esta (el gesto
        // habitual en los menus de filtro de Networks).
        if (event.getClickedInventory() != inv) {
            ItemStack mover = event.getCurrentItem();
            if (mover == null || mover.getType().isAir()) {
                return;
            }
            NodeBlob blob = blob();
            String name = mover.getType().name();
            if (!blob.filterMaterials.contains(name)) {
                if (blob.filterMaterials.size() >= 25) {
                    player.sendMessage(Text.msg("Filter is full.", NamedTextColor.RED));
                } else {
                    blob.filterMaterials.add(name);
                    NodeStore.put(block, blob);
                    player.sendMessage(Text.msg("Added to filter: " + name, NamedTextColor.GREEN));
                }
            }
            refresh();
            return;
        }
        NodeBlob blob = blob();

        if (event.getRawSlot() == MODE_SLOT) {
            blob.filterBlacklist = !blob.filterBlacklist;
            NodeStore.put(block, blob);
            refresh();
            return;
        }
        if (event.getRawSlot() == HINT_SLOT) {
            refresh();
            return;
        }

        List<String> filters = new ArrayList<>(blob.filterMaterials);
        ItemStack cursor = event.getCursor();
        if (cursor != null && !cursor.getType().isAir()) {
            String name = cursor.getType().name();
            if (!filters.contains(name)) {
                if (filters.size() >= 25) {
                    player.sendMessage(Text.msg("Filter is full.", NamedTextColor.RED));
                } else {
                    filters.add(name);
                }
            }
        } else {
            ItemStack current = event.getCurrentItem();
            if (current != null && !current.getType().isAir()) {
                filters.remove(String.valueOf(current.getType().name()));
            }
        }
        blob.filterMaterials.clear();
        blob.filterMaterials.addAll(filters.stream().filter(java.util.Objects::nonNull).toList());
        NodeStore.put(block, blob);
        refresh();
    }
}

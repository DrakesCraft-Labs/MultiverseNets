package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.PosUtil;
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

public class FilterMenu extends MenuHolder {

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
            ItemStack icon = new ItemStack(mat);
            var meta = icon.getItemMeta();
            meta.displayName(Component.text(mat.name(), NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text("Click to remove", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false)));
            icon.setItemMeta(meta);
            inv.setItem(slot++, icon);
        }
        ItemStack hint = new ItemStack(Material.PAPER);
        var meta = hint.getItemMeta();
        meta.displayName(Component.text("Filter (empty = everything)", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Click with item in hand: add material", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("Click icon without item: remove material", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        hint.setItemMeta(meta);
        inv.setItem(26, hint);
    }

    @Override
    protected void click(InventoryClickEvent event) {
        if (event.getClickedInventory() != inv) {
            return;
        }
        NodeBlob blob = blob();
        List<String> filters = new ArrayList<>(blob.filterMaterials);

        ItemStack cursor = event.getCursor();
        if (raw(event) == 26) {
            refresh();
            return;
        }
        if (cursor != null && !cursor.getType().isAir()) {
            String name = cursor.getType().name();
            if (!filters.contains(name)) {
                if (filters.size() >= 26) {
                    player.sendMessage(com.chagui68.multiversenets.util.Text.msg("Filter is full.", NamedTextColor.RED));
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

    private int raw(InventoryClickEvent event) {
        return event.getRawSlot();
    }
}

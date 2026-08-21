package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
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

import java.util.List;

public class CellMenu extends MenuHolder {

    private final Block block;
    private final DeviceType type;

    public CellMenu(MultiverseNets plugin, Player player, Block block, DeviceType type) {
        super(plugin, player);
        this.block = block;
        this.type = type;
    }

    public void openMenu() {
        open(27, Component.text(type.display(), NamedTextColor.DARK_AQUA)
                .decoration(TextDecoration.ITALIC, false));
    }

    @Override
    protected void draw() {
        NodeBlob blob = NodeStore.get(block);
        long cap = Items.capacityOf(type);
        ItemStack icon;
        if (blob == null || blob.cellSample == null) {
            icon = new ItemStack(Material.BARRIER);
            var meta = icon.getItemMeta();
            meta.displayName(Component.text("Empty", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Capacity: " + Items.formatAmount(cap), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Stores a single item type", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)));
            icon.setItemMeta(meta);
        } else {
            icon = blob.cellSample.clone();
            icon.setAmount(1);
            var meta = icon.getItemMeta();
            meta.lore(List.of(
                    Component.text("Amount: " + blob.cellAmount + " (" + Items.formatAmount(blob.cellAmount) + ")",
                            NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Usage: " + (blob.cellAmount * 100 / cap) + "%", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            icon.setItemMeta(meta);
        }
        inv.setItem(13, icon);

        ItemStack eject = new ItemStack(Material.HOPPER);
        var meta = eject.getItemMeta();
        meta.displayName(Component.text("Extract to inventory", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("Up to 10 stacks per click", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)));
        eject.setItemMeta(meta);
        inv.setItem(22, eject);
    }

    @Override
    protected void click(InventoryClickEvent event) {
        if (event.getRawSlot() != 22 || block.getWorld() == null) {
            return;
        }
        NodeBlob blob = NodeStore.get(block);
        if (blob == null || blob.cellSample == null) {
            return;
        }
        int delivered = 0;
        for (int i = 0; i < 10 && blob != null && blob.cellAmount > 0; i++) {
            int want = (int) Math.min(blob.cellSample.getMaxStackSize(), blob.cellAmount);
            if (want <= 0) {
                break;
            }
            ItemStack stack = blob.cellSample.clone();
            stack.setAmount(want);
            int leftover = stack.getAmount() - addToPlayer(stack);
            delivered += stack.getAmount() - leftover;
            blob.cellAmount -= (stack.getAmount() - leftover);
            if (blob.cellAmount <= 0) {
                blob.cellAmount = 0;
                blob.cellSample = null;
            }
            NodeStore.put(block, blob);
            blob = NodeStore.get(block);
        }
        player.sendMessage(Text.msg("Extracted " + delivered + " items.", NamedTextColor.GREEN));
        refresh();
    }

    private int addToPlayer(ItemStack stack) {
        var leftover = player.getInventory().addItem(stack);
        int remaining = 0;
        for (ItemStack over : leftover.values()) {
            remaining += over.getAmount();
        }
        return stack.getAmount() - remaining;
    }
}

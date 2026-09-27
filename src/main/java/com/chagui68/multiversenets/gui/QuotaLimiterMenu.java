package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.StackUtils;
import com.chagui68.multiversenets.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Interactive GUI for configuring the Network Quota Limiter.
 * Allows setting a sample item, configuring stock ceiling limits, and toggling enforcement.
 */
public class QuotaLimiterMenu extends MenuHolder {

    public static final int ITEM_SLOT = 13;
    public static final int TOGGLE_ACTIVE_SLOT = 22;
    public static final int CUSTOM_LIMIT_SLOT = 31;

    public static final int SUB_1000_SLOT = 18;
    public static final int SUB_64_SLOT = 19;
    public static final int SUB_10_SLOT = 20;
    public static final int SUB_1_SLOT = 21;

    public static final int ADD_1_SLOT = 23;
    public static final int ADD_10_SLOT = 24;
    public static final int ADD_64_SLOT = 25;
    public static final int ADD_1000_SLOT = 26;

    private static final int[] BACKGROUND_SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            9, 10, 11, 12, 14, 15, 16, 17,
            27, 28, 29, 30, 32, 33, 34, 35
    };

    private final Block block;

    public QuotaLimiterMenu(MultiverseNets plugin, Player player, Block block) {
        super(plugin, player);
        this.block = block;
    }

    public void openMenu() {
        open(36, Component.text("Network Quota Limiter", NamedTextColor.DARK_AQUA)
                .decoration(TextDecoration.ITALIC, false));
    }

    @Override
    protected Set<Integer> vanillaSlots() {
        return Set.of();
    }

    private NodeBlob blob() {
        NodeBlob b = NodeStore.get(block);
        if (b == null) {
            b = NodeBlob.create("MVN_LIMITER");
            b.quotaActive = true;
            b.quotaLimit = 1000;
        }
        return b;
    }

    @Override
    protected void draw() {
        NodeBlob blob = blob();

        ItemStack bg = panel(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot : BACKGROUND_SLOTS) {
            inv.setItem(slot, bg);
        }

        // Target sample item slot
        inv.setItem(ITEM_SLOT, sampleIcon(blob));

        // Subtraction buttons
        inv.setItem(SUB_1000_SLOT, button(Material.RED_TERRACOTTA, "-1,000", NamedTextColor.RED));
        inv.setItem(SUB_64_SLOT, button(Material.RED_STAINED_GLASS_PANE, "-64", NamedTextColor.RED));
        inv.setItem(SUB_10_SLOT, button(Material.ORANGE_STAINED_GLASS_PANE, "-10", NamedTextColor.GOLD));
        inv.setItem(SUB_1_SLOT, button(Material.YELLOW_STAINED_GLASS_PANE, "-1", NamedTextColor.YELLOW));

        // Addition buttons
        inv.setItem(ADD_1_SLOT, button(Material.YELLOW_STAINED_GLASS_PANE, "+1", NamedTextColor.YELLOW));
        inv.setItem(ADD_10_SLOT, button(Material.LIME_STAINED_GLASS_PANE, "+10", NamedTextColor.GREEN));
        inv.setItem(ADD_64_SLOT, button(Material.GREEN_STAINED_GLASS_PANE, "+64", NamedTextColor.GREEN));
        inv.setItem(ADD_1000_SLOT, button(Material.GREEN_TERRACOTTA, "+1,000", NamedTextColor.DARK_GREEN));

        // Toggle enforcement button
        ItemStack toggle;
        if (blob.quotaActive) {
            toggle = new ItemStack(Material.LIME_DYE);
            var meta = toggle.getItemMeta();
            meta.displayName(Component.text("Status: ACTIVE (Enforcing)", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Limits total items in storage to " + Items.formatAmount(blob.quotaLimit) + ".", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Click to disable quota enforcement.", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)));
            toggle.setItemMeta(meta);
        } else {
            toggle = new ItemStack(Material.GRAY_DYE);
            var meta = toggle.getItemMeta();
            meta.displayName(Component.text("Status: DISABLED (Bypassed)", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Quota ceiling is not currently enforced.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Click to enable quota enforcement.", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)));
            toggle.setItemMeta(meta);
        }
        inv.setItem(TOGGLE_ACTIVE_SLOT, toggle);

        // Custom limit prompt button
        ItemStack customBtn = new ItemStack(Material.NAME_TAG);
        var metaCustom = customBtn.getItemMeta();
        metaCustom.displayName(Component.text("Set Custom Limit (Chat)", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        metaCustom.lore(List.of(
                Component.text("Current Quota: " + Items.formatAmount(blob.quotaLimit) + " (" + blob.quotaLimit + ")", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Click to type a precise number in chat.", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)));
        customBtn.setItemMeta(metaCustom);
        inv.setItem(CUSTOM_LIMIT_SLOT, customBtn);
    }

    private ItemStack sampleIcon(NodeBlob blob) {
        if (blob.quotaSample == null || blob.quotaSample.getType().isAir()) {
            ItemStack empty = new ItemStack(Material.ITEM_FRAME);
            var meta = empty.getItemMeta();
            meta.displayName(Component.text("No Target Item Set", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Click with an item on your cursor to set the target.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("The network will cap incoming stock for this item.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            empty.setItemMeta(meta);
            return empty;
        }

        ItemStack icon = blob.quotaSample.clone();
        icon.setAmount(1);
        var meta = icon.getItemMeta();
        List<Component> lore = new ArrayList<>();
        if (meta.hasLore() && meta.lore() != null) {
            lore.addAll(meta.lore());
        }
        lore.add(Component.empty());
        lore.add(Component.text("Quota Limit: " + Items.formatAmount(blob.quotaLimit) + " (" + blob.quotaLimit + ")", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));

        Network net = plugin.networks().networkAt(block);
        if (net != null) {
            long currentCount = net.storage().count(i -> StackUtils.itemsMatch(i, blob.quotaSample));
            long remaining = Math.max(0, blob.quotaLimit - currentCount);
            lore.add(Component.text("Current Network Stock: " + Items.formatAmount(currentCount), NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Headroom Allowed: " + Items.formatAmount(remaining), NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.empty());
        lore.add(Component.text("Left Click: Replace target item", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Right Click: Clear target item", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private ItemStack panel(Material material, String name) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack button(Material mat, String name, NamedTextColor color) {
        ItemStack item = new ItemStack(mat);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    protected void click(InventoryClickEvent event) {
        int raw = event.getRawSlot();
        NodeBlob blob = blob();

        if (raw == ITEM_SLOT) {
            if (event.getClick() == ClickType.RIGHT) {
                blob.quotaSample = null;
                NodeStore.put(block, blob);
                refresh();
                return;
            }
            ItemStack cursor = event.getCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                blob.quotaSample = StackUtils.getAsQuantity(cursor, 1);
                NodeStore.put(block, blob);
                refresh();
            }
            return;
        }

        if (raw == TOGGLE_ACTIVE_SLOT) {
            blob.quotaActive = !blob.quotaActive;
            NodeStore.put(block, blob);
            player.sendMessage(Text.msg(blob.quotaActive
                    ? "Quota Limiter: ACTIVATED" : "Quota Limiter: DEACTIVATED", NamedTextColor.YELLOW));
            refresh();
            return;
        }

        if (raw == CUSTOM_LIMIT_SLOT) {
            player.closeInventory();
            ChatPrompts.ask(player, "Enter max stock limit number (e.g. 500, 2048, 100000):", text -> {
                if (text == null || text.isBlank()) {
                    openMenu();
                    return;
                }
                try {
                    long val = Long.parseLong(text.trim().replaceAll("[,_]", ""));
                    if (val < 0) {
                        player.sendMessage(Text.msg("Limit must be 0 or positive.", NamedTextColor.RED));
                    } else {
                        NodeBlob b = blob();
                        b.quotaLimit = val;
                        NodeStore.put(block, b);
                        player.sendMessage(Text.msg("Quota limit updated to: " + Items.formatAmount(val), NamedTextColor.GREEN));
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(Text.msg("Invalid integer format.", NamedTextColor.RED));
                }
                openMenu();
            });
            return;
        }

        long delta = 0;
        if (raw == SUB_1000_SLOT) delta = -1000;
        else if (raw == SUB_64_SLOT) delta = -64;
        else if (raw == SUB_10_SLOT) delta = -10;
        else if (raw == SUB_1_SLOT) delta = -1;
        else if (raw == ADD_1_SLOT) delta = 1;
        else if (raw == ADD_10_SLOT) delta = 10;
        else if (raw == ADD_64_SLOT) delta = 64;
        else if (raw == ADD_1000_SLOT) delta = 1000;

        if (delta != 0) {
            blob.quotaLimit = Math.max(0, blob.quotaLimit + delta);
            NodeStore.put(block, blob);
            refresh();
        }
    }
}

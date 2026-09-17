package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.compat.SlimefunBridge;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.NetworkManager;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.StackUtils;
import com.chagui68.multiversenets.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration GUI for item filters (whitelist / blacklist) and directional target faces.
 *
 * Menú de configuración de filtro de ítems (lista blanca / negra) y caras objetivo direccionales.
 */
public class FilterMenu extends MenuHolder {

    public static final int MODE_SLOT = 17;
    public static final int CLEAR_SLOT = 25;
    public static final int HELP_SLOT = 26;
    public static final int MAX_FILTER_SLOTS = 17;

    public static final int ALL_DIRECTIONS_SLOT = 24;
    private static final int[] DIRECTION_SLOTS = {18, 19, 20, 21, 22, 23};
    private static final BlockFace[] DIRECTION_FACES = {
            BlockFace.DOWN, BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST
    };
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
        if (blob == null) {
            blob = NodeBlob.create(type.name());
        }
        if (blob.filterItems == null) {
            blob.filterItems = new ArrayList<>();
        }
        if (blob.filterMaterials == null) {
            blob.filterMaterials = new ArrayList<>();
        }
        if (blob.filterItems.isEmpty() && !blob.filterMaterials.isEmpty()) {
            for (String matName : blob.filterMaterials) {
                Material mat = Material.matchMaterial(matName);
                if (mat != null && mat.isItem()) {
                    blob.filterItems.add(new ItemStack(mat));
                }
            }
        }
        return blob;
    }

    @Override
    protected void draw() {
        NodeBlob blob = blob();

        for (int i = 0; i < MAX_FILTER_SLOTS; i++) {
            if (i < blob.filterItems.size()) {
                ItemStack item = blob.filterItems.get(i);
                if (item != null && !item.getType().isAir()) {
                    ItemStack icon = item.clone();
                    icon.setAmount(1);
                    var meta = icon.getItemMeta();
                    if (meta != null) {
                        List<Component> lore = meta.hasLore() && meta.lore() != null
                                ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                        lore.add(Component.empty());
                        lore.add(Component.text("Left / Right Click: Remove from filter", NamedTextColor.RED)
                                .decoration(TextDecoration.ITALIC, false));
                        meta.lore(lore);
                        icon.setItemMeta(meta);
                    }
                    inv.setItem(i, icon);
                    continue;
                }
            }
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

        if (type.isDirectional()) {
            for (int i = 0; i < DIRECTION_FACES.length; i++) {
                BlockFace f = DIRECTION_FACES[i];
                int slot = DIRECTION_SLOTS[i];
                Block adj = block.getRelative(f);
                boolean isSelected = blob.targetFace != null && blob.targetFace.equalsIgnoreCase(f.name());

                ItemStack icon;
                if (adj.getType().isItem() && !adj.getType().isAir()) {
                    icon = new ItemStack(adj.getType());
                } else {
                    icon = new ItemStack(isSelected ? Material.LIME_STAINED_GLASS_PANE : Material.LIGHT_GRAY_STAINED_GLASS_PANE);
                }
                var meta = icon.getItemMeta();
                if (meta != null) {
                    meta.displayName(Component.text(f.name() + ": " + getBlockDescription(adj),
                            isSelected ? NamedTextColor.GREEN : NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
                    List<Component> lore = new ArrayList<>();
                    lore.add(Component.text("Relative: " + f.name() + " (" + adj.getX() + ", " + adj.getY() + ", " + adj.getZ() + ")",
                            NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
                    lore.add(Component.text("Block: " + adj.getType().name(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                    if (SlimefunBridge.isMachine(adj)) {
                        String sf = SlimefunBridge.getId(adj);
                        if (sf != null) {
                            lore.add(Component.text("Machine: " + sf, NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
                        }
                    }
                    lore.add(Component.empty());
                    if (isSelected) {
                        lore.add(Component.text("● SELECTED TARGET (Only interacts with this block)", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
                        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    } else {
                        lore.add(Component.text("Click to target ONLY this face / block", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
                    }
                    meta.lore(lore);
                    icon.setItemMeta(meta);
                }
                inv.setItem(slot, icon);
            }

            boolean isAllSelected = blob.targetFace == null || blob.targetFace.equalsIgnoreCase("ALL");
            ItemStack allIcon = new ItemStack(isAllSelected ? Material.COMPASS : Material.RECOVERY_COMPASS);
            var metaAll = allIcon.getItemMeta();
            if (metaAll != null) {
                metaAll.displayName(Component.text("Target: ALL (Any Adjacent Block)",
                        isAllSelected ? NamedTextColor.GREEN : NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("Interacts with any connected container or machine.",
                        NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.empty());
                if (isAllSelected) {
                    lore.add(Component.text("● CURRENTLY ACTIVE", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
                    metaAll.addEnchant(Enchantment.UNBREAKING, 1, true);
                    metaAll.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                } else {
                    lore.add(Component.text("Click to allow interacting with all adjacent blocks.", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
                }
                metaAll.lore(lore);
                allIcon.setItemMeta(metaAll);
            }
            inv.setItem(ALL_DIRECTIONS_SLOT, allIcon);
        } else {
            ItemStack border = panel(Material.GRAY_STAINED_GLASS_PANE, " ");
            for (int slot : BOTTOM_BORDER_SLOTS) {
                inv.setItem(slot, border);
            }
        }

        ItemStack clear = new ItemStack(Material.BARRIER);
        var metaClear = clear.getItemMeta();
        metaClear.displayName(Component.text("Clear Filter", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        metaClear.lore(List.of(
                Component.text("Click to remove all items from this filter.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
        clear.setItemMeta(metaClear);
        inv.setItem(CLEAR_SLOT, clear);

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

        if (raw >= inv.getSize()) {
            ItemStack mover = event.getCurrentItem();
            if (mover == null || mover.getType().isAir()) {
                return;
            }
            addFilterItem(blob, mover);
            return;
        }

        if (raw == MODE_SLOT) {
            blob.filterBlacklist = !blob.filterBlacklist;
            NodeStore.put(block, blob);
            player.sendMessage(Text.msg(blob.filterBlacklist
                    ? "Filter mode set to: Blacklist" : "Filter mode set to: Whitelist", NamedTextColor.GREEN));
            draw();
            return;
        }

        if (type.isDirectional() && raw >= 18 && raw <= 24) {
            if (raw == ALL_DIRECTIONS_SLOT) {
                blob.targetFace = "ALL";
                NodeStore.put(block, blob);
                player.sendMessage(Text.msg("Target direction set to: ALL (Any adjacent container)", NamedTextColor.GREEN));
                draw();
                return;
            }
            for (int i = 0; i < DIRECTION_SLOTS.length; i++) {
                if (DIRECTION_SLOTS[i] == raw) {
                    BlockFace f = DIRECTION_FACES[i];
                    if (blob.targetFace != null && blob.targetFace.equalsIgnoreCase(f.name())) {
                        blob.targetFace = "ALL";
                        player.sendMessage(Text.msg("Reset target direction to: ALL", NamedTextColor.YELLOW));
                    } else {
                        blob.targetFace = f.name();
                        player.sendMessage(Text.msg("Target direction set to: " + f.name() + " (" + getBlockDescription(block.getRelative(f)) + ")", NamedTextColor.GREEN));
                    }
                    NodeStore.put(block, blob);
                    draw();
                    return;
                }
            }
        }

        if (raw == CLEAR_SLOT) {
            blob.filterItems.clear();
            blob.filterMaterials.clear();
            NodeStore.put(block, blob);
            player.sendMessage(Text.msg("Filter cleared.", NamedTextColor.YELLOW));
            draw();
            return;
        }

        if (raw == HELP_SLOT) {
            draw();
            return;
        }

        if (raw >= 0 && raw < MAX_FILTER_SLOTS) {
            ItemStack cursor = event.getView().getCursor();
            boolean hasCursor = cursor != null && !cursor.getType().isAir();

            if (raw < blob.filterItems.size()) {
                if (hasCursor) {
                    if (isAlreadyInFilter(blob, cursor)) {
                        player.sendMessage(Text.msg("This item is already registered in the filter.", NamedTextColor.YELLOW));
                    } else {
                        ItemStack template = StackUtils.getAsQuantity(cursor, 1);
                        blob.filterItems.set(raw, template);
                        if (raw < blob.filterMaterials.size()) {
                            blob.filterMaterials.set(raw, template.getType().name());
                        } else if (!blob.filterMaterials.contains(template.getType().name())) {
                            blob.filterMaterials.add(template.getType().name());
                        }
                        NodeStore.put(block, blob);
                        player.sendMessage(Text.msg("Updated filter slot to: " + getItemDisplayName(template), NamedTextColor.GREEN));
                        draw();
                    }
                } else {
                    ItemStack removed = blob.filterItems.remove(raw);
                    if (raw < blob.filterMaterials.size()) {
                        blob.filterMaterials.remove(raw);
                    }
                    NodeStore.put(block, blob);
                    player.sendMessage(Text.msg("Removed from filter: " + getItemDisplayName(removed), NamedTextColor.YELLOW));
                    draw();
                }
            } else {
                if (hasCursor) {
                    addFilterItem(blob, cursor);
                }
            }
        }
    }

    private boolean isAlreadyInFilter(NodeBlob blob, ItemStack item) {
        if (blob.filterItems == null) {
            return false;
        }
        for (ItemStack ft : blob.filterItems) {
            if (ft != null && NetworkManager.matchesFilter(ft, item)) {
                return true;
            }
        }
        return false;
    }

    private void addFilterItem(NodeBlob blob, ItemStack item) {
        if (isAlreadyInFilter(blob, item)) {
            player.sendMessage(Text.msg("This item is already registered in the filter.", NamedTextColor.YELLOW));
            return;
        }
        if (blob.filterItems.size() >= MAX_FILTER_SLOTS) {
            player.sendMessage(Text.msg("Filter is full (max " + MAX_FILTER_SLOTS + " items).", NamedTextColor.RED));
            return;
        }
        ItemStack template = StackUtils.getAsQuantity(item, 1);
        blob.filterItems.add(template);
        if (!blob.filterMaterials.contains(template.getType().name())) {
            blob.filterMaterials.add(template.getType().name());
        }
        NodeStore.put(block, blob);
        player.sendMessage(Text.msg("Added to filter: " + getItemDisplayName(template), NamedTextColor.GREEN));
        draw();
    }

    private String getBlockDescription(Block b) {
        if (b == null || b.getType().isAir()) {
            return "Air";
        }
        if (SlimefunBridge.isMachine(b)) {
            String sfId = SlimefunBridge.getId(b);
            if (sfId != null) {
                return sfId;
            }
        }
        return b.getType().name();
    }

    private String getItemDisplayName(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return "Air";
        }
        DeviceType dev = Items.typeOf(item);
        if (dev != null) {
            return dev.display();
        }
        String sfId = SlimefunBridge.getId(item);
        if (sfId != null) {
            return sfId;
        }
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().name();
    }

    private ItemStack panel(Material material, String name) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}

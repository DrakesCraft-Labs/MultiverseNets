package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.NetworkManager;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.Settings;
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
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Dedicated interactive GUI for the Greedy Cell.
 * Displays stored items, shared capacity usage, monitor stats (% used/free),
 * and controls for filters and directional distribution.
 *
 * Menú interactivo dedicado para la Greedy Cell.
 * Muestra los ítems almacenados, capacidad compartida, estadísticas del monitor (% usado/libre)
 * y controles para filtros y distribución direccional.
 */
public class GreedyMenu extends MenuHolder {

    private static final int STORAGE_SLOTS = 36; // slots 0 to 35
    private static final int PREV_PAGE_SLOT = 36;
    private static final int NEXT_PAGE_SLOT = 44;
    private static final int FILTER_SLOT = 45;
    private static final int DEPOSIT_ALL_SLOT = 46;
    private static final int MONITOR_SLOT = 49;
    private static final int DIRECTION_SLOT = 50;
    private static final int INFO_SLOT = 53;

    private static final String[] FACES_CYCLE = {"ALL", "NORTH", "EAST", "SOUTH", "WEST", "UP", "DOWN"};

    private final Block block;
    private int page = 0;

    public GreedyMenu(MultiverseNets plugin, Player player, Block block) {
        super(plugin, player);
        this.block = block;
    }

    public void openMenu() {
        open(54, Component.text("Greedy Cell Storage", NamedTextColor.DARK_GREEN)
                .decoration(TextDecoration.ITALIC, false));
    }

    @Override
    protected void draw() {
        NodeBlob blob = NodeStore.get(block);
        if (blob == null) {
            return;
        }

        long cap = Settings.greedyCapacity();
        long totalUsed = blob.totalGreedyAmount();
        long freeSpace = Math.max(0, cap - totalUsed);

        int storedTypes = blob.greedySamples != null ? blob.greedySamples.size() : 0;
        int maxPages = Math.max(1, (storedTypes + STORAGE_SLOTS - 1) / STORAGE_SLOTS);
        if (page >= maxPages) {
            page = Math.max(0, maxPages - 1);
        }

        // 1. Draw storage slots (0 to 35) for the current page
        int startIndex = page * STORAGE_SLOTS;
        for (int i = 0; i < STORAGE_SLOTS; i++) {
            int itemIndex = startIndex + i;
            if (itemIndex < storedTypes) {
                ItemStack sample = blob.greedySamples.get(itemIndex);
                Long amt = (blob.greedyAmounts != null && itemIndex < blob.greedyAmounts.size()) ? blob.greedyAmounts.get(itemIndex) : 0L;
                if (sample != null && amt != null && amt > 0) {
                    inv.setItem(i, createStoredItemIcon(sample, amt, totalUsed, cap));
                    continue;
                }
            }
            inv.setItem(i, emptySlotIcon());
        }

        // 2. Draw separator row (36 to 44) with pagination buttons
        ItemStack separator = panel(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 36; slot <= 44; slot++) {
            inv.setItem(slot, separator);
        }

        // Previous Page button on slot 36
        if (page > 0) {
            inv.setItem(PREV_PAGE_SLOT, panel(Material.RED_STAINED_GLASS_PANE, "◀ Previous Page (" + page + "/" + maxPages + ")"));
        } else {
            inv.setItem(PREV_PAGE_SLOT, panel(Material.GRAY_STAINED_GLASS_PANE, "◀ First Page (1/" + maxPages + ")"));
        }

        // Page indicator on middle slot 40
        ItemStack pageIndicator = new ItemStack(Material.PAPER);
        var pageMeta = pageIndicator.getItemMeta();
        if (pageMeta != null) {
            pageMeta.displayName(Component.text("Page " + (page + 1) + " of " + maxPages, NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false));
            pageMeta.lore(List.of(
                    Component.text("Total Item Types: " + storedTypes, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Items " + (storedTypes == 0 ? 0 : startIndex + 1) + " - " + Math.min(startIndex + STORAGE_SLOTS, storedTypes), NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
            ));
            pageIndicator.setItemMeta(pageMeta);
        }
        inv.setItem(40, pageIndicator);

        // Next Page button on slot 44
        if (page < maxPages - 1) {
            inv.setItem(NEXT_PAGE_SLOT, panel(Material.GREEN_STAINED_GLASS_PANE, "Next Page ▶ (" + (page + 2) + "/" + maxPages + ")"));
        } else {
            inv.setItem(NEXT_PAGE_SLOT, panel(Material.GRAY_STAINED_GLASS_PANE, "Last Page ▶ (" + maxPages + "/" + maxPages + ")"));
        }

        // 3. Control & Monitor row (45 to 53)
        inv.setItem(47, separator);
        inv.setItem(48, separator);
        inv.setItem(51, separator);
        inv.setItem(52, separator);

        inv.setItem(FILTER_SLOT, filterButtonIcon(blob));
        inv.setItem(DEPOSIT_ALL_SLOT, depositAllIcon());
        inv.setItem(MONITOR_SLOT, monitorIcon(blob, totalUsed, freeSpace, cap));
        inv.setItem(DIRECTION_SLOT, directionIcon(blob));
        inv.setItem(INFO_SLOT, infoIcon());
    }

    private ItemStack createStoredItemIcon(ItemStack sample, long amount, long totalUsed, long cap) {
        ItemStack icon = sample.clone();
        icon.setAmount(1);
        var meta = icon.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.hasLore() && meta.lore() != null
                    ? new ArrayList<>(meta.lore())
                    : new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("Stored: ", NamedTextColor.GRAY)
                    .append(Component.text(Items.formatAmount(amount) + " (" + amount + ")", NamedTextColor.AQUA))
                    .decoration(TextDecoration.ITALIC, false));

            double pctOfUsed = totalUsed > 0 ? (amount * 100.0 / totalUsed) : 0.0;
            lore.add(Component.text("Buffer Usage: ", NamedTextColor.GRAY)
                    .append(Component.text(String.format(Locale.US, "%.1f%%", pctOfUsed), NamedTextColor.YELLOW))
                    .decoration(TextDecoration.ITALIC, false));

            lore.add(Component.empty());
            lore.add(Component.text("▶ Left Click: Take 1 item", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("▶ Right Click: Take 64 items (1 stack)", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("▶ Shift+Click: Take to inventory", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private ItemStack emptySlotIcon() {
        ItemStack item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Available Slot", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Place an item here with cursor", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("to store it manually.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack filterButtonIcon(NodeBlob blob) {
        ItemStack item = new ItemStack(Material.HOPPER);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Configure Filters", NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
            int mats = blob.filterMaterials != null ? blob.filterMaterials.size() : 0;
            int items = blob.filterItems != null ? blob.filterItems.size() : 0;
            meta.lore(List.of(
                    Component.text("Active filters: " + (mats + items), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Mode: " + (blob.filterBlacklist ? "Blacklist" : "Whitelist"), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Click to open filter menu.", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack depositAllIcon() {
        ItemStack item = new ItemStack(Material.CHEST);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Quick Deposit", NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Click to deposit all matching items", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("from your inventory into this cell.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Click to deposit.", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack monitorIcon(NodeBlob blob, long used, long free, long cap) {
        ItemStack item = new ItemStack(Material.RESPAWN_ANCHOR);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Capacity Monitor", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));

            double usedPct = cap > 0 ? (used * 100.0 / cap) : 0.0;
            double freePct = cap > 0 ? (free * 100.0 / cap) : 100.0;

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Max Capacity: ", NamedTextColor.GRAY)
                    .append(Component.text(Items.formatAmount(cap), NamedTextColor.WHITE))
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Used Space: ", NamedTextColor.GRAY)
                    .append(Component.text(Items.formatAmount(used) + " (" + String.format(Locale.US, "%.1f%%", usedPct) + ")", NamedTextColor.YELLOW))
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Free Space: ", NamedTextColor.GRAY)
                    .append(Component.text(Items.formatAmount(free) + " (" + String.format(Locale.US, "%.1f%%", freePct) + ")", NamedTextColor.GREEN))
                    .decoration(TextDecoration.ITALIC, false));

            lore.add(Component.empty());
            lore.add(renderProgressBar(usedPct));
            lore.add(Component.empty());

            int count = blob.greedySamples != null ? blob.greedySamples.size() : 0;
            lore.add(Component.text("Item Breakdown (" + count + " types):", NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false));

            if (count == 0) {
                lore.add(Component.text(" (No items stored)", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false));
            } else {
                int limit = Math.min(count, 5);
                for (int i = 0; i < limit; i++) {
                    ItemStack sample = blob.greedySamples.get(i);
                    long amount = blob.greedyAmounts.get(i);
                    String name = sample.hasItemMeta() && sample.getItemMeta().hasDisplayName()
                            ? net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(sample.getItemMeta().displayName())
                            : sample.getType().name();
                    lore.add(Component.text(" • " + name + ": ", NamedTextColor.GRAY)
                            .append(Component.text(Items.formatAmount(amount), NamedTextColor.WHITE))
                            .decoration(TextDecoration.ITALIC, false));
                }
                if (count > limit) {
                    lore.add(Component.text(" ... and " + (count - limit) + " more types.", NamedTextColor.DARK_GRAY)
                            .decoration(TextDecoration.ITALIC, false));
                }
            }

            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private Component renderProgressBar(double pct) {
        int totalBars = 20;
        int filled = (int) Math.round((pct / 100.0) * totalBars);
        if (filled > totalBars) filled = totalBars;
        if (filled < 0) filled = 0;

        StringBuilder sbFilled = new StringBuilder();
        for (int i = 0; i < filled; i++) {
            sbFilled.append("|");
        }
        StringBuilder sbEmpty = new StringBuilder();
        for (int i = filled; i < totalBars; i++) {
            sbEmpty.append("|");
        }

        NamedTextColor fillColor = pct > 90.0 ? NamedTextColor.RED : (pct > 60.0 ? NamedTextColor.YELLOW : NamedTextColor.GREEN);

        return Component.text("[", NamedTextColor.DARK_GRAY)
                .append(Component.text(sbFilled.toString(), fillColor))
                .append(Component.text(sbEmpty.toString(), NamedTextColor.DARK_GRAY))
                .append(Component.text("] " + String.format(Locale.US, "%.1f%%", pct), fillColor))
                .decoration(TextDecoration.ITALIC, false);
    }

    private ItemStack directionIcon(NodeBlob blob) {
        ItemStack item = new ItemStack(Material.COMPASS);
        var meta = item.getItemMeta();
        if (meta != null) {
            String face = blob.targetFace != null ? blob.targetFace.toUpperCase(Locale.ROOT) : "ALL";
            meta.displayName(Component.text("Export Face: " + face, NamedTextColor.LIGHT_PURPLE)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Specifies which adjacent face receives", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("exported items automatically.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Click to cycle target face.", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack infoIcon() {
        ItemStack item = new ItemStack(Material.DISPENSER);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Distribution Info", NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("The Greedy Cell acts as a buffer:", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("1. Sucks filtered items from network.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("2. Distributes items to adjacent containers.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("3. Shared capacity across all items.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack panel(Material material, String name) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    protected void click(InventoryClickEvent event) {
        int raw = event.getRawSlot();
        NodeBlob blob = NodeStore.get(block);
        if (blob == null) {
            return;
        }

        // Handle control slots
        if (raw == FILTER_SLOT) {
            new FilterMenu(plugin, player, block, DeviceType.MVN_GREEDY_CELL).openMenu();
            return;
        }

        if (raw == DEPOSIT_ALL_SLOT) {
            depositFromPlayerInventory(blob);
            draw();
            return;
        }

        if (raw == DIRECTION_SLOT) {
            cycleDirection(blob);
            draw();
            return;
        }

        // Pagination buttons on separator row
        if (raw == PREV_PAGE_SLOT) {
            if (page > 0) {
                page--;
                draw();
            }
            return;
        }

        if (raw == NEXT_PAGE_SLOT) {
            int storedTypes = blob.greedySamples != null ? blob.greedySamples.size() : 0;
            int maxPages = Math.max(1, (storedTypes + STORAGE_SLOTS - 1) / STORAGE_SLOTS);
            if (page < maxPages - 1) {
                page++;
                draw();
            }
            return;
        }

        // Storage slots (0 to 35)
        if (raw >= 0 && raw < STORAGE_SLOTS) {
            handleStorageSlotClick(event, blob, raw);
            return;
        }

        // Bottom inventory (player clicked their own inventory)
        if (raw >= event.getView().getTopInventory().getSize()) {
            if (event.isShiftClick()) {
                ItemStack item = event.getCurrentItem();
                if (item != null && !item.getType().isAir()) {
                    long cap = Settings.greedyCapacity();
                    long space = Math.max(0, cap - blob.totalGreedyAmount());
                    if (space > 0 && allowsItem(blob, item)) {
                        long take = Math.min(space, (long) item.getAmount());
                        blob.addGreedyItem(item, take);
                        int remaining = (int) (item.getAmount() - take);
                        if (remaining <= 0) {
                            event.setCurrentItem(null);
                        } else {
                            item.setAmount(remaining);
                            event.setCurrentItem(item);
                        }
                        NodeStore.put(block, blob);
                        draw();
                        player.updateInventory();
                    }
                }
            }
        }
    }

    private void handleStorageSlotClick(InventoryClickEvent event, NodeBlob blob, int slotIndex) {
        var view = event.getView();
        ItemStack cursor = view.getCursor();
        ClickType click = event.getClick();

        int storedTypes = blob.greedySamples != null ? blob.greedySamples.size() : 0;
        int itemIndex = page * STORAGE_SLOTS + slotIndex;
        boolean hasItemInSlot = itemIndex < storedTypes;

        // Player is holding an item on cursor: try deposit
        if (cursor != null && !cursor.getType().isAir()) {
            long cap = Settings.greedyCapacity();
            long space = Math.max(0, cap - blob.totalGreedyAmount());
            if (space <= 0) {
                player.sendMessage(Text.msg("The Greedy Cell is full (maximum capacity reached).", NamedTextColor.RED));
                return;
            }
            if (!allowsItem(blob, cursor)) {
                player.sendMessage(Text.msg("This item is not allowed by the Greedy Cell filter.", NamedTextColor.RED));
                return;
            }
            long take = Math.min(space, (long) cursor.getAmount());
            blob.addGreedyItem(cursor, take);
            int rem = (int) (cursor.getAmount() - take);
            if (rem <= 0) {
                view.setCursor(null);
            } else {
                cursor.setAmount(rem);
                view.setCursor(cursor);
            }
            NodeStore.put(block, blob);
            draw();
            player.updateInventory();
            return;
        }

        // Slot has an item and cursor is empty: withdraw
        if (hasItemInSlot) {
            ItemStack sample = blob.greedySamples.get(itemIndex);
            long amount = blob.greedyAmounts.get(itemIndex);
            if (sample == null || amount <= 0) {
                return;
            }

            boolean shift = click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT;
            if (shift) {
                int want = Math.min(sample.getMaxStackSize(), 64);
                long take = Math.min(amount, (long) want);
                blob.removeGreedyItem(itemIndex, take);
                ItemStack withdrawn = sample.clone();
                withdrawn.setAmount((int) take);
                int leftover = NetworkManager.insertInto(player.getInventory(), withdrawn);
                if (leftover > 0) {
                    blob.addGreedyItem(sample, leftover);
                }
                NodeStore.put(block, blob);
                draw();
                player.updateInventory();
                return;
            }

            int want = (click == ClickType.RIGHT) ? Math.min(sample.getMaxStackSize(), 64) : 1;
            long take = Math.min(amount, (long) want);
            blob.removeGreedyItem(itemIndex, take);
            ItemStack withdrawn = sample.clone();
            withdrawn.setAmount((int) take);
            view.setCursor(withdrawn);
            NodeStore.put(block, blob);
            draw();
            player.updateInventory();
        }
    }

    private void depositFromPlayerInventory(NodeBlob blob) {
        long cap = Settings.greedyCapacity();
        long space = Math.max(0, cap - blob.totalGreedyAmount());
        if (space <= 0) {
            player.sendMessage(Text.msg("The Greedy Cell is full.", NamedTextColor.RED));
            return;
        }
        long depositedTotal = 0;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || item.getType().isAir()) {
                continue;
            }
            if (allowsItem(blob, item)) {
                long take = Math.min(space, (long) item.getAmount());
                if (take > 0) {
                    blob.addGreedyItem(item, take);
                    space -= take;
                    depositedTotal += take;
                    int rem = (int) (item.getAmount() - take);
                    if (rem <= 0) {
                        player.getInventory().setItem(i, null);
                    } else {
                        item.setAmount(rem);
                    }
                    if (space <= 0) {
                        break;
                    }
                }
            }
        }
        if (depositedTotal > 0) {
            NodeStore.put(block, blob);
            player.sendMessage(Text.msg("Deposited " + Items.formatAmount(depositedTotal) + " items into the Greedy Cell.", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Text.msg("No matching items found in your inventory.", NamedTextColor.YELLOW));
        }
    }

    private void cycleDirection(NodeBlob blob) {
        String current = blob.targetFace != null ? blob.targetFace.toUpperCase(Locale.ROOT) : "ALL";
        int idx = 0;
        for (int i = 0; i < FACES_CYCLE.length; i++) {
            if (FACES_CYCLE[i].equals(current)) {
                idx = i;
                break;
            }
        }
        int next = (idx + 1) % FACES_CYCLE.length;
        blob.targetFace = FACES_CYCLE[next];
        NodeStore.put(block, blob);
    }

    private boolean allowsItem(NodeBlob blob, ItemStack item) {
        if (blob.indexOfGreedySample(item) >= 0) {
            return true;
        }
        boolean hasFilter = (blob.filterMaterials != null && !blob.filterMaterials.isEmpty())
                || (blob.filterItems != null && !blob.filterItems.isEmpty());
        if (!hasFilter) {
            return true; // No filter configured: allow any item
        }
        Predicate<ItemStack> pred = NetworkManager.filterPredicate(blob);
        return pred.test(item);
    }
}

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
    private static final int FILTER_SLOT = 45;
    private static final int DEPOSIT_ALL_SLOT = 46;
    private static final int MONITOR_SLOT = 49;
    private static final int DIRECTION_SLOT = 50;
    private static final int INFO_SLOT = 53;

    private static final String[] FACES_CYCLE = {"ALL", "NORTH", "EAST", "SOUTH", "WEST", "UP", "DOWN"};

    private final Block block;

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

        // 1. Draw storage slots (0 to 35)
        int storedTypes = blob.greedySamples != null ? blob.greedySamples.size() : 0;
        for (int i = 0; i < STORAGE_SLOTS; i++) {
            if (i < storedTypes) {
                ItemStack sample = blob.greedySamples.get(i);
                Long amt = (blob.greedyAmounts != null && i < blob.greedyAmounts.size()) ? blob.greedyAmounts.get(i) : 0L;
                if (sample != null && amt != null && amt > 0) {
                    inv.setItem(i, createStoredItemIcon(sample, amt, totalUsed, cap));
                    continue;
                }
            }
            inv.setItem(i, emptySlotIcon());
        }

        // 2. Draw separator row (36 to 44)
        ItemStack separator = panel(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 36; slot <= 44; slot++) {
            inv.setItem(slot, separator);
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
            lore.add(Component.text("Almacenado: ", NamedTextColor.GRAY)
                    .append(Component.text(Items.formatAmount(amount) + " (" + amount + ")", NamedTextColor.AQUA))
                    .decoration(TextDecoration.ITALIC, false));

            double pctOfUsed = totalUsed > 0 ? (amount * 100.0 / totalUsed) : 0.0;
            lore.add(Component.text("Ocupación del buffer: ", NamedTextColor.GRAY)
                    .append(Component.text(String.format(Locale.US, "%.1f%%", pctOfUsed), NamedTextColor.YELLOW))
                    .decoration(TextDecoration.ITALIC, false));

            lore.add(Component.empty());
            lore.add(Component.text("▶ Clic Izquierdo: Extraer 1", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("▶ Clic Derecho: Extraer 64 (1 stack)", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("▶ Shift+Clic: Extraer a inventario", NamedTextColor.DARK_GRAY)
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
            meta.displayName(Component.text("Ranura Disponible", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Coloca un ítem aquí con el cursor", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("para almacenarlo manualmente.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack filterButtonIcon(NodeBlob blob) {
        ItemStack item = new ItemStack(Material.HOPPER);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Configurar Filtros", NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
            int mats = blob.filterMaterials != null ? blob.filterMaterials.size() : 0;
            int items = blob.filterItems != null ? blob.filterItems.size() : 0;
            meta.lore(List.of(
                    Component.text("Filtros activos: " + (mats + items), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Modo: " + (blob.filterBlacklist ? "Lista Negra" : "Lista Blanca"), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Clic para abrir menú de filtros.", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack depositAllIcon() {
        ItemStack item = new ItemStack(Material.CHEST);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Depósito Rápido", NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Deposita todos los ítems válidos", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("de tu inventario en esta Greedy Cell.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Clic para depositar.", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack monitorIcon(NodeBlob blob, long used, long free, long cap) {
        ItemStack item = new ItemStack(Material.RESPAWN_ANCHOR);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Monitor de Capacidad", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));

            double usedPct = cap > 0 ? (used * 100.0 / cap) : 0.0;
            double freePct = cap > 0 ? (free * 100.0 / cap) : 100.0;

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Capacidad Máxima: ", NamedTextColor.GRAY)
                    .append(Component.text(Items.formatAmount(cap), NamedTextColor.WHITE))
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Espacio Usado: ", NamedTextColor.GRAY)
                    .append(Component.text(Items.formatAmount(used) + " (" + String.format(Locale.US, "%.1f%%", usedPct) + ")", NamedTextColor.YELLOW))
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Espacio Libre: ", NamedTextColor.GRAY)
                    .append(Component.text(Items.formatAmount(free) + " (" + String.format(Locale.US, "%.1f%%", freePct) + ")", NamedTextColor.GREEN))
                    .decoration(TextDecoration.ITALIC, false));

            lore.add(Component.empty());
            lore.add(renderProgressBar(usedPct));
            lore.add(Component.empty());

            int count = blob.greedySamples != null ? blob.greedySamples.size() : 0;
            lore.add(Component.text("Desglose por ítems (" + count + " tipos):", NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false));

            if (count == 0) {
                lore.add(Component.text(" (Sin ítems almacenados)", NamedTextColor.DARK_GRAY)
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
                    lore.add(Component.text(" ... y " + (count - limit) + " tipos más.", NamedTextColor.DARK_GRAY)
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
            meta.displayName(Component.text("Dirección de Salida: " + face, NamedTextColor.LIGHT_PURPLE)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Determina a qué cara adyacente reparte", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("ítems automáticamente.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Clic para alternar dirección.", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack infoIcon() {
        ItemStack item = new ItemStack(Material.DISPENSER);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Información de Distribución", NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("La Greedy Cell funciona como búfer:", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("1. Succiona ítems de la red según sus filtros.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("2. Distribuye ítems a contenedores vecinos.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("3. La capacidad es compartida entre todos los ítems.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
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
            new FilterMenu(plugin, player, block, DeviceType.GREEDY_CELL).openMenu();
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
        boolean hasItemInSlot = slotIndex < storedTypes;

        // Player is holding an item on cursor: try deposit
        if (cursor != null && !cursor.getType().isAir()) {
            long cap = Settings.greedyCapacity();
            long space = Math.max(0, cap - blob.totalGreedyAmount());
            if (space <= 0) {
                player.sendMessage(Text.msg("La celda Greedy está llena (capacidad máxima alcanzada).", NamedTextColor.RED));
                return;
            }
            if (!allowsItem(blob, cursor)) {
                player.sendMessage(Text.msg("Este ítem no está permitido por el filtro de la Greedy Cell.", NamedTextColor.RED));
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
            ItemStack sample = blob.greedySamples.get(slotIndex);
            long amount = blob.greedyAmounts.get(slotIndex);
            if (sample == null || amount <= 0) {
                return;
            }

            boolean shift = click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT;
            if (shift) {
                int want = Math.min(sample.getMaxStackSize(), 64);
                long take = Math.min(amount, (long) want);
                blob.removeGreedyItem(slotIndex, take);
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
            blob.removeGreedyItem(slotIndex, take);
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
            player.sendMessage(Text.msg("La Greedy Cell está llena.", NamedTextColor.RED));
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
            player.sendMessage(Text.msg("Depositados " + Items.formatAmount(depositedTotal) + " ítems en la Greedy Cell.", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Text.msg("No se encontraron ítems compatibles en tu inventario.", NamedTextColor.YELLOW));
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

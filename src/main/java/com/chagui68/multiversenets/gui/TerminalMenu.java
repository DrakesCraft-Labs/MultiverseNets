package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.net.NetworkManager;
import com.chagui68.multiversenets.net.NetworkStorage;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.Keys;
import com.chagui68.multiversenets.util.StackUtils;
import com.chagui68.multiversenets.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Main interactive digital storage terminal GUI for MultiverseNets networks.
 *
 * Menú principal interactivo de la terminal de almacenamiento digital para redes MultiverseNets.
 */
public class TerminalMenu extends MenuHolder {

    private static final int PAGE_SIZE = 48;
    private static final int INPUT_SLOT = 8;
    private static final int PURGER_TOGGLE_SLOT = 17;
    private static final int SORT_SLOT = 26;
    private static final int FLUIDS_TOGGLE_SLOT = 35; // 3rd Button: Network Fluids Storage
    private static final int PREV_SLOT = 44;
    private static final int NEXT_SLOT = 53;

    private static final int[] DISPLAY_SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7,
            9, 10, 11, 12, 13, 14, 15, 16,
            18, 19, 20, 21, 22, 23, 24, 25,
            27, 28, 29, 30, 31, 32, 33, 34,
            36, 37, 38, 39, 40, 41, 42, 43,
            45, 46, 47, 48, 49, 50, 51, 52
    };

    private static final String AMOUNT_PREFIX = "Amount: ";

    private enum SortOrder {MVN_ALPHABETIC, MVN_AMOUNT}

    private final Network network;
    private final ItemStack[] displayedSamples = new ItemStack[54];
    private final java.util.Map<Integer, String> displayedFluids = new java.util.HashMap<>();
    /** Grid slot → recovered memory module shown there (temporary items from an old Controller). */
    private final java.util.Map<Integer, ItemStack> displayedRecovered = new java.util.HashMap<>();
    private int page = 0;
    private String query = "";
    private SortOrder sortOrder = SortOrder.MVN_ALPHABETIC;
    private boolean showOnlyPurged = false;
    private boolean showFluids = false;
    private BukkitTask tickTask;

    public TerminalMenu(MultiverseNets plugin, Player player, Network network) {
        super(plugin, player);
        this.network = network;
    }

    public void openMenu() {
        cancelTask();
        open(54, Component.text("Network Terminal", NamedTextColor.DARK_AQUA)
                .decoration(TextDecoration.ITALIC, false));
        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::liveTick, 10L, 10L);
    }

    @Override
    protected java.util.Set<Integer> vanillaSlots() {
        return java.util.Set.of(INPUT_SLOT);
    }

    @Override
    protected void draw() {
        ItemStack background = panel(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ");
        inv.setItem(PURGER_TOGGLE_SLOT, purgerToggleIcon());
        inv.setItem(SORT_SLOT, sortAndSearchIcon());
        inv.setItem(FLUIDS_TOGGLE_SLOT, fluidsToggleIcon());
        inv.setItem(PREV_SLOT, panel(Material.RED_STAINED_GLASS_PANE, "Previous Page"));
        inv.setItem(NEXT_SLOT, panel(Material.RED_STAINED_GLASS_PANE, "Next Page"));

        java.util.Arrays.fill(displayedSamples, null);
        displayedFluids.clear();
        displayedRecovered.clear();

        if (showFluids) {
            drawFluids(background);
        } else {
            drawItems(background);
        }
    }

    private void drawItems(ItemStack background) {
        List<NetworkStorage.View> list = filteredItems();
        // Los modulos recuperados de un Controlador antiguo van primero, aparte del stock.
        List<ItemStack> recovered = showOnlyPurged ? List.of() : recoveredModules();
        int total = recovered.size() + list.size();
        int pages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page >= pages) {
            page = pages - 1;
        }
        int start = page * PAGE_SIZE;
        for (int i = 0; i < DISPLAY_SLOTS.length; i++) {
            int slot = DISPLAY_SLOTS[i];
            int index = start + i;
            if (index < recovered.size()) {
                displayedRecovered.put(slot, recovered.get(index));
                inv.setItem(slot, recoveredIcon(recovered.get(index)));
            } else if (index - recovered.size() < list.size()) {
                NetworkStorage.View view = list.get(index - recovered.size());
                displayedSamples[slot] = view.sample();
                inv.setItem(slot, gridIcon(view));
            } else {
                inv.setItem(slot, background);
            }
        }
    }

    private void drawFluids(ItemStack background) {
        java.util.Map<String, Long> allFluids = new java.util.LinkedHashMap<>(network.fluidStorage().getFluids());
        List<java.util.Map.Entry<String, Long>> list = new ArrayList<>();
        for (var entry : allFluids.entrySet()) {
            if (entry.getValue() != null && entry.getValue() > 0) {
                list.add(entry);
            }
        }

        if (list.isEmpty()) {
            for (int slot : DISPLAY_SLOTS) {
                inv.setItem(slot, background);
            }
            inv.setItem(DISPLAY_SLOTS[22], panel(Material.GRAY_STAINED_GLASS_PANE, "No liquids currently stored"));
            return;
        }

        int pages = Math.max(1, (list.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page >= pages) {
            page = pages - 1;
        }
        int start = page * PAGE_SIZE;
        for (int i = 0; i < DISPLAY_SLOTS.length; i++) {
            int slot = DISPLAY_SLOTS[i];
            int index = start + i;
            if (index < list.size()) {
                var entry = list.get(index);
                displayedFluids.put(slot, entry.getKey());
                inv.setItem(slot, fluidGridIcon(entry.getKey(), entry.getValue()));
            } else {
                inv.setItem(slot, background);
            }
        }
    }

    private List<ItemStack> recoveredModules() {
        NodeBlob ctrl = NodeStore.get(network.block(network.controllerPos()));
        if (ctrl == null || ctrl.recoveredModules == null) {
            return List.of();
        }
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack module : ctrl.recoveredModules) {
            if (module != null && !module.getType().isAir()) {
                out.add(module);
            }
        }
        return out;
    }

    private ItemStack recoveredIcon(ItemStack module) {
        ItemStack icon = module.clone();
        var meta = icon.getItemMeta();
        List<Component> lore = meta.hasLore() && meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("⏳ TEMPORARY ITEM", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("This module was inside the Controller.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Its items are kept inside the module.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Click to take it, then install it", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("in a DRAM Bay.", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    /**
     * EN: Hands a recovered module to the player (cursor if empty, else inventory). It is looked up
     * again in the Controller's list, so two viewers can never take the same module.
     * ES: Entrega un módulo recuperado al jugador (al cursor si está vacío, si no al inventario).
     * Se busca de nuevo en la lista del Controlador, así dos jugadores nunca se llevan el mismo.
     */
    private void takeRecovered(InventoryClickEvent event, ItemStack shown) {
        org.bukkit.block.Block ctrlBlock = network.block(network.controllerPos());
        NodeBlob ctrl = NodeStore.get(ctrlBlock);
        if (ctrl == null || ctrl.recoveredModules == null) {
            draw();
            return;
        }
        int index = -1;
        for (int i = 0; i < ctrl.recoveredModules.size(); i++) {
            ItemStack candidate = ctrl.recoveredModules.get(i);
            if (candidate != null && candidate.equals(shown)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            draw();
            return;
        }
        ItemStack module = ctrl.recoveredModules.remove(index);
        NodeStore.put(ctrlBlock, ctrl);
        ItemStack cursor = event.getView().getCursor();
        if (cursor == null || cursor.getType().isAir()) {
            event.getView().setCursor(module);
        } else {
            giveOrDrop(module);
        }
        player.sendMessage(Text.msg("Module taken. Install it in a DRAM Bay to bring its items back.", NamedTextColor.GREEN));
        draw();
    }

    private List<NetworkStorage.View> filteredItems() {
        List<NetworkStorage.View> all = showOnlyPurged
                ? network.storage().getPurgedItemsView()
                : network.storage().view();
        Comparator<NetworkStorage.View> comparator = sortOrder == SortOrder.MVN_AMOUNT
                ? Comparator.comparingLong(NetworkStorage.View::amount).reversed()
                : Comparator.comparing(v -> readableName(v.sample()));
        List<NetworkStorage.View> out = new ArrayList<>(all);
        out.sort(comparator);
        if (query.isBlank()) {
            return out;
        }
        String q = query.toLowerCase();
        out.removeIf(v -> !matchesSearch(v, q));
        return out;
    }

    private boolean matchesSearch(NetworkStorage.View view, String q) {
        ItemStack item = view.sample();
        long amount = view.amount();
        if (q.startsWith(">=") || q.startsWith("<=") || q.startsWith(">") || q.startsWith("<") || q.startsWith("=")) {
            try {
                if (q.startsWith(">=")) {
                    long target = Long.parseLong(q.substring(2).trim());
                    return amount >= target;
                } else if (q.startsWith("<=")) {
                    long target = Long.parseLong(q.substring(2).trim());
                    return amount <= target;
                } else if (q.startsWith(">")) {
                    long target = Long.parseLong(q.substring(1).trim());
                    return amount > target;
                } else if (q.startsWith("<")) {
                    long target = Long.parseLong(q.substring(1).trim());
                    return amount < target;
                } else if (q.startsWith("=")) {
                    long target = Long.parseLong(q.substring(1).trim());
                    return amount == target;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        if (q.startsWith("@")) {
            String filter = q.substring(1).trim();
            if (filter.isEmpty()) return true;
            String typeName = item.getType().name().toLowerCase();
            if (typeName.contains(filter)) return true;
            if (item.hasItemMeta()) {
                var pdc = item.getItemMeta().getPersistentDataContainer();
                for (org.bukkit.NamespacedKey key : pdc.getKeys()) {
                    if (key.getNamespace().toLowerCase().contains(filter) || key.getKey().toLowerCase().contains(filter)) {
                        return true;
                    }
                }
            }
            return false;
        }
        if (q.startsWith("#")) {
            String filter = q.substring(1).trim();
            if (filter.isEmpty()) return true;
            if (item.hasItemMeta() && item.getItemMeta().hasLore() && item.getItemMeta().lore() != null) {
                for (Component line : item.getItemMeta().lore()) {
                    String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(line);
                    if (plain.toLowerCase().contains(filter)) {
                        return true;
                    }
                }
            }
            return false;
        }
        if (readableName(item).toLowerCase().contains(q)) {
            return true;
        }
        if (item.getType().name().toLowerCase().contains(q)) {
            return true;
        }
        if (item.hasItemMeta()) {
            var meta = item.getItemMeta();
            if (meta.hasLore() && meta.lore() != null) {
                for (Component line : meta.lore()) {
                    String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(line);
                    if (plain.toLowerCase().contains(q)) {
                        return true;
                    }
                }
            }
            var pdc = meta.getPersistentDataContainer();
            for (org.bukkit.NamespacedKey key : pdc.getKeys()) {
                if (key.getKey().toLowerCase().contains(q) || key.toString().toLowerCase().contains(q)) {
                    return true;
                }
                try {
                    String val = pdc.get(key, PersistentDataType.STRING);
                    if (val != null && val.toLowerCase().contains(q)) {
                        return true;
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return false;
    }

    private String readableName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(item.getItemMeta().displayName());
        }
        return item.getType().name();
    }

    private ItemStack gridIcon(NetworkStorage.View view) {
        ItemStack icon = view.sample().clone();
        icon.setAmount(1);
        var meta = icon.getItemMeta();
        List<Component> lore = meta != null && meta.hasLore() && meta.lore() != null
                ? new ArrayList<>(meta.lore())
                : new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(AMOUNT_PREFIX + Items.formatAmount(view.amount()), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));

        if (!showOnlyPurged) {
            long greedyAmt = network.storage().getGreedyStoredAmount(view.sample());
            if (greedyAmt > 0) {
                lore.add(Component.text("⚡ In Greedy Buffer: " + Items.formatAmount(greedyAmt), NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false));
            }
            long memoryAmt = network.storage().getMemoryStoredAmount(view.sample());
            if (memoryAmt > 0) {
                lore.add(Component.text("▣ In DRAM: " + Items.formatAmount(memoryAmt), NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false));
            }
        } else {
            lore.add(Component.text("⚠ Targeted by Purger", NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
        }

        if (meta != null) {
            meta.lore(lore);
            meta.getPersistentDataContainer().set(Keys.TERMINAL_DISPLAY, PersistentDataType.BYTE, (byte) 1);
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private ItemStack purgerToggleIcon() {
        if (!showOnlyPurged) {
            ItemStack item = new ItemStack(Material.MAGMA_BLOCK);
            var meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("Network Status: Normal Storage", NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false));
                meta.lore(List.of(
                        Component.text("Active purgers in network: " + network.storage().countActivePurgers(), NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false),
                        Component.text("Active greedy cells: " + network.storage().countActiveGreedyCells(), NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false),
                        Component.empty(),
                        Component.text("▶ Click: View Purged Items", NamedTextColor.GOLD)
                                .decoration(TextDecoration.ITALIC, false)
                ));
                item.setItemMeta(meta);
            }
            return item;
        } else {
            ItemStack item = new ItemStack(Material.LAVA_BUCKET);
            var meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("Network Status: Purge Mode", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false));
                meta.lore(List.of(
                        Component.text("Showing items targeted for", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false),
                        Component.text("destruction by active purgers.", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false),
                        Component.empty(),
                        Component.text("◀ Click: Return to Normal Storage", NamedTextColor.GREEN)
                                .decoration(TextDecoration.ITALIC, false)
                ));
                item.setItemMeta(meta);
            }
            return item;
        }
    }

    private ItemStack panel(Material material, String name) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack sortAndSearchIcon() {
        ItemStack item = new ItemStack(query.isBlank() ? Material.COMPARATOR : Material.NAME_TAG);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Sort & Search: " + (sortOrder == SortOrder.MVN_ALPHABETIC ? "A-Z" : "Amount")
                    + (query.isBlank() ? "" : " [" + query + "]"), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Left-Click: Cycle Sort Order (" + (sortOrder == SortOrder.MVN_ALPHABETIC ? "Amount" : "A-Z") + ")", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
                    Component.text("Right-Click: Set text search query", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false),
                    Component.text("Shift+Right Click: Clear search filter", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack fluidsToggleIcon() {
        ItemStack item = new ItemStack(showFluids ? Material.BUCKET : Material.WATER_BUCKET);
        var meta = item.getItemMeta();
        if (meta != null) {
            long totalMb = network.fluidStorage().getTotalAmountMb();
            int types = network.fluidStorage().getFluids().size();
            meta.displayName(Component.text(showFluids ? "Network Fluids Storage [ACTIVE]" : "Network Fluids Storage",
                    showFluids ? NamedTextColor.GREEN : NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text(showFluids
                            ? "Currently viewing all liquids stored in the network."
                            : "Click to view liquids stored in the network.", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("Stored liquids: " + types + " types", NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("Total volume: " + Items.formatAmount(totalMb) + " mB (" + (totalMb / 1000) + " Buckets)", NamedTextColor.AQUA)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text(showFluids ? "◀ Click to return to Items view" : "▶ Click to switch to Fluids view",
                            showFluids ? NamedTextColor.RED : NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("▪ MultiverseNets", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack fluidGridIcon(String fluidType, long amountMb) {
        Material displayMat = switch (fluidType.toUpperCase(java.util.Locale.ROOT)) {
            case "LAVA" -> Material.LAVA_BUCKET;
            case "MILK" -> Material.MILK_BUCKET;
            case "POWDER_SNOW" -> Material.POWDER_SNOW_BUCKET;
            case "HONEY" -> Material.HONEY_BOTTLE;
            default -> Material.WATER_BUCKET;
        };
        ItemStack item = new ItemStack(displayMat);
        var meta = item.getItemMeta();
        if (meta != null) {
            NamedTextColor col = switch (fluidType.toUpperCase(java.util.Locale.ROOT)) {
                case "LAVA" -> NamedTextColor.GOLD;
                case "MILK" -> NamedTextColor.WHITE;
                case "POWDER_SNOW" -> NamedTextColor.AQUA;
                case "HONEY" -> NamedTextColor.YELLOW;
                default -> NamedTextColor.DARK_AQUA;
            };
            meta.displayName(Component.text(fluidType, col).decoration(TextDecoration.ITALIC, false));
            String reqContainer = "HONEY".equalsIgnoreCase(fluidType) ? "Glass Bottle" : "Bucket";
            int units = "HONEY".equalsIgnoreCase(fluidType) ? (int)(amountMb / 250) : (int)(amountMb / 1000);
            String unitName = "HONEY".equalsIgnoreCase(fluidType) ? "Bottles" : "Buckets";
            meta.lore(List.of(
                    Component.text("Stored: " + Items.formatAmount(amountMb) + " mB (" + units + " " + unitName + ")", NamedTextColor.WHITE)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Left Click: Withdraw 1 (" + reqContainer + ")", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("▪ MultiverseNets", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
            ));
            meta.getPersistentDataContainer().set(Keys.TERMINAL_DISPLAY, PersistentDataType.BYTE, (byte) 2);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    protected void click(InventoryClickEvent event) {
        int raw = event.getRawSlot();
        switch (raw) {
            case PURGER_TOGGLE_SLOT -> {
                showOnlyPurged = !showOnlyPurged;
                if (showOnlyPurged) {
                    showFluids = false;
                }
                page = 0;
                refresh();
                return;
            }
            case PREV_SLOT -> {
                if (page > 0) {
                    page--;
                    refresh();
                }
                return;
            }
            case NEXT_SLOT -> {
                page++;
                refresh();
                return;
            }
            case SORT_SLOT -> {
                if (event.getClick() == ClickType.RIGHT) {
                    player.closeInventory();
                    ChatPrompts.ask(player, "Type your search term:", text -> {
                        query = text == null ? "" : text;
                        page = 0;
                        openMenu();
                    });
                } else if (event.getClick() == ClickType.SHIFT_RIGHT) {
                    query = "";
                    page = 0;
                    refresh();
                } else {
                    sortOrder = sortOrder == SortOrder.MVN_ALPHABETIC ? SortOrder.MVN_AMOUNT : SortOrder.MVN_ALPHABETIC;
                    page = 0;
                    refresh();
                }
                return;
            }
            case FLUIDS_TOGGLE_SLOT -> {
                showFluids = !showFluids;
                if (showFluids) {
                    showOnlyPurged = false;
                }
                page = 0;
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
                refresh();
                return;
            }
            default -> {
            }
        }

        for (int slot : DISPLAY_SLOTS) {
            if (raw == slot) {
                if (showFluids) {
                    String fluidType = displayedFluids.get(slot);
                    withdrawFluid(event, fluidType);
                } else if (displayedRecovered.containsKey(slot)) {
                    takeRecovered(event, displayedRecovered.get(slot));
                } else {
                    withdrawFromDisplay(event);
                }
                return;
            }
        }

        if (raw >= event.getView().getTopInventory().getSize()) {
            insertPlayerStack(event);
        }
    }

    private void withdrawFluid(InventoryClickEvent event, String fluidType) {
        if (fluidType == null) return;
        long stored = network.fluidStorage().count(fluidType);
        Material reqContainer = "HONEY".equalsIgnoreCase(fluidType) ? Material.GLASS_BOTTLE : Material.BUCKET;
        Material filledItem = switch (fluidType.toUpperCase(java.util.Locale.ROOT)) {
            case "LAVA" -> Material.LAVA_BUCKET;
            case "MILK" -> Material.MILK_BUCKET;
            case "POWDER_SNOW" -> Material.POWDER_SNOW_BUCKET;
            case "HONEY" -> Material.HONEY_BOTTLE;
            default -> Material.WATER_BUCKET;
        };
        int mbCost = "HONEY".equalsIgnoreCase(fluidType) ? 250 : 1000;
        String containerName = "HONEY".equalsIgnoreCase(fluidType) ? "Glass Bottle" : "Bucket";

        // 1. Check if network has at least 1 unit of fluid
        if (stored < mbCost) {
            player.sendMessage(Text.msg("Not enough " + fluidType + " in network! (Need at least " + mbCost + " mB)", NamedTextColor.RED));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // 2. Check if player has the required container on cursor
        ItemStack cursor = event.getView().getCursor();
        boolean shift = event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT;

        if (cursor != null && cursor.getType() == reqContainer) {
            if (network.fluidStorage().withdraw(fluidType, mbCost) >= mbCost) {
                cursor.setAmount(cursor.getAmount() - 1);
                if (cursor.getAmount() <= 0) {
                    event.getView().setCursor(new ItemStack(filledItem));
                } else {
                    event.getView().setCursor(cursor);
                    giveOrDrop(new ItemStack(filledItem));
                }
                player.playSound(player.getLocation(), "HONEY".equalsIgnoreCase(fluidType) ? org.bukkit.Sound.ITEM_BOTTLE_FILL : org.bukkit.Sound.ITEM_BUCKET_FILL, 1f, 1f);
                player.sendMessage(Text.msg("Withdrew 1x " + filledItem.name() + " from network fluids.", NamedTextColor.GREEN));
                draw();
                return;
            }
        }

        // 3. Check player inventory for the required container
        int containerSlot = -1;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack it = player.getInventory().getItem(i);
            if (it != null && it.getType() == reqContainer) {
                containerSlot = i;
                break;
            }
        }

        if (containerSlot == -1) {
            player.sendMessage(Text.msg("You need an empty " + containerName + " to withdraw " + fluidType + "!", NamedTextColor.RED));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        if (shift) {
            int filledCount = 0;
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack it = player.getInventory().getItem(i);
                if (it != null && it.getType() == reqContainer) {
                    while (it.getAmount() > 0 && network.fluidStorage().count(fluidType) >= mbCost) {
                        if (network.fluidStorage().withdraw(fluidType, mbCost) >= mbCost) {
                            it.setAmount(it.getAmount() - 1);
                            giveOrDrop(new ItemStack(filledItem));
                            filledCount++;
                        } else {
                            break;
                        }
                    }
                    if (it.getAmount() <= 0) {
                        player.getInventory().setItem(i, null);
                    }
                    if (network.fluidStorage().count(fluidType) < mbCost) break;
                }
            }
            if (filledCount > 0) {
                player.playSound(player.getLocation(), "HONEY".equalsIgnoreCase(fluidType) ? org.bukkit.Sound.ITEM_BOTTLE_FILL : org.bukkit.Sound.ITEM_BUCKET_FILL, 1f, 1f);
                player.sendMessage(Text.msg("Withdrew " + filledCount + "x " + filledItem.name() + " from network.", NamedTextColor.GREEN));
            }
        } else {
            ItemStack it = player.getInventory().getItem(containerSlot);
            if (it != null && network.fluidStorage().withdraw(fluidType, mbCost) >= mbCost) {
                it.setAmount(it.getAmount() - 1);
                if (it.getAmount() <= 0) {
                    player.getInventory().setItem(containerSlot, null);
                }
                giveOrDrop(new ItemStack(filledItem));
                player.playSound(player.getLocation(), "HONEY".equalsIgnoreCase(fluidType) ? org.bukkit.Sound.ITEM_BOTTLE_FILL : org.bukkit.Sound.ITEM_BUCKET_FILL, 1f, 1f);
                player.sendMessage(Text.msg("Withdrew 1x " + filledItem.name() + " from network.", NamedTextColor.GREEN));
            }
        }
        draw();
        player.updateInventory();
    }

    private void withdrawFromDisplay(InventoryClickEvent event) {
        ItemStack icon = event.getCurrentItem();
        if (!isGridStack(icon)) {
            return;
        }
        int raw = event.getRawSlot();
        ItemStack sample = (raw >= 0 && raw < displayedSamples.length) ? displayedSamples[raw] : null;
        ItemStack target = sample != null ? sample : cleanStack(icon);
        Predicate<ItemStack> matches =
                item -> com.chagui68.multiversenets.util.StackUtils.itemsMatch(item, target);
        ClickType click = event.getClick();
        boolean shift = click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT;

        if (shift) {
            int want = Math.min(target.getMaxStackSize(), 64);
            ItemStack withdrawn = network.storage().withdraw(matches, want);
            if (withdrawn != null) {
                int leftover = NetworkManager.insertInto(player.getInventory(), withdrawn);
                if (leftover > 0) {
                    withdrawn.setAmount(leftover);
                    int unreturned = network.storage().deposit(withdrawn);
                    if (unreturned > 0) {
                        withdrawn.setAmount(unreturned);
                        giveOrDrop(withdrawn);
                    }
                }
            }
            draw();
            player.updateInventory();
            return;
        }

        var view = event.getView();
        ItemStack cursor = view.getCursor();
        boolean right = click == ClickType.RIGHT;

        if (cursor == null || cursor.getType().isAir()) {
            int want = right ? Math.min(target.getMaxStackSize(), 64) : 1;
            ItemStack withdrawn = network.storage().withdraw(matches, want);
            if (withdrawn != null) {
                view.setCursor(withdrawn);
            }
        } else if (!right && StackUtils.itemsMatch(target, cursor) && cursor.getAmount() < cursor.getMaxStackSize()) {
            ItemStack single = network.storage().withdraw(matches, 1);
            if (single != null) {
                cursor.setAmount(Math.min(cursor.getMaxStackSize(), cursor.getAmount() + 1));
                view.setCursor(cursor);
            }
        }
        draw();
        player.updateInventory();
    }

    private boolean isFluidContainer(ItemStack item) {
        if (item == null) return false;
        Material m = item.getType();
        return m == Material.WATER_BUCKET || m == Material.LAVA_BUCKET || m == Material.MILK_BUCKET
                || m == Material.POWDER_SNOW_BUCKET || m == Material.HONEY_BOTTLE;
    }

    private void depositFluidContainer(InventoryClickEvent event, ItemStack item) {
        Material m = item.getType();
        String fluid = switch (m) {
            case WATER_BUCKET -> "WATER";
            case LAVA_BUCKET -> "LAVA";
            case MILK_BUCKET -> "MILK";
            case POWDER_SNOW_BUCKET -> "POWDER_SNOW";
            case HONEY_BOTTLE -> "HONEY";
            default -> null;
        };
        if (fluid == null) return;
        int mb = "HONEY".equals(fluid) ? 250 : 1000;
        Material emptyContainer = "HONEY".equals(fluid) ? Material.GLASS_BOTTLE : Material.BUCKET;

        long remainder = network.fluidStorage().deposit(fluid, mb);
        if (remainder == 0) {
            int playerSlot = playerInventorySlot(event);
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
                giveOrDrop(new ItemStack(emptyContainer));
            } else {
                player.getInventory().setItem(playerSlot, new ItemStack(emptyContainer));
            }
            player.playSound(player.getLocation(), "HONEY".equals(fluid) ? org.bukkit.Sound.ITEM_BOTTLE_EMPTY : org.bukkit.Sound.ITEM_BUCKET_EMPTY, 1f, 1f);
            player.sendMessage(Text.msg("Deposited " + mb + " mB of " + fluid + " into network fluids.", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Text.msg("Cannot deposit " + fluid + ": network has no Quantum Fluid Cell with capacity!", NamedTextColor.RED));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
        draw();
        player.updateInventory();
    }

    private void insertPlayerStack(InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType().isAir()) {
            return;
        }
        if (isFluidContainer(item)) {
            depositFluidContainer(event, item);
            return;
        }
        ItemStack actual = item.clone();
        int initialAmount = actual.getAmount();
        int playerSlot = playerInventorySlot(event);
        player.getInventory().setItem(playerSlot, null);
        int leftover = network.storage().deposit(actual);
        if (leftover <= 0) {
            player.sendMessage(Text.msg("Deposited " + Items.formatAmount(initialAmount) + " items.", NamedTextColor.GREEN));
        } else {
            if (leftover < initialAmount) {
                player.sendMessage(Text.msg("Deposited " + Items.formatAmount(initialAmount - leftover) + " items.", NamedTextColor.GREEN));
            } else {
                player.sendMessage(Text.msg("Nothing deposited: the network needs a Quantum Cell "
                        + "with free space for this item.", NamedTextColor.RED));
            }
            ItemStack returned = actual.clone();
            returned.setAmount(leftover);
            player.getInventory().setItem(playerSlot, returned);
        }
        draw();
        player.updateInventory();
    }

    private void liveTick() {
        if (inv == null || player.getOpenInventory().getTopInventory().getHolder() != this) {
            cancelTask();
            return;
        }
        ItemStack input = inv.getItem(INPUT_SLOT);
        if (input != null && !input.getType().isAir()) {
            if (isFluidContainer(input)) {
                Material m = input.getType();
                String fluid = switch (m) {
                    case WATER_BUCKET -> "WATER";
                    case LAVA_BUCKET -> "LAVA";
                    case MILK_BUCKET -> "MILK";
                    case POWDER_SNOW_BUCKET -> "POWDER_SNOW";
                    case HONEY_BOTTLE -> "HONEY";
                    default -> null;
                };
                int mb = "HONEY".equals(fluid) ? 250 : 1000;
                Material empty = "HONEY".equals(fluid) ? Material.GLASS_BOTTLE : Material.BUCKET;
                if (fluid != null && network.fluidStorage().deposit(fluid, mb) == 0) {
                    // Las botellas de miel apilan hasta 16: solo se vacia una por ciclo, el resto
                    // del stack se queda en la ranura. Antes el stack entero se cambiaba por una
                    // sola botella vacia y se perdian las demas.
                    if (input.getAmount() > 1) {
                        input.setAmount(input.getAmount() - 1);
                        giveOrDrop(new ItemStack(empty));
                    } else {
                        inv.setItem(INPUT_SLOT, new ItemStack(empty));
                    }
                }
            } else {
                int leftover = network.storage().deposit(input);
                if (leftover <= 0) {
                    inv.setItem(INPUT_SLOT, null);
                } else {
                    input.setAmount(leftover);
                }
            }
        }
        draw();
    }

    @Override
    protected void onClose(InventoryCloseEvent event) {
        cancelTask();
        ItemStack input = inv.getItem(INPUT_SLOT);
        if (input == null || input.getType().isAir()) {
            return;
        }
        if (isFluidContainer(input)) {
            giveOrDrop(input);
            inv.setItem(INPUT_SLOT, null);
            return;
        }
        int leftover = network.storage().deposit(input);
        inv.setItem(INPUT_SLOT, null);
        if (leftover > 0) {
            input.setAmount(leftover);
            giveOrDrop(input);
        }
    }

    private void cancelTask() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
    }

    private boolean isGridStack(ItemStack icon) {
        if (icon == null || icon.getType().isAir() || !icon.hasItemMeta()) {
            return false;
        }
        Byte mark = icon.getItemMeta().getPersistentDataContainer()
                .get(Keys.TERMINAL_DISPLAY, PersistentDataType.BYTE);
        return mark != null && mark == (byte) 1;
    }

    private ItemStack cleanStack(ItemStack icon) {
        ItemStack clean = icon.clone();
        var meta = clean.getItemMeta();
        if (meta != null) {
            if (meta.hasLore() && meta.lore() != null) {
                List<Component> lore = new ArrayList<>(meta.lore());
                while (!lore.isEmpty()) {
                    Component last = lore.get(lore.size() - 1);
                    String str = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(last);
                    if (str.startsWith(AMOUNT_PREFIX) || str.contains("Greedy Buffer") || str.contains("In DRAM") || str.contains("Purger") || str.isBlank()) {
                        lore.remove(lore.size() - 1);
                    } else {
                        break;
                    }
                }
                meta.lore(lore.isEmpty() ? null : lore);
            }
            meta.getPersistentDataContainer().remove(Keys.TERMINAL_DISPLAY);
            clean.setItemMeta(meta);
        }
        return clean;
    }
}

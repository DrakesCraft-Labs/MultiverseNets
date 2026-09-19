package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.net.NetworkManager;
import com.chagui68.multiversenets.net.NetworkStorage;
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
    private static final int FILTER_SLOT = 35;
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
    private int page = 0;
    private String query = "";
    private SortOrder sortOrder = SortOrder.MVN_ALPHABETIC;
    private boolean showOnlyPurged = false;
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
        inv.setItem(SORT_SLOT, panel(Material.BLUE_STAINED_GLASS_PANE,
                sortOrder == SortOrder.MVN_ALPHABETIC ? "Change Sort Order: A-Z" : "Change Sort Order: Amount"));
        inv.setItem(FILTER_SLOT, filterIcon());
        inv.setItem(PREV_SLOT, panel(Material.RED_STAINED_GLASS_PANE, "Previous Page"));
        inv.setItem(NEXT_SLOT, panel(Material.RED_STAINED_GLASS_PANE, "Next Page"));

        java.util.Arrays.fill(displayedSamples, null);
        List<NetworkStorage.View> list = filteredItems();
        int pages = Math.max(1, (list.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page >= pages) {
            page = pages - 1;
        }
        int start = page * PAGE_SIZE;
        for (int i = 0; i < DISPLAY_SLOTS.length; i++) {
            int slot = DISPLAY_SLOTS[i];
            int index = start + i;
            if (index < list.size()) {
                NetworkStorage.View view = list.get(index);
                displayedSamples[slot] = view.sample();
                inv.setItem(slot, gridIcon(view));
            } else {
                inv.setItem(slot, background);
            }
        }
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
        out.removeIf(v -> !matchesSearch(v.sample(), q));
        return out;
    }

    private boolean matchesSearch(ItemStack item, String q) {
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

    private ItemStack filterIcon() {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(query.isBlank()
                        ? "Set Filter (Right Click to Clear)"
                        : "Filter: " + query + " (Right Click to Clear)",
                NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    protected void click(InventoryClickEvent event) {
        int raw = event.getRawSlot();
        switch (raw) {
            case PURGER_TOGGLE_SLOT -> {
                showOnlyPurged = !showOnlyPurged;
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
                sortOrder = sortOrder == SortOrder.MVN_ALPHABETIC ? SortOrder.MVN_AMOUNT : SortOrder.MVN_ALPHABETIC;
                page = 0;
                refresh();
                return;
            }
            case FILTER_SLOT -> {
                if (event.getClick() == ClickType.RIGHT) {
                    query = "";
                    page = 0;
                    refresh();
                } else {
                    player.closeInventory();
                    ChatPrompts.ask(player, "Type your search term:", text -> {
                        query = text == null ? "" : text;
                        page = 0;
                        openMenu();
                    });
                }
                return;
            }
            default -> {
            }
        }

        for (int slot : DISPLAY_SLOTS) {
            if (raw == slot) {
                withdrawFromDisplay(event);
                return;
            }
        }

        if (raw >= event.getView().getTopInventory().getSize()) {
            insertPlayerStack(event);
        }
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
                    network.storage().deposit(withdrawn);
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

    private void insertPlayerStack(InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType().isAir()) {
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
            int leftover = network.storage().deposit(input);
            if (leftover <= 0) {
                inv.setItem(INPUT_SLOT, null);
            } else {
                input.setAmount(leftover);
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
                    if (str.startsWith(AMOUNT_PREFIX) || str.contains("Greedy Buffer") || str.contains("Purger") || str.isBlank()) {
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

package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.craft.Blueprints;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.net.NetworkManager;
import com.chagui68.multiversenets.net.NetworkStorage;
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
import org.bukkit.inventory.Recipe;

import java.util.ArrayList;
import java.util.List;

/**
 * Interactive crafting grid menu allowing players to craft items using items stored in the network.
 *
 * Menú de mesa de crafteo interactiva que permite a los jugadores craftear usando ítems de la red.
 */
public class CraftingGridMenu extends MenuHolder {

    private static final int[] MATRIX_SLOTS = {0, 1, 2, 9, 10, 11, 18, 19, 20};
    private static final int[] BROWSER_SLOTS = {3, 4, 5, 6, 7, 8, 12, 13, 14, 15, 16, 17, 21, 22, 23, 24, 25, 26};

    private static final int PREV_SLOT = 27;
    private static final int NEXT_SLOT = 29;
    private static final int RESULT_SLOT = 31;
    private static final int CRAFT_ONE_SLOT = 33;
    private static final int CRAFT_ALL_SLOT = 35;
    private static final int CLEAR_SLOT = 38;
    private static final int INFO_SLOT = 41;

    private final Network network;
    private final Block block;
    private int page = 0;

    public CraftingGridMenu(MultiverseNets plugin, Player player, Network network, Block block) {
        super(plugin, player);
        this.network = network;
        this.block = block;
    }

    public void openMenu() {
        open(54, Component.text("Crafting Grid", NamedTextColor.DARK_AQUA)
                .decoration(TextDecoration.ITALIC, false));
    }

    private NodeBlob blob() {
        NodeBlob blob = NodeStore.get(block);
        return blob == null ? NodeBlob.create(com.chagui68.multiversenets.item.DeviceType.MVN_CRAFTING_GRID.name()) : blob;
    }

    private ItemStack[] matrix() {
        return blob().craftingMatrix;
    }

    private boolean isMatrixSlot(int raw) {
        for (int slot : MATRIX_SLOTS) {
            if (slot == raw) {
                return true;
            }
        }
        return false;
    }

    private boolean isBrowserSlot(int raw) {
        for (int slot : BROWSER_SLOTS) {
            if (slot == raw) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void draw() {
        ItemStack[] matrix = matrix();
        for (int i = 0; i < MATRIX_SLOTS.length; i++) {
            ItemStack tpl = matrix[i];
            if (tpl != null && !tpl.getType().isAir()) {
                inv.setItem(MATRIX_SLOTS[i], StackUtils.getAsQuantity(tpl, 1));
            } else {
                inv.setItem(MATRIX_SLOTS[i], panel(Material.BLACK_STAINED_GLASS_PANE, "Empty"));
            }
        }

        List<NetworkStorage.View> view = network.storage().view();
        int pages = Math.max(1, (view.size() + BROWSER_SLOTS.length - 1) / BROWSER_SLOTS.length);
        if (page >= pages) {
            page = pages - 1;
        }
        int start = page * BROWSER_SLOTS.length;
        for (int i = 0; i < BROWSER_SLOTS.length; i++) {
            int index = start + i;
            if (index < view.size()) {
                inv.setItem(BROWSER_SLOTS[i], browserIcon(view.get(index)));
            } else {
                inv.setItem(BROWSER_SLOTS[i], panel(Material.GRAY_STAINED_GLASS_PANE, " "));
            }
        }
        drawControls(pages);
    }

    private ItemStack browserIcon(NetworkStorage.View view) {
        ItemStack icon = StackUtils.getAsQuantity(view.sample(), 1);
        var meta = icon.getItemMeta();
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Amount: " + Items.formatAmount(view.amount()), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Click: add 1 to the crafting plan", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private void drawControls(int pages) {
        inv.setItem(PREV_SLOT, button(Material.ARROW, "Previous page", page > 0));
        inv.setItem(NEXT_SLOT, button(Material.SPECTRAL_ARROW, "Next page", page < pages - 1));

        Recipe recipe = currentRecipe();
        if (recipe != null) {
            ItemStack result = recipe.getResult().clone();
            var meta = result.getItemMeta();
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            result.setItemMeta(meta);
            inv.setItem(RESULT_SLOT, result);
        } else {
            inv.setItem(RESULT_SLOT, button(Material.BARRIER, "No match", false));
        }

        inv.setItem(CRAFT_ONE_SLOT, button(Material.CRAFTING_TABLE, "Craft x1", recipe != null));
        inv.setItem(CRAFT_ALL_SLOT, button(Material.BLAST_FURNACE, "Craft x16", recipe != null));
        inv.setItem(CLEAR_SLOT, button(Material.WATER_BUCKET, "Clear plan", true));
        inv.setItem(INFO_SLOT, button(Material.BOOK, "Pulls ingredients from the network", true));
    }

    private Recipe currentRecipe() {
        ItemStack[] matrix = matrix();
        if (Blueprints.isEmpty(matrix)) {
            return null;
        }
        return Blueprints.resolve(matrix, network.world());
    }

    private ItemStack button(Material material, String label, boolean enabled) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(label, enabled ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    protected void click(InventoryClickEvent event) {
        int raw = event.getRawSlot();
        switch (raw) {
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
            case CRAFT_ONE_SLOT -> {
                craft(1);
                return;
            }
            case CRAFT_ALL_SLOT -> {
                craft(16);
                return;
            }
            case CLEAR_SLOT -> {
                NodeBlob blob = blob();
                for (int i = 0; i < blob.craftingMatrix.length; i++) {
                    blob.craftingMatrix[i] = null;
                }
                NodeStore.put(block, blob);
                refresh();
                return;
            }
            case RESULT_SLOT, INFO_SLOT -> {
                return;
            }
            default -> {
            }
        }

        if (isMatrixSlot(raw)) {
            handleMatrixClick(event);
            return;
        }
        if (isBrowserSlot(raw)) {
            handleBrowserClick(raw);
            return;
        }
        if (raw >= event.getView().getTopInventory().getSize()
                && (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT)) {
            depositStack(event);
        }
    }

    private void handleMatrixClick(InventoryClickEvent event) {
        int idx = 0;
        for (int i = 0; i < MATRIX_SLOTS.length; i++) {
            if (MATRIX_SLOTS[i] == event.getRawSlot()) {
                idx = i;
                break;
            }
        }
        NodeBlob blob = blob();
        ItemStack cursor = event.getView().getCursor();
        if (cursor != null && !cursor.getType().isAir()) {
            blob.craftingMatrix[idx] = StackUtils.getAsQuantity(cursor, 1);
        } else {
            blob.craftingMatrix[idx] = null;
        }
        NodeStore.put(block, blob);
        refresh();
    }

    private void handleBrowserClick(int raw) {
        int indexInPage = -1;
        for (int i = 0; i < BROWSER_SLOTS.length; i++) {
            if (BROWSER_SLOTS[i] == raw) {
                indexInPage = i;
                break;
            }
        }
        if (indexInPage < 0) {
            return;
        }
        List<NetworkStorage.View> view = network.storage().view();
        int listIndex = page * BROWSER_SLOTS.length + indexInPage;
        if (listIndex >= view.size()) {
            return;
        }
        ItemStack sample = view.get(listIndex).sample();

        NodeBlob blob = blob();
        ItemStack[] matrix = blob.craftingMatrix;
        for (int i = 0; i < 9; i++) {
            if (matrix[i] == null || matrix[i].getType().isAir()) {
                matrix[i] = StackUtils.getAsQuantity(sample, 1);
                NodeStore.put(block, blob);
                refresh();
                return;
            }
        }
        player.sendMessage(Text.msg("The crafting plan is full.", NamedTextColor.RED));
    }

    /**
     * Attempts crafting operations, withdrawing ingredients atomically from network storage.
 *
     * Intenta operaciones de crafteo, extrayendo ingredientes de forma atómica del almacenamiento de red.
     */
    private void craft(int maxCrafts) {
        int crafted = 0;
        for (int n = 0; n < maxCrafts; n++) {
            Recipe recipe = currentRecipe();
            if (recipe == null) {
                if (n == 0) {
                    player.sendMessage(Text.msg("That arrangement does not match any recipe.", NamedTextColor.RED));
                }
                break;
            }
            ItemStack[] plan = Blueprints.normalize(matrix());

            record Need(ItemStack sample, int amount) {}
            List<Need> needs = new ArrayList<>();
            for (ItemStack input : plan) {
                if (input == null || input.getType().isAir()) {
                    continue;
                }
                boolean merged = false;
                for (int i = 0; i < needs.size(); i++) {
                    Need req = needs.get(i);
                    if (StackUtils.itemsMatch(req.sample(), input)) {
                        needs.set(i, new Need(req.sample(), req.amount() + 1));
                        merged = true;
                        break;
                    }
                }
                if (!merged) {
                    needs.add(new Need(StackUtils.getAsQuantity(input, 1), 1));
                }
            }
            boolean missing = false;
            for (Need need : needs) {
                if (network.storage().count(item -> StackUtils.itemsMatch(item, need.sample())) < need.amount()) {
                    missing = true;
                    break;
                }
            }
            if (missing) {
                if (n == 0) {
                    player.sendMessage(Text.msg("Not enough ingredients in the network.", NamedTextColor.RED));
                }
                break;
            }
            List<ItemStack> taken = new ArrayList<>();
            for (ItemStack slot : plan) {
                if (slot == null) {
                    continue;
                }
                ItemStack got = network.storage().withdraw(item -> StackUtils.itemsMatch(item, slot), 1);
                if (got == null) {
                    break;
                }
                taken.add(got);
            }
            if (taken.size() < countInputs(plan)) {
                network.storage().depositAll(taken);
                if (n == 0) {
                    player.sendMessage(Text.msg("Not enough ingredients in the network.", NamedTextColor.RED));
                }
                break;
            }

            ItemStack result = recipe.getResult().clone();
            int leftover = NetworkManager.insertInto(player.getInventory(), result);
            if (leftover > 0) {
                result.setAmount(leftover);
                network.storage().deposit(result);
            }
            crafted++;
        }
        if (crafted > 0) {
            player.sendMessage(Text.msg("Crafted x" + crafted + ".", NamedTextColor.GREEN));
        }
        refresh();
    }

    private static int countInputs(ItemStack[] plan) {
        int n = 0;
        for (ItemStack s : plan) {
            if (s != null && !s.getType().isAir()) {
                n++;
            }
        }
        return n;
    }

    private void depositStack(InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType().isAir()) {
            return;
        }
        ItemStack actual = item.clone();
        int initialAmount = actual.getAmount();
        int playerSlot = playerInventorySlot(event);
        player.getInventory().setItem(playerSlot, null);
        int leftover = network.storage().deposit(actual);
        if (leftover > 0) {
            actual.setAmount(leftover);
            player.getInventory().setItem(playerSlot, actual);
        }
        if (leftover < initialAmount) {
            player.sendMessage(Text.msg("Deposited " + Items.formatAmount(initialAmount - leftover) + " items.",
                    NamedTextColor.GREEN));
        }
        refresh();
        player.updateInventory();
    }

    private ItemStack panel(Material material, String name) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}

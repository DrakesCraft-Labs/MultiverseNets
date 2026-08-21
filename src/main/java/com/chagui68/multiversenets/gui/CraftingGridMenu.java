package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.net.NetworkManager;
import com.chagui68.multiversenets.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.ArrayList;
import java.util.List;

public class CraftingGridMenu extends MenuHolder {

    private static final int[] MATRIX_SLOTS = {0, 1, 2, 9, 10, 11, 18, 19, 20};
    private static final int[] BROWSER_SLOTS = {3, 4, 5, 6, 7, 8, 12, 13, 14, 15, 16, 17, 21, 22, 23, 24, 25, 26};

    private final Network network;
    private final ItemStack[] matrix = new ItemStack[9];
    private int page = 0;

    public CraftingGridMenu(MultiverseNets plugin, Player player, Network network) {
        super(plugin, player);
        this.network = network;
    }

    public void openMenu() {
        open(54, Component.text("Crafting Grid", NamedTextColor.DARK_AQUA)
                .decoration(TextDecoration.ITALIC, false));
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
        List<NetworkStorageView> view = wrap(network.storage().view());
        int pages = Math.max(1, (view.size() + BROWSER_SLOTS.length - 1) / BROWSER_SLOTS.length);
        if (page >= pages) {
            page = pages - 1;
        }
        int start = page * BROWSER_SLOTS.length;
        for (int i = 0; i < BROWSER_SLOTS.length && start + i < view.size(); i++) {
            inv.setItem(BROWSER_SLOTS[i], browserIcon(view.get(start + i)));
        }
        drawControls(pages);
    }

    private record NetworkStorageView(ItemStack sample, long amount) {
    }

    private List<NetworkStorageView> wrap(List<com.chagui68.multiversenets.net.NetworkStorage.View> original) {
        List<NetworkStorageView> out = new ArrayList<>();
        for (var v : original) {
            out.add(new NetworkStorageView(v.sample(), v.amount()));
        }
        return out;
    }

    private ItemStack browserIcon(NetworkStorageView view) {
        ItemStack icon = view.sample().clone();
        icon.setAmount(Math.max(1, Math.min(icon.getMaxStackSize(), 64)));
        var meta = icon.getItemMeta();
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Amount: " + Items.formatAmount(view.amount()), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Click: add 1 to grid", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private void drawControls(int pages) {
        inv.setItem(27, button(Material.ARROW, "Previous page", page > 0));
        inv.setItem(29, button(Material.SPECTRAL_ARROW, "Next page", page < pages - 1));

        ItemStack result = computeResult();
        if (result != null) {
            var meta = result.getItemMeta();
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            result.setItemMeta(meta);
            inv.setItem(31, result);
        } else {
            inv.setItem(31, button(Material.BARRIER, "No match", false));
        }

        inv.setItem(33, button(Material.CRAFTING_TABLE, "Craft x1", result != null));
        inv.setItem(35, button(Material.BLAST_FURNACE, "Craft all", result != null));
        inv.setItem(38, button(Material.WATER_BUCKET, "Clear grid", true));
        inv.setItem(41, button(Material.BOOK, "Pulls ingredients from the network", true));
    }

    private ItemStack button(Material material, String label, boolean enabled) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(label, enabled ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack[] matrixArray() {
        ItemStack[] arr = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            arr[i] = matrix[i] == null ? null : matrix[i].clone();
        }
        return arr;
    }

    private CraftingRecipe computeRecipe() {
        ItemStack[] arr = new ItemStack[9];
        boolean any = false;
        for (int i = 0; i < 9; i++) {
            if (matrix[i] != null) {
                any = true;
                ItemStack copy = matrix[i].clone();
                copy.setAmount(1);
                arr[i] = copy;
            }
        }
        if (!any) {
            return null;
        }
        Recipe recipe = Bukkit.getCraftingRecipe(arr, network.world());
        return recipe instanceof CraftingRecipe cr ? cr : null;
    }

    private ItemStack computeResult() {
        CraftingRecipe recipe = computeRecipe();
        return recipe == null ? null : recipe.getResult().clone();
    }

    @Override
    protected void click(InventoryClickEvent event) {
        int raw = event.getRawSlot();
        switch (raw) {
            case 27 -> {
                if (page > 0) {
                    page--;
                    refresh();
                }
                return;
            }
            case 29 -> {
                page++;
                refresh();
                return;
            }
            case 33 -> {
                craft(event, 1);
                return;
            }
            case 35 -> {
                craft(event, 64);
                return;
            }
            case 38 -> {
                java.util.Arrays.fill(matrix, null);
                refresh();
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
        if (event.getClickedInventory() == event.getView().getBottomInventory()
                && (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT)) {
            depositAllFromPlayer();
        }
    }

    private void handleMatrixClick(InventoryClickEvent event) {
        int idx = matrixIndex(event.getRawSlot());
        ItemStack cursor = event.getCursor();
        if (cursor != null && !cursor.getType().isAir()) {
            matrix[idx] = cursor.clone();
            matrix[idx].setAmount(1);
        } else {
            matrix[idx] = null;
        }
        refresh();
    }

    private int matrixIndex(int raw) {
        for (int i = 0; i < MATRIX_SLOTS.length; i++) {
            if (MATRIX_SLOTS[i] == raw) {
                return i;
            }
        }
        return 0;
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
        List<com.chagui68.multiversenets.net.NetworkStorage.View> view = network.storage().view();
        int start = page * BROWSER_SLOTS.length;
        int listIndex = start + indexInPage;
        if (listIndex >= view.size()) {
            return;
        }
        ItemStack sample = view.get(listIndex).sample();

        for (int i = 0; i < 9; i++) {
            if (matrix[i] == null) {
                matrix[i] = sample.clone();
                matrix[i].setAmount(1);
                refresh();
                return;
            }
        }
        player.sendMessage(Text.msg("The crafting grid is full.", NamedTextColor.RED));
    }

    private void craft(InventoryClickEvent event, int maxCrafts) {
        int crafted = 0;
        for (int n = 0; n < maxCrafts; n++) {
            CraftingRecipe recipe = computeRecipe();
            if (recipe == null) {
                if (n == 0) {
                    player.sendMessage(Text.msg("That arrangement does not match any recipe.", NamedTextColor.RED));
                }
                break;
            }
            ItemStack[] needed = new ItemStack[9];
            for (int i = 0; i < 9; i++) {
                if (matrix[i] != null) {
                    needed[i] = matrix[i].clone();
                    needed[i].setAmount(1);
                }
            }

            List<ItemStack> taken = new ArrayList<>();
            boolean missing = false;
            for (ItemStack req : needed) {
                if (req == null) {
                    continue;
                }
                long have = network.storage().count(req::isSimilar);
                if (have < 1) {
                    missing = true;
                    break;
                }
                ItemStack got = network.storage().withdraw(req::isSimilar, 1);
                if (got == null) {
                    missing = true;
                    break;
                }
                taken.add(got);
            }
            if (missing) {
                network.storage().depositAll(taken);
                player.sendMessage(Text.msg("Not enough ingredients in the network.", NamedTextColor.RED));
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

    private void depositAllFromPlayer() {
        ItemStack[] contents = player.getInventory().getContents();
        long deposited = 0;
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it == null || it.getType().isAir()) {
                continue;
            }
            int leftover = network.storage().deposit(it);
            if (leftover <= 0) {
                player.getInventory().setItem(i, null);
                deposited += it.getAmount();
            } else if (leftover < it.getAmount()) {
                deposited += it.getAmount() - leftover;
                it.setAmount(leftover);
            }
        }
        if (deposited > 0) {
            player.sendMessage(Text.msg("Deposited " + Items.formatAmount(deposited) + " items.", NamedTextColor.GREEN));
        }
        refresh();
    }
}

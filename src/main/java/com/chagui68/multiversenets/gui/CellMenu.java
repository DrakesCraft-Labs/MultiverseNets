package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
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
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.List;
import java.util.Set;

/**
 * GUI menu for Quantum Storage Cells (T1-T6 and Greedy Cell): high-capacity storage for a single item type.
 *
 * Menú de interfaz para Celdas Cuánticas (T1-T6 y Greedy Cell): almacén de alta capacidad para un solo tipo de ítem.
 */
public class CellMenu extends MenuHolder {

    public static final int ITEM_SLOT = 4;
    public static final int DEPOSIT_ALL_SLOT = 11;
    public static final int SET_SLOT = 13;
    public static final int EXTRACT_ALL_SLOT = 15;

    private static final int[] BACKGROUND_SLOTS = {
            0, 1, 2, 3, 5, 6, 7, 8,
            9, 10, 12, 14, 16, 17
    };

    private final Block block;
    private final DeviceType type;

    public CellMenu(MultiverseNets plugin, Player player, Block block, DeviceType type) {
        super(plugin, player);
        this.block = block;
        this.type = type;
    }

    public void openMenu() {
        open(18, Component.text(type.display(), NamedTextColor.DARK_AQUA)
                .decoration(TextDecoration.ITALIC, false));
    }

    @Override
    protected Set<Integer> vanillaSlots() {
        return Set.of();
    }

    @Override
    protected void draw() {
        ItemStack background = panel(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot : BACKGROUND_SLOTS) {
            inv.setItem(slot, background);
        }

        ItemStack setItem = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        var metaSet = setItem.getItemMeta();
        metaSet.displayName(Component.text("Set Item", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        metaSet.lore(List.of(
                passiveText("Click with an item on your cursor to register it."),
                passiveText("Only works while the cell is empty."),
                Component.empty(),
                Component.text("Shift+Click: Toggle void excess", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)));
        setItem.setItemMeta(metaSet);
        inv.setItem(SET_SLOT, setItem);

        ItemStack depositAll = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
        var metaDep = depositAll.getItemMeta();
        metaDep.displayName(Component.text("Quick Deposit", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        metaDep.lore(List.of(
                passiveText("Click to deposit all matching items"),
                passiveText("from your inventory into this cell.")));
        depositAll.setItemMeta(metaDep);
        inv.setItem(DEPOSIT_ALL_SLOT, depositAll);

        ItemStack extractAll = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        var metaExt = extractAll.getItemMeta();
        metaExt.displayName(Component.text("Quick Take Out", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        metaExt.lore(List.of(
                passiveText("Left Click: Fill inventory with stored items"),
                passiveText("Right Click: Take 1 item"),
                passiveText("Shift+Right Click: Take 64 items (1 stack)")));
        extractAll.setItemMeta(metaExt);
        inv.setItem(EXTRACT_ALL_SLOT, extractAll);

        updateDisplay();
    }

    private Component passiveText(String text) {
        return Component.text(text, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
    }

    private void updateDisplay() {
        NodeBlob blob = NodeStore.get(block);
        long cap = Items.capacityOf(type);
        ItemStack icon;
        if (blob == null || blob.cellSample == null || blob.cellAmount <= 0) {
            icon = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            var meta = icon.getItemMeta();
            meta.displayName(Component.text("No Registered Item", NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Capacity: " + Items.formatAmount(cap), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Stores a single item type", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Click with item on cursor to set", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)));
            icon.setItemMeta(meta);
        } else {
            icon = blob.cellSample.clone();
            icon.setAmount(1);
            var meta = icon.getItemMeta();
            meta.lore(List.of(
                    Component.empty(),
                    Component.text("Amount: " + blob.cellAmount + " / " + Items.formatAmount(cap),
                            NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Usage: " + (cap > 0 ? (blob.cellAmount * 100 / cap) : 0) + "%", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Left Click: Take 1 item", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
                    Component.text("Right Click: Take 64 items (1 stack)", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
                    Component.text("Shift+Click: Fill inventory", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)));
            icon.setItemMeta(meta);
        }
        inv.setItem(ITEM_SLOT, icon);
    }

    private boolean absorbIntoCell(NodeBlob blob, ItemStack stack) {
        if (stack == null || stack.getType().isAir() || stack.getAmount() <= 0) {
            return false;
        }
        long cap = Items.capacityOf(type);
        if (blob.cellSample != null && !StackUtils.itemsMatch(blob.cellSample, stack)) {
            return false;
        }
        long space = cap - blob.cellAmount;
        if (space <= 0) {
            return false;
        }
        int take = (int) Math.min(Math.min(space, stack.getAmount()), Integer.MAX_VALUE);
        if (take <= 0) {
            return false;
        }
        if (blob.cellSample == null) {
            blob.cellSample = StackUtils.getAsQuantity(stack, 1);
        }
        blob.cellAmount += take;
        stack.setAmount(stack.getAmount() - take);
        NodeStore.put(block, blob);
        return true;
    }

    @Override
    protected void click(InventoryClickEvent event) {
        int raw = event.getRawSlot();
        NodeBlob blob = NodeStore.get(block);
        if (blob == null) {
            return;
        }

        if (raw == SET_SLOT) {
            if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
                blob.filterBlacklist = !blob.filterBlacklist;
                NodeStore.put(block, blob);
                player.sendMessage(Text.msg(blob.filterBlacklist
                        ? "Void excess items: ENABLED" : "Void excess items: DISABLED", NamedTextColor.GREEN));
                return;
            }
            if (blob.cellSample != null && blob.cellAmount > 0) {
                player.sendMessage(Text.msg("The cell holds " + Items.formatAmount(blob.cellAmount)
                        + ". Empty it before changing the stored item.", NamedTextColor.RED));
                return;
            }
            ItemStack cursor = event.getView().getCursor();
            if (cursor == null || cursor.getType().isAir()) {
                return;
            }
            blob.cellSample = StackUtils.getAsQuantity(cursor, 1);
            NodeStore.put(block, blob);
            updateDisplay();
            player.sendMessage(Text.msg("Stored item set to: " + blob.cellSample.getType().name(), NamedTextColor.GREEN));
            return;
        }

        if (raw == DEPOSIT_ALL_SLOT) {
            performQuickDeposit(blob);
            updateDisplay();
            return;
        }

        if (raw == EXTRACT_ALL_SLOT) {
            performQuickExtract(blob, event.getClick());
            updateDisplay();
            return;
        }

        if (raw == ITEM_SLOT) {
            if (blob.cellSample == null || blob.cellAmount <= 0) {
                ItemStack cursor = event.getView().getCursor();
                if (cursor != null && !cursor.getType().isAir()) {
                    blob.cellSample = StackUtils.getAsQuantity(cursor, 1);
                    NodeStore.put(block, blob);
                    updateDisplay();
                    player.sendMessage(Text.msg("Stored item set to: " + blob.cellSample.getType().name(), NamedTextColor.GREEN));
                }
                return;
            }
            ClickType click = event.getClick();
            if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
                extractToInventory(blob, Integer.MAX_VALUE);
            } else if (click == ClickType.RIGHT) {
                extractToCursorOrInventory(blob, Math.min(blob.cellSample.getMaxStackSize(), 64), event);
            } else {
                extractToCursorOrInventory(blob, 1, event);
            }
            updateDisplay();
            return;
        }

        if (raw >= inv.getSize() && (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT)) {
            ItemStack mover = event.getCurrentItem();
            if (mover == null || mover.getType().isAir()) {
                return;
            }
            int playerSlot = playerInventorySlot(event);
            ItemStack clone = mover.clone();
            if (absorbIntoCell(blob, clone)) {
                if (clone.getAmount() <= 0) {
                    player.getInventory().setItem(playerSlot, null);
                } else {
                    player.getInventory().setItem(playerSlot, clone);
                }
                updateDisplay();
                player.updateInventory();
            }
        }
    }

    private void performQuickDeposit(NodeBlob blob) {
        if (blob.cellSample == null) {
            player.sendMessage(Text.msg("Set an item type before using Quick Deposit.", NamedTextColor.YELLOW));
            return;
        }
        PlayerInventory pInv = player.getInventory();
        long cap = Items.capacityOf(type);
        int deposited = 0;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = pInv.getItem(i);
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            if (StackUtils.itemsMatch(blob.cellSample, stack)) {
                long space = cap - blob.cellAmount;
                if (space <= 0) {
                    break;
                }
                int take = (int) Math.min(space, stack.getAmount());
                blob.cellAmount += take;
                deposited += take;
                stack.setAmount(stack.getAmount() - take);
                if (stack.getAmount() <= 0) {
                    pInv.setItem(i, null);
                } else {
                    pInv.setItem(i, stack);
                }
            }
        }

        if (deposited > 0) {
            NodeStore.put(block, blob);
            player.sendMessage(Text.msg("Deposited " + Items.formatAmount(deposited) + " items.", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Text.msg("No matching items to deposit (or cell is full).", NamedTextColor.YELLOW));
        }
    }

    private void performQuickExtract(NodeBlob blob, ClickType click) {
        if (blob.cellSample == null || blob.cellAmount <= 0) {
            player.sendMessage(Text.msg("The cell is empty.", NamedTextColor.YELLOW));
            return;
        }
        if (click == ClickType.SHIFT_RIGHT || click == ClickType.SHIFT_LEFT) {
            extractToInventory(blob, Math.min(blob.cellSample.getMaxStackSize(), 64));
        } else if (click == ClickType.RIGHT) {
            extractToInventory(blob, 1);
        } else {
            extractToInventory(blob, Integer.MAX_VALUE);
        }
    }

    private void extractToInventory(NodeBlob blob, int maxWant) {
        if (blob.cellSample == null || blob.cellAmount <= 0) {
            return;
        }
        int maxStack = blob.cellSample.getMaxStackSize();
        int totalTaken = 0;
        PlayerInventory pInv = player.getInventory();

        for (int i = 0; i < 36 && totalTaken < maxWant && blob.cellAmount > 0; i++) {
            ItemStack slotItem = pInv.getItem(i);
            if (slotItem == null || slotItem.getType().isAir()) {
                int toGive = (int) Math.min(Math.min(maxStack, maxWant - totalTaken), blob.cellAmount);
                ItemStack stack = StackUtils.getAsQuantity(blob.cellSample, toGive);
                pInv.setItem(i, stack);
                blob.cellAmount -= toGive;
                totalTaken += toGive;
            } else if (StackUtils.itemsMatch(blob.cellSample, slotItem) && slotItem.getAmount() < maxStack) {
                int room = maxStack - slotItem.getAmount();
                int toGive = (int) Math.min(Math.min(room, maxWant - totalTaken), blob.cellAmount);
                slotItem.setAmount(slotItem.getAmount() + toGive);
                pInv.setItem(i, slotItem);
                blob.cellAmount -= toGive;
                totalTaken += toGive;
            }
        }

        if (blob.cellAmount <= 0) {
            blob.cellAmount = 0;
            blob.cellSample = null;
        }
        NodeStore.put(block, blob);
        player.updateInventory();
    }

    private void extractToCursorOrInventory(NodeBlob blob, int want, InventoryClickEvent event) {
        if (blob.cellSample == null || blob.cellAmount <= 0) {
            return;
        }
        var view = event.getView();
        ItemStack cursor = view.getCursor();
        int maxStack = blob.cellSample.getMaxStackSize();

        if (cursor == null || cursor.getType().isAir()) {
            int toTake = (int) Math.min(Math.min(want, maxStack), blob.cellAmount);
            ItemStack stack = StackUtils.getAsQuantity(blob.cellSample, toTake);
            blob.cellAmount -= toTake;
            view.setCursor(stack);
        } else if (StackUtils.itemsMatch(blob.cellSample, cursor) && cursor.getAmount() < maxStack) {
            int room = maxStack - cursor.getAmount();
            int toTake = (int) Math.min(Math.min(want, room), blob.cellAmount);
            cursor.setAmount(cursor.getAmount() + toTake);
            view.setCursor(cursor);
            blob.cellAmount -= toTake;
        } else {
            extractToInventory(blob, want);
            return;
        }

        if (blob.cellAmount <= 0) {
            blob.cellAmount = 0;
            blob.cellSample = null;
        }
        NodeStore.put(block, blob);
        player.updateInventory();
    }

    @Override
    protected void onClose(InventoryCloseEvent event) {
    }

    private ItemStack panel(Material material, String name) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}

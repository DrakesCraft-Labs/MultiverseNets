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
 * Menú de Quantum Storage Cell:
 *
 *   [ fondo ][ fondo ][ fondo ][ fondo ][DISPLAY][ fondo ][ fondo ][ fondo ][ fondo ]
 *   [ fondo ][ fondo ][DEPOSIT][ fondo ][SET ITEM][ fondo ][EXTRACT][ fondo ][ fondo ]
 *
 *   - DISPLAY (4): ítem guardado con monto y porcentaje; clic izquierdo saca 1, derecho 64, shift llena el inventario.
 *   - SET ITEM (13): registra el tipo con el cursor cuando está vacía; shift alterna vaciado (void).
 *   - QUICK DEPOSIT (11): deposita automáticamente todos los ítems coincidentes del inventario.
 *   - QUICK EXTRACT (15): atajos rápidos para retirar ítems.
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
        ItemStack fondo = panel(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot : BACKGROUND_SLOTS) {
            inv.setItem(slot, fondo);
        }

        ItemStack setItem = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        var metaSet = setItem.getItemMeta();
        metaSet.displayName(Component.text("Set Item", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        metaSet.lore(List.of(
                passivo("Click with an item on your cursor to register it."),
                passivo("Only works while the cell is empty."),
                Component.empty(),
                Component.text("Shift+Click: Toggle void excess", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)));
        setItem.setItemMeta(metaSet);
        inv.setItem(SET_SLOT, setItem);

        ItemStack depositAll = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
        var metaDep = depositAll.getItemMeta();
        metaDep.displayName(Component.text("Quick Deposit", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        metaDep.lore(List.of(
                passivo("Click to deposit all matching items"),
                passivo("from your inventory into this cell.")));
        depositAll.setItemMeta(metaDep);
        inv.setItem(DEPOSIT_ALL_SLOT, depositAll);

        ItemStack extractAll = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        var metaExt = extractAll.getItemMeta();
        metaExt.displayName(Component.text("Quick Take Out", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        metaExt.lore(List.of(
                passivo("Left Click: Fill inventory with stored items"),
                passivo("Right Click: Take 1 item"),
                passivo("Shift+Right Click: Take 64 items (1 stack)")));
        extractAll.setItemMeta(metaExt);
        inv.setItem(EXTRACT_ALL_SLOT, extractAll);

        actualizarDisplay();
    }

    private Component passivo(String texto) {
        return Component.text(texto, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
    }

    private void actualizarDisplay() {
        NodeBlob blob = NodeStore.get(block);
        long cap = Items.capacityOf(type);
        ItemStack icono;
        if (blob == null || blob.cellSample == null || blob.cellAmount <= 0) {
            icono = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            var meta = icono.getItemMeta();
            meta.displayName(Component.text("No Registered Item", NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Capacity: " + Items.formatAmount(cap), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Stores a single item type", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Click with item on cursor to set", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)));
            icono.setItemMeta(meta);
        } else {
            icono = blob.cellSample.clone();
            icono.setAmount(1);
            var meta = icono.getItemMeta();
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
            icono.setItemMeta(meta);
        }
        inv.setItem(ITEM_SLOT, icono);
    }

    private boolean absorberEnCelda(NodeBlob blob, ItemStack stack) {
        if (stack == null || stack.getType().isAir() || stack.getAmount() <= 0) {
            return false;
        }
        long cap = Items.capacityOf(type);
        if (blob.cellSample != null && !StackUtils.itemsMatch(blob.cellSample, stack)) {
            return false;
        }
        long espacio = cap - blob.cellAmount;
        if (espacio <= 0) {
            return false;
        }
        int tomar = (int) Math.min(Math.min(espacio, stack.getAmount()), Integer.MAX_VALUE);
        if (tomar <= 0) {
            return false;
        }
        if (blob.cellSample == null) {
            blob.cellSample = StackUtils.getAsQuantity(stack, 1);
        }
        blob.cellAmount += tomar;
        stack.setAmount(stack.getAmount() - tomar);
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

        // 1) Hueco SET_SLOT (13)
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
            actualizarDisplay();
            player.sendMessage(Text.msg("Stored item set to: " + blob.cellSample.getType().name(), NamedTextColor.GREEN));
            return;
        }

        // 2) Hueco QUICK DEPOSIT (11)
        if (raw == DEPOSIT_ALL_SLOT) {
            realizarQuickDeposit(blob);
            actualizarDisplay();
            return;
        }

        // 3) Hueco QUICK EXTRACT (15)
        if (raw == EXTRACT_ALL_SLOT) {
            realizarQuickExtract(blob, event.getClick());
            actualizarDisplay();
            return;
        }

        // 4) Hueco DISPLAY / ITEM_SLOT (4)
        if (raw == ITEM_SLOT) {
            if (blob.cellSample == null || blob.cellAmount <= 0) {
                ItemStack cursor = event.getView().getCursor();
                if (cursor != null && !cursor.getType().isAir()) {
                    blob.cellSample = StackUtils.getAsQuantity(cursor, 1);
                    NodeStore.put(block, blob);
                    actualizarDisplay();
                    player.sendMessage(Text.msg("Stored item set to: " + blob.cellSample.getType().name(), NamedTextColor.GREEN));
                }
                return;
            }
            ClickType click = event.getClick();
            if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
                extraerHaciaInventario(blob, Integer.MAX_VALUE);
            } else if (click == ClickType.RIGHT) {
                extraerHaciaCursorOInventario(blob, Math.min(blob.cellSample.getMaxStackSize(), 64), event);
            } else {
                extraerHaciaCursorOInventario(blob, 1, event);
            }
            actualizarDisplay();
            return;
        }

        // 5) Shift-click desde el inventario del jugador hacia la celda
        if (raw >= inv.getSize() && (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT)) {
            ItemStack mover = event.getCurrentItem();
            if (mover == null || mover.getType().isAir()) {
                return;
            }
            int slotJugador = slotInventarioJugador(event);
            ItemStack clon = mover.clone();
            if (absorberEnCelda(blob, clon)) {
                if (clon.getAmount() <= 0) {
                    player.getInventory().setItem(slotJugador, null);
                } else {
                    player.getInventory().setItem(slotJugador, clon);
                }
                actualizarDisplay();
            }
        }
    }

    private void realizarQuickDeposit(NodeBlob blob) {
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

    private void realizarQuickExtract(NodeBlob blob, ClickType click) {
        if (blob.cellSample == null || blob.cellAmount <= 0) {
            player.sendMessage(Text.msg("The cell is empty.", NamedTextColor.YELLOW));
            return;
        }
        if (click == ClickType.SHIFT_RIGHT || click == ClickType.SHIFT_LEFT) {
            extraerHaciaInventario(blob, Math.min(blob.cellSample.getMaxStackSize(), 64));
        } else if (click == ClickType.RIGHT) {
            extraerHaciaInventario(blob, 1);
        } else {
            extraerHaciaInventario(blob, Integer.MAX_VALUE);
        }
    }

    private void extraerHaciaInventario(NodeBlob blob, int maxWant) {
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
    }

    private void extraerHaciaCursorOInventario(NodeBlob blob, int want, InventoryClickEvent event) {
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
            blob.cellAmount -= toTake;
        } else {
            extraerHaciaInventario(blob, want);
            return;
        }

        if (blob.cellAmount <= 0) {
            blob.cellAmount = 0;
            blob.cellSample = null;
        }
        NodeStore.put(block, blob);
    }

    @Override
    protected void onClose(InventoryCloseEvent event) {
        // No hay slots físicos vanilla en el menú
    }

    private ItemStack panel(Material material, String nombre) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(nombre, NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}

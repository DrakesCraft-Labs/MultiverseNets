package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Set;

/**
 * Calco del menu de un Quantum Storage de Networks (NetworkQuantumStorage):
 *
 *   [ Input ][ENTRADA][ Input ][ Item ][DISPLAY][ Item ][ Output][SALIDA][ Output ]
 *   [ fondo ][ fondo ][ fondo ][ fondo ][ SET ITEM ][ fondo ][ fondo ][ fondo ][ fondo ]
 *
 *   - ENTRADA (1): hueco real; la celda absorbe lo que pongas ahi si coincide con su tipo.
 *   - DISPLAY (4): el item guardado con su monto; en Networks es el ITEM_SLOT.
 *   - SALIDA (7): hueco real que la celda rellena sola hasta un stack, como el OUTPUT_SLOT.
 *   - SET ITEM (13): registra el tipo con un item en el cursor, solo con la celda vacia.
 */
public class CellMenu extends MenuHolder {

    private static final int INPUT_SLOT = 1;
    private static final int ITEM_SLOT = 4;
    private static final int OUTPUT_SLOT = 7;
    private static final int SET_SLOT = 13;

    private static final int[] FONDO_SLOTS = {9, 10, 11, 12, 14, 15, 16, 17};

    private final Block block;
    private final DeviceType type;
    private BukkitTask tarea;

    public CellMenu(MultiverseNets plugin, Player player, Block block, DeviceType type) {
        super(plugin, player);
        this.block = block;
        this.type = type;
    }

    public void openMenu() {
        open(27, Component.text(type.display(), NamedTextColor.DARK_AQUA)
                .decoration(TextDecoration.ITALIC, false));
        // Mismo rol del BlockTicker del Quantum Storage: mover entrada y salida por ticks.
        tarea = plugin.getServer().getScheduler().runTaskTimer(plugin, this::moverPorTick, 5L, 5L);
    }

    @Override
    protected Set<Integer> vanillaSlots() {
        return Set.of(INPUT_SLOT, OUTPUT_SLOT);
    }

    @Override
    protected void draw() {
        ItemStack entradaFondo = panel(Material.GREEN_STAINED_GLASS_PANE, "Input");
        inv.setItem(0, entradaFondo);
        inv.setItem(2, entradaFondo);

        ItemStack itemFondo = panel(Material.BLUE_STAINED_GLASS_PANE, "Item Stored");
        inv.setItem(3, itemFondo);
        inv.setItem(5, itemFondo);

        ItemStack salidaFondo = panel(Material.ORANGE_STAINED_GLASS_PANE, "Output");
        inv.setItem(6, salidaFondo);
        inv.setItem(8, salidaFondo);

        for (int slot : FONDO_SLOTS) {
            inv.setItem(slot, panel(Material.GRAY_STAINED_GLASS_PANE, " "));
        }

        ItemStack setItem = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        var metaSet = setItem.getItemMeta();
        metaSet.displayName(Component.text("Set Item", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        metaSet.lore(List.of(
                passivo("Drag an item on top of this pane to register it."),
                passivo("Only works while the cell is empty.")));
        setItem.setItemMeta(metaSet);
        inv.setItem(SET_SLOT, setItem);

        actualizarDisplay();
    }

    private Component passivo(String texto) {
        return Component.text(texto, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
    }

    /** Solo reemplaza el icono central: no toca los huecos vanilla ni fuerza redraw completo. */
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
                    Component.text("Stores a single item type", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            icono.setItemMeta(meta);
        } else {
            icono = blob.cellSample.clone();
            icono.setAmount(1);
            var meta = icono.getItemMeta();
            meta.lore(List.of(
                    Component.empty(),
                    Component.text("Amount: " + blob.cellAmount + " (" + Items.formatAmount(blob.cellAmount) + ")",
                            NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Usage: " + (blob.cellAmount * 100 / cap) + "%", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)));
            icono.setItemMeta(meta);
        }
        inv.setItem(ITEM_SLOT, icono);
    }

    /**
     * Entrada y salida por ticks, igual que onTick en Networks: absorbe lo que entra y rellena
     * la salida hasta un stack.
     */
    private void moverPorTick() {
        if (inv == null || player.getOpenInventory().getTopInventory().getHolder() != this) {
            if (tarea != null) {
                tarea.cancel();
                tarea = null;
            }
            return;
        }
        NodeBlob blob = NodeStore.get(block);
        if (blob == null) {
            return;
        }
        long cap = Items.capacityOf(type);
        boolean cambio = false;

        ItemStack entrada = inv.getItem(INPUT_SLOT);
        if (entrada != null && !entrada.getType().isAir() && cap > 0
                && (blob.cellSample == null || entrada.isSimilar(blob.cellSample))
                && blob.cellAmount < cap) {
            long espacio = cap - blob.cellAmount;
            int tomar = (int) Math.min(Math.min(espacio, entrada.getAmount()), Integer.MAX_VALUE);
            if (tomar > 0) {
                if (blob.cellSample == null) {
                    blob.cellSample = entrada.clone();
                    blob.cellSample.setAmount(1);
                }
                blob.cellAmount += tomar;
                int sobra = entrada.getAmount() - tomar;
                if (sobra <= 0) {
                    inv.setItem(INPUT_SLOT, null);
                } else {
                    entrada.setAmount(sobra);
                }
                NodeStore.put(block, blob);
                cambio = true;
            }
        }

        if (blob.cellSample != null && blob.cellAmount > 0) {
            ItemStack salida = inv.getItem(OUTPUT_SLOT);
            if (salida == null || salida.getType().isAir()) {
                int dar = (int) Math.min(blob.cellSample.getMaxStackSize(), blob.cellAmount);
                if (dar > 0) {
                    ItemStack stack = blob.cellSample.clone();
                    stack.setAmount(dar);
                    inv.setItem(OUTPUT_SLOT, stack);
                    descontar(blob, dar);
                    NodeStore.put(block, blob);
                    cambio = true;
                }
            } else if (salida.isSimilar(blob.cellSample) && salida.getAmount() < salida.getMaxStackSize()) {
                int caben = salida.getMaxStackSize() - salida.getAmount();
                int dar = (int) Math.min(caben, blob.cellAmount);
                if (dar > 0) {
                    salida.setAmount(salida.getAmount() + dar);
                    descontar(blob, dar);
                    NodeStore.put(block, blob);
                    cambio = true;
                }
            }
        }

        if (cambio) {
            actualizarDisplay();
        }
    }

    @Override
    protected void click(InventoryClickEvent event) {
        if (event.getRawSlot() != SET_SLOT) {
            return;
        }
        NodeBlob blob = NodeStore.get(block);
        if (blob == null) {
            return;
        }
        if (blob.cellSample != null && blob.cellAmount > 0) {
            player.sendMessage(Text.msg("The cell must be empty before changing the stored item.",
                    NamedTextColor.RED));
            return;
        }
        ItemStack cursor = event.getView().getCursor();
        if (cursor == null || cursor.getType().isAir()) {
            return;
        }
        blob.cellSample = cursor.clone();
        blob.cellSample.setAmount(1);
        NodeStore.put(block, blob);
        actualizarDisplay();
    }

    private void descontar(NodeBlob blob, int cuantos) {
        blob.cellAmount -= cuantos;
        if (blob.cellAmount <= 0) {
            blob.cellAmount = 0;
            blob.cellSample = null;
        }
    }

    private ItemStack panel(Material material, String nombre) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(nombre, material == Material.LIME_STAINED_GLASS_PANE
                ? NamedTextColor.GREEN : NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}

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
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Set;

/**
 * Calco del menu de un Quantum Storage de Networks (NetworkQuantumStorage):
 *
 *   Dos filas (18 huecos), suficiente para lo unico que hace una celda:
 *
 *   [ Input ][ENTRADA][ Input ][ Item ][DISPLAY][ Item ][ Output][SALIDA][ Output ]
 *   [ fondo ][ fondo ][ fondo ][ fondo ][ SET ITEM ][ fondo ][ fondo ][ fondo ][ fondo ]
 *
 *   - ENTRADA (1): hueco real; la celda absorbe lo que pongas ahi si coincide con su tipo.
 *   - DISPLAY (4): el item guardado con su monto; en Networks es el ITEM_SLOT.
 *   - SALIDA (7): hueco real que la celda rellena sola hasta un stack, como el OUTPUT_SLOT.
 *   - SET ITEM (13): registra el tipo con un item en el cursor, solo con la celda vacia.
 *
 * Al cerrar, lo que quede en ENTRADA se absorbe a la celda y lo que no quepa (o lo que quede en
 * SALIDA) vuelve al jugador. Antes de este cambio esos items se evaporaban al cerrar.
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
        // 18 huecos (2 filas): las tres de antes sobraban enteras.
        open(18, Component.text(type.display(), NamedTextColor.DARK_AQUA)
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
                passivo("Click with an item on your cursor to register it."),
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
            cancelarTarea();
            return;
        }
        NodeBlob blob = NodeStore.get(block);
        if (blob == null) {
            cancelarTarea();
            return;
        }
        boolean cambio = moverEntrada(blob) | moverSalida(blob);

        if (cambio) {
            actualizarDisplay();
        }
    }

    /** Absorbe del hueco de entrada; devuelve si cambio algo. */
    private boolean moverEntrada(NodeBlob blob) {
        ItemStack entrada = inv.getItem(INPUT_SLOT);
        if (entrada == null || entrada.getType().isAir()) {
            return false;
        }
        if (absorberEnCelda(blob, entrada)) {
            if (entrada.getAmount() <= 0) {
                inv.setItem(INPUT_SLOT, null);
            }
            return true;
        }
        return false;
    }

    /**
     * Mete a la celda lo que se pueda del stack dado (que se queda mutado con el sobrante).
     * Devuelve true si entro algo.
     */
    private boolean absorberEnCelda(NodeBlob blob, ItemStack stack) {
        long cap = Items.capacityOf(type);
        if (blob.cellSample != null && !stack.isSimilar(blob.cellSample)) {
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
            blob.cellSample = stack.clone();
            blob.cellSample.setAmount(1);
        }
        blob.cellAmount += tomar;
        stack.setAmount(stack.getAmount() - tomar);
        NodeStore.put(block, blob);
        return true;
    }

    /** Rellena el hueco de salida hasta un stack; devuelve si cambio algo. */
    private boolean moverSalida(NodeBlob blob) {
        if (blob.cellSample == null || blob.cellAmount <= 0) {
            return false;
        }
        ItemStack salida = inv.getItem(OUTPUT_SLOT);
        if (salida == null || salida.getType().isAir()) {
            int dar = (int) Math.min(blob.cellSample.getMaxStackSize(), blob.cellAmount);
            if (dar <= 0) {
                return false;
            }
            ItemStack stack = blob.cellSample.clone();
            stack.setAmount(dar);
            inv.setItem(OUTPUT_SLOT, stack);
            descontar(blob, dar);
            NodeStore.put(block, blob);
            return true;
        }
        if (salida.isSimilar(blob.cellSample) && salida.getAmount() < salida.getMaxStackSize()) {
            int caben = salida.getMaxStackSize() - salida.getAmount();
            int dar = (int) Math.min(caben, blob.cellAmount);
            if (dar <= 0) {
                return false;
            }
            salida.setAmount(salida.getAmount() + dar);
            descontar(blob, dar);
            NodeStore.put(block, blob);
            return true;
        }
        return false;
    }

    @Override
    protected void click(InventoryClickEvent event) {
        int raw = event.getRawSlot();
        if (raw == SET_SLOT || raw == ITEM_SLOT) {
            // Tanto el boton "Set Item" como el propio display fijan el tipo si esta vacio:
            // el display es lo primero que el jugador intenta clicar con el item en mano.
            NodeBlob blob = NodeStore.get(block);
            if (blob == null) {
                return;
            }
            if (blob.cellSample != null && blob.cellAmount > 0) {
                player.sendMessage(Text.msg("The cell holds " + Items.formatAmount(blob.cellAmount)
                        + ". Empty it (Output slot) before changing the stored item.", NamedTextColor.RED));
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
            return;
        }
        // Shift sobre el inventario propio: lleva el stack al hueco de entrada si cupiera,
        // para no obligar a arrastrar a mano (la celda de Networks acepta el gesto igual).
        if (raw >= inv.getSize()
                && (event.getClick() == org.bukkit.event.inventory.ClickType.SHIFT_LEFT
                || event.getClick() == org.bukkit.event.inventory.ClickType.SHIFT_RIGHT)) {
            ItemStack mover = event.getCurrentItem();
            ItemStack entrada = inv.getItem(INPUT_SLOT);
            if (mover == null || mover.getType().isAir()
                    || (entrada != null && !entrada.getType().isAir() && !entrada.isSimilar(mover))) {
                return;
            }
            int slotJugador = slotInventarioJugador(event);
            if (entrada == null || entrada.getType().isAir()) {
                inv.setItem(INPUT_SLOT, mover);
                player.getInventory().setItem(slotJugador, null);
            } else {
                int pasan = Math.min(entrada.getMaxStackSize() - entrada.getAmount(), mover.getAmount());
                if (pasan <= 0) {
                    return;
                }
                entrada.setAmount(entrada.getAmount() + pasan);
                mover.setAmount(mover.getAmount() - pasan);
                if (mover.getAmount() <= 0) {
                    player.getInventory().setItem(slotJugador, null);
                }
            }
        }
    }

    /**
     * Recuperacion al cerrar: ENTRADA y SALIDA intentan volver PRIMERO a la celda.
     *
     * La salida es el caso delicado: mientras el menu esta abierto el slot 7 se re-llena solo
     * desde el almacen (como el OUTPUT_SLOT de NetworksV6, que pertenece al bloque y conserva
     * su contenido al cerrar). Si aqui se la dieramos al jugador, cada cierre le regalaria un
     * stack extraido gratis — y al usuario le pareceria que "la celda le escupe items en vez de
     * guardarlos". Solo vuelve al jugador lo que la celda no puede tragar (tipo distinto metido
     * a mano, o celda llena).
     */
    @Override
    protected void onClose(InventoryCloseEvent event) {
        cancelarTarea();
        NodeBlob blob = NodeStore.get(block);
        for (int slot : new int[]{INPUT_SLOT, OUTPUT_SLOT}) {
            ItemStack contenido = inv.getItem(slot);
            if (contenido == null || contenido.getType().isAir()) {
                continue;
            }
            inv.setItem(slot, null);
            if (blob != null) {
                absorberEnCelda(blob, contenido);
            }
            if (contenido.getAmount() > 0) {
                devolverAlJugador(contenido);
            }
        }
    }

    private void cancelarTarea() {
        if (tarea != null) {
            tarea.cancel();
            tarea = null;
        }
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

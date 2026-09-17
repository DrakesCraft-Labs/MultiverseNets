package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

/**
 * Guardia de los menus del plugin. Replica el GridDupeGuardListener de NetworksV6, sin el cual
 * los menus son un colador de dupes:
 *
 *   - DOUBLE_CLICK / COLLECT_TO_CURSOR: juntaria en el cursor los iconos pintados del display
 *     (que son items reales con lore "Amount:" y la marca en el PDC). Prohibido en toda la vista.
 *   - MIDDLE (pick-block creativo): clonaria los iconos. Prohibido.
 *   - NUMBER_KEY / SWAP_OFFHAND sobre el inventario superior: intercambian items de verdad con
 *     los huecos pintados. Prohibido salvo en los huecos vanilla del menu.
 *   - SHIFT desde el inventario del jugador: shift+izquierdo lo enruta el menu (inserta a la
 *     red); shift+DERECHO de vanilla lo moveria al inventario superior y perderia el item al
 *     refrescar: prohibido.
 *   - Drags que toquen huecos no vanilla: prohibidos.
 *
 * Y al cerrar, se avisa al menu para que recupere lo que quede en sus huecos vanilla.
 */
public class GuiListener implements Listener {

    private final MultiverseNets plugin;

    public GuiListener(MultiverseNets plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof MenuHolder menu)) {
            return;
        }

        ClickType type = event.getClick();
        InventoryAction action = event.getAction();

        // Prohibiciones tomadas literalmente del guard de NetworksV6 (#230): son gestos que
        // operan sobre la vista entera saltandose los handlers por slot.
        if (type == ClickType.DOUBLE_CLICK || type == ClickType.MIDDLE
                || type == ClickType.CREATIVE
                || action == InventoryAction.COLLECT_TO_CURSOR
                || action == InventoryAction.HOTBAR_MOVE_AND_READD
                || action == InventoryAction.HOTBAR_SWAP
                || action == InventoryAction.DROP_ALL_CURSOR
                || action == InventoryAction.DROP_ALL_SLOT) {
            event.setCancelled(true);
            return;
        }

        boolean esTop = event.getClickedInventory() != null && event.getClickedInventory().equals(top);
        if (!esTop) {
            // Inventario del jugador: LIBRE como en Networks (antes se congelaba entero y no se
            // podia ni coger un item al cursor para definir el tipo de la celda). Lo unico
            // peligroso es el shift, que saltaria a los huecos pintados del menu: se cancela y
            // se deja en manos del menu, que decide adonde va ese stack.
            if (type == ClickType.SHIFT_LEFT || type == ClickType.SHIFT_RIGHT) {
                event.setCancelled(true);
                if (!ChatPrompts.isPending((Player) event.getWhoClicked())) {
                    menu.click(event);
                }
            }
            return;
        }

        // Clic sobre el inventario superior.
        if (menu.vanillaSlots().contains(event.getRawSlot())) {
            return;
        }
        event.setCancelled(true);
        if (ChatPrompts.isPending((Player) event.getWhoClicked())) {
            return;
        }
        menu.click(event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder menu)) {
            return;
        }
        java.util.Set<Integer> vanilla = menu.vanillaSlots();
        int topSize = event.getView().getTopInventory().getSize();
        for (int raw : event.getRawSlots()) {
            if (raw < topSize && !vanilla.contains(raw)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof MenuHolder menu) {
            menu.onClose(event);
        }
    }
}

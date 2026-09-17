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

        // Prohibiciones tomadas del guard de NetworksV6: gestos que
        // operan sobre la vista entera o atajos de teclado saltándose los handlers por slot.
        if (type == ClickType.DOUBLE_CLICK
                || type == ClickType.MIDDLE
                || type == ClickType.NUMBER_KEY
                || type == ClickType.SWAP_OFFHAND
                || type == ClickType.DROP
                || type == ClickType.CONTROL_DROP
                || type == ClickType.CREATIVE
                || action == InventoryAction.COLLECT_TO_CURSOR
                || action == InventoryAction.HOTBAR_MOVE_AND_READD
                || action == InventoryAction.HOTBAR_SWAP
                || action == InventoryAction.DROP_ALL_CURSOR
                || action == InventoryAction.DROP_ALL_SLOT
                || action == InventoryAction.DROP_ONE_CURSOR
                || action == InventoryAction.DROP_ONE_SLOT
                || action == InventoryAction.CLONE_STACK
                || action == InventoryAction.UNKNOWN) {
            event.setCancelled(true);
            return;
        }

        boolean esTop = event.getClickedInventory() != null && event.getClickedInventory().equals(top);
        if (!esTop) {
            // Inventario del jugador: libre para mover ítems dentro de su inventario,
            // excepto shift-clicks que intenten saltar al inventario superior:
            // se cancelan y se delegan al menú para procesarlos de forma segura.
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

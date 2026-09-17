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
 * Inventory security listener guarding against duplication glitches, item cloning, and invalid clicks in custom GUIs.
 *
 * Listener de seguridad de inventarios que previene errores de duplicación (dupes), clonado y clics inválidos en GUIs.
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

        boolean isTop = event.getClickedInventory() != null && event.getClickedInventory().equals(top);
        if (!isTop) {
            if (type == ClickType.SHIFT_LEFT || type == ClickType.SHIFT_RIGHT) {
                event.setCancelled(true);
                if (!ChatPrompts.isPending((Player) event.getWhoClicked())) {
                    menu.click(event);
                }
            }
            return;
        }

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

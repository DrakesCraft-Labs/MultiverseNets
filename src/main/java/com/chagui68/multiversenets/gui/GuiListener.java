package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public class GuiListener implements Listener {

    private final MultiverseNets plugin;

    public GuiListener(MultiverseNets plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder menu)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        boolean esTop = event.getClickedInventory() != null && event.getClickedInventory().equals(top);

        if (!esTop) {
            if (menu.cancelarClicsJugador()) {
                event.setCancelled(true);
                if (ChatPrompts.isPending((Player) event.getWhoClicked())) {
                    return;
                }
                menu.click(event);
                return;
            }
            // Modelo de la grilla de Networks: shift+izquierdo inserta el stack clicado y
            // cualquier otro clic sobre el inventario propio queda vanilla.
            if (event.getClick() == ClickType.SHIFT_LEFT) {
                event.setCancelled(true);
                if (ChatPrompts.isPending((Player) event.getWhoClicked())) {
                    return;
                }
                menu.click(event);
            }
            return;
        }

        java.util.Set<Integer> vanilla = menu.vanillaSlots();
        if (vanilla.contains(event.getRawSlot())) {
            return;
        }
        event.setCancelled(true);
        if (ChatPrompts.isPending((Player) event.getWhoClicked())) {
            return;
        }
        menu.click(event);
    }

    @EventHandler
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
}

package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public abstract class MenuHolder implements InventoryHolder {

    protected final MultiverseNets plugin;
    protected final Player player;
    protected Inventory inv;

    protected MenuHolder(MultiverseNets plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    protected void open(int size, Component title) {
        inv = Bukkit.createInventory(this, size, title);
        draw();
        player.openInventory(inv);
    }

    protected void refresh() {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.getOpenInventory().getTopInventory().getHolder(false) == this) {
                inv.clear();
                draw();
            }
        });
    }

    protected abstract void draw();

    protected abstract void click(InventoryClickEvent event);

    /**
     * Huecos del inventario superior que quedan bajo control vanilla (colocar y sacar de
     * verdad), vacio si ninguno. La grilla usa uno de entrada y el almacen de celda usa entrada
     * y salida, igual que un Quantum Storage de Networks.
     */
    protected java.util.Set<Integer> vanillaSlots() {
        return java.util.Set.of();
    }

    /**
     * Si los clics sobre el inventario propio se bloquean y derivan a {@link #click}. Los menus
     * simples lo quieren; el terminal sigue a Networks: shift+izquierdo inserta y el resto pasa
     * vanilla.
     */
    protected boolean cancelarClicsJugador() {
        return true;
    }
}

package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
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

    /**
     * Redibuja en el siguiente tick conservando los huecos vanilla (lo que el jugador tenga
     * ahi puesto no se toca). Antes se limpiaba el inventario entero y cada menu que delegaba
     * se comia la entrada del usuario.
     */
    protected void refresh() {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.getOpenInventory().getTopInventory().getHolder(false) != this) {
                return;
            }
            java.util.Map<Integer, org.bukkit.inventory.ItemStack> conservados = new java.util.HashMap<>();
            for (int slot : vanillaSlots()) {
                org.bukkit.inventory.ItemStack it = inv.getItem(slot);
                if (it != null) {
                    conservados.put(slot, it);
                }
            }
            inv.clear();
            draw();
            conservados.forEach(inv::setItem);
        });
    }

    protected abstract void draw();

    protected abstract void click(InventoryClickEvent event);

    /**
     * Al cerrar el inventario. Las clases con huecos vanilla (entrada/salida de la celda, de la
     * terminal o del encoder) TIENEN que recuperar lo que quede dentro: sin esto, cerrar el
     * menu con items puestos los borraba del mundo.
     */
    protected void onClose(InventoryCloseEvent event) {
    }

    /**
     * Devuelve un stack al jugador: primero a su inventario y lo que no quepa se suelta a sus
     * pies. Usar siempre que un item "huérfano" de menu no tenga otro destino claro.
     */
    protected void devolverAlJugador(org.bukkit.inventory.ItemStack stack) {
        if (stack == null || stack.getType().isAir() || stack.getAmount() <= 0) {
            return;
        }
        var sobra = player.getInventory().addItem(stack);
        for (var resto : sobra.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), resto);
        }
    }

    /**
     * El indice del inventario del jugador para un click sobre su zona. Se calcula desde el raw
     * slot y NO con InventoryClickEvent.getSlot(), que para clicks al inventario inferior puede
     * devolver cualquier cosa segun la implementacion del servidor (nos lo demostro el test:
     * convertSlot daba 12 para el slot 3 y el "se limpia al insertar en la red" nunca limpiaba
     * nada: dupe en potencia).
     */
    protected static int slotInventarioJugador(InventoryClickEvent event) {
        return event.getRawSlot() - event.getView().getTopInventory().getSize();
    }

    /**
     * Huecos del inventario superior que quedan bajo control vanilla (colocar y sacar de
     * verdad), vacio si ninguno. La grilla usa uno de entrada y el almacen de celda usa entrada
     * y salida, igual que un Quantum Storage de Networks.
     */
    protected java.util.Set<Integer> vanillaSlots() {
        return java.util.Set.of();
    }
}

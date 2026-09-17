package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Base abstract class for all custom GUI menus in MultiverseNets.
 *
 * Clase base abstracta para todos los menús de interfaz gráfica (GUI) de MultiverseNets.
 */
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

    /**
     * Creates and opens the backing Bukkit inventory.
 *
     * Crea y abre el inventario Bukkit correspondiente.
     *
     * @param size Inventory slot size / Tamaño en ranuras del inventario
     * @param title Inventory title component / Componente de título del inventario
     */
    protected void open(int size, Component title) {
        inv = Bukkit.createInventory(this, size, title);
        draw();
        player.openInventory(inv);
    }

    /**
     * Redraws the menu on the next tick while preserving items in vanilla-controlled slots.
 *
     * Redibuja el menú en el siguiente tick conservando los ítems en ranuras controladas por vanilla.
     */
    protected void refresh() {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Inventory top = player.getOpenInventory().getTopInventory();
            if (inv == null || !top.equals(inv)) {
                return;
            }
            java.util.Map<Integer, org.bukkit.inventory.ItemStack> preserved = new java.util.HashMap<>();
            for (int slot : vanillaSlots()) {
                org.bukkit.inventory.ItemStack it = inv.getItem(slot);
                if (it != null) {
                    preserved.put(slot, it);
                }
            }
            inv.clear();
            draw();
            preserved.forEach(inv::setItem);
        });
    }

    /**
     * Renders items and UI buttons onto the inventory canvas.
 *
     * Renderiza ítems y botones de interfaz en el inventario.
     */
    protected abstract void draw();

    /**
     * Handles an inventory click event triggered inside this menu.
 *
     * Procesa un evento de clic en el inventario dentro de este menú.
     *
     * @param event Inventory click event / Evento de clic en inventario
     */
    protected abstract void click(InventoryClickEvent event);

    /**
     * Invoked when the player closes this inventory menu.
 *
     * Invocado cuando el jugador cierra este menú de inventario.
     *
     * @param event Inventory close event / Evento de cierre de inventario
     */
    protected void onClose(InventoryCloseEvent event) {
    }

    /**
     * Returns an item stack to the player's inventory or drops it naturally at their feet if full.
 *
     * Devuelve un stack al inventario del jugador o lo suelta a sus pies si está lleno.
     *
     * @param stack ItemStack to return / ItemStack a devolver
     */
    protected void giveOrDrop(org.bukkit.inventory.ItemStack stack) {
        if (stack == null || stack.getType().isAir() || stack.getAmount() <= 0) {
            return;
        }
        var leftover = player.getInventory().addItem(stack);
        for (var remainder : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), remainder);
        }
    }

    /** Backward compatibility alias for {@link #giveOrDrop(org.bukkit.inventory.ItemStack)} */
    protected void devolverAlJugador(org.bukkit.inventory.ItemStack stack) {
        giveOrDrop(stack);
    }

    /**
     * Computes the player's inventory slot index from an inventory click event.
     * Accurately resolves the actual clicked slot in the player's inventory (supporting hotbar 0-8
     * and storage rows 9-35) without assuming linear raw slot mapping, preventing duplication glitches.
     *
     * Calcula el índice de ranura del inventario del jugador a partir de un evento de clic.
     * Resuelve con precisión la ranura cliqueada en el inventario del jugador (soportando hotbar 0-8
     * y almacenamiento 9-35) sin asumir mapeo lineal de slots raw, previniendo errores de duplicación.
     *
     * @param event Inventory click event / Evento de clic en inventario
     * @return 0-indexed player inventory slot index / Índice de ranura del inventario del jugador (base 0)
     */
    protected static int playerInventorySlot(InventoryClickEvent event) {
        int slot = event.getSlot();
        org.bukkit.inventory.ItemStack current = event.getCurrentItem();
        if (slot >= 0 && slot < 36) {
            org.bukkit.inventory.ItemStack inSlot = event.getWhoClicked().getInventory().getItem(slot);
            if (inSlot != null && current != null && inSlot.isSimilar(current)) {
                return slot;
            }
        }
        int rawOffset = event.getRawSlot() - event.getView().getTopInventory().getSize();
        if (rawOffset >= 0 && rawOffset < 36) {
            org.bukkit.inventory.ItemStack inRaw = event.getWhoClicked().getInventory().getItem(rawOffset);
            if (inRaw != null && current != null && inRaw.isSimilar(current)) {
                return rawOffset;
            }
        }
        if (slot >= 0 && slot < 36) {
            return slot;
        }
        return Math.max(0, Math.min(35, rawOffset));
    }

    /** Backward compatibility alias for {@link #playerInventorySlot(InventoryClickEvent)} */
    protected static int slotInventarioJugador(InventoryClickEvent event) {
        return playerInventorySlot(event);
    }

    /**
     * Set of upper inventory slot indices that allow normal vanilla item placement and extraction.
 *
     * Conjunto de ranuras del inventario superior que permiten colocar y extraer ítems en modo vanilla normal.
     *
     * @return Set of slot indices / Conjunto de índices de ranuras
     */
    protected java.util.Set<Integer> vanillaSlots() {
        return java.util.Set.of();
    }
}

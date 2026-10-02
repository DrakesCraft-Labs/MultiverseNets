package com.chagui68.multiversenets.listen;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.MemoryModules;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.Keys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Resilient recipe lifecycle and crafting event listener.
 * <p>
 * Ensures MultiverseNets recipes persist across datapack/server reloads (such as Bukkit.reloadData),
 * automatically unlocks recipes in player recipe books upon join, and allows smooth Quantum Cell upgrades
 * directly in 3x3 crafting tables while preserving stored cargo.
 * </p>
 */
public class CraftingListener implements Listener {

    private final MultiverseNets plugin;

    public CraftingListener(MultiverseNets plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Re-registers recipes when the server finishes loading all plugins/datapacks or completes /reload.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerLoad(ServerLoadEvent event) {
        plugin.getLogger().info("[Recipes] ServerLoadEvent (" + event.getType() + "): Verifying and registering recipes...");
        Items.registerRecipes(plugin);
        for (Player player : Bukkit.getOnlinePlayers()) {
            Items.discoverRecipes(player);
        }
    }

    /**
     * Unlocks MultiverseNets recipes in the player's recipe book upon joining.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Items.discoverRecipes(event.getPlayer());
    }

    /**
     * EN: Checks every craft that touches MultiverseNets items.
     * <ul>
     * <li>Our recipes take devices (cells, modules, cables, fluid cells, pushers) as plain-material
     * ingredients; the result is only kept when each of those slots really holds the device the
     * recipe asks for, and no other device is in the grid.</li>
     * <li>A cell or memory-module upgrade keeps the cargo of the ingredient. Any other recipe
     * refuses an ingredient that still stores something, so nothing is crafted away.</li>
     * <li>Other recipes (vanilla or other plugins) never use a device as a plain item: a Wireless
     * Terminal is a Nether Star and an L1 module is a copper ingot, and both used to be eaten by
     * vanilla recipes.</li>
     * </ul>
     * The result is only ever set when the server matched a recipe. Setting one without a recipe
     * (the old cargo-cell upgrade) handed the result out without consuming the ingredients.
     *
     * ES: Revisa cada crafteo que toca ítems de MultiverseNets.
     * <ul>
     * <li>Nuestras recetas usan dispositivos (celdas, módulos, cables, celdas de fluidos, pushers)
     * como ingredientes de material simple; el resultado solo se mantiene si cada una de esas
     * ranuras tiene de verdad el dispositivo pedido y no hay otro dispositivo en la mesa.</li>
     * <li>Mejorar una celda o un módulo de memoria conserva la carga del ingrediente. Cualquier otra
     * receta rechaza un ingrediente que aún guarda algo, así no se pierde nada al craftear.</li>
     * <li>Las demás recetas (vanilla u otros plugins) nunca usan un dispositivo como ítem simple:
     * una Terminal Inalámbrica es una Nether Star y un módulo L1 es un lingote de cobre, y ambos se
     * consumían en recetas vanilla.</li>
     * </ul>
     * El resultado solo se pone cuando el servidor casó una receta. Ponerlo sin receta (la antigua
     * mejora de celdas con carga) entregaba el resultado sin consumir los ingredientes.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();
        ItemStack[] matrix = inv.getMatrix();
        if (matrix == null) {
            return;
        }
        Map<DeviceType, Integer> found = new EnumMap<>(DeviceType.class);
        ItemStack cargoSource = null;
        for (ItemStack ingredient : matrix) {
            DeviceType type = Items.typeOf(ingredient);
            if (type == null) {
                continue;
            }
            found.merge(type, 1, Integer::sum);
            if (cargoOf(ingredient) != null) {
                cargoSource = ingredient;
            }
        }
        Recipe recipe = event.getRecipe();
        if (recipe == null && !found.isEmpty()) {
            ItemStack[] grid = asThreeByThree(matrix);
            if (grid != null) {
                recipe = Bukkit.getCraftingRecipe(grid, event.getView().getPlayer().getWorld());
            }
        }
        if (recipe == null) {
            return;
        }
        if (!(recipe instanceof Keyed keyed) || !plugin.getName().toLowerCase(Locale.ROOT)
                .equals(keyed.getKey().getNamespace())) {
            if (!found.isEmpty()) {
                inv.setResult(null);
            }
            return;
        }
        NamespacedKey key = keyed.getKey();
        if (!found.equals(Items.deviceIngredients(key))) {
            inv.setResult(null);
            return;
        }
        if (cargoSource == null) {
            return;
        }
        if (!Items.isUpgradeRecipe(key)) {
            inv.setResult(null);
            return;
        }
        ItemStack result = inv.getResult();
        if (result == null || result.getType().isAir()) {
            result = recipe.getResult();
        }
        DeviceType resultType = Items.typeOf(result);
        if (resultType == null) {
            return;
        }
        inv.setResult(withCargo(resultType, cargoSource));
    }

    private static String cargoOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(Keys.CELL_CARGO, PersistentDataType.STRING);
    }

    /** The upgraded item, carrying the ingredient's cargo and the lore line that shows it. */
    private static ItemStack withCargo(DeviceType resultType, ItemStack source) {
        String cargo = cargoOf(source);
        NodeBlob blob = cargo == null ? null : NodeStore.decode(cargo);
        if (blob == null) {
            return Items.create(resultType);
        }
        if (resultType.isMemoryModule()) {
            return MemoryModules.moduleItem(resultType, blob);
        }
        ItemStack result = Items.create(resultType);
        var meta = result.getItemMeta();
        meta.getPersistentDataContainer().set(Keys.CELL_CARGO, PersistentDataType.STRING, cargo);
        List<Component> lore = new ArrayList<>();
        if (meta.hasLore()) {
            lore.addAll(meta.lore());
        }
        if (blob.cellSample != null && blob.cellAmount > 0) {
            lore.add(Component.text("Cargo: " + Items.formatAmount(blob.cellAmount) + " x "
                    + blob.cellSample.getType().name(), NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        result.setItemMeta(meta);
        return result;
    }

    /**
     * EN: Bukkit.getCraftingRecipe only accepts a 3x3 matrix. The player's own 2x2 grid hands us
     * 4 slots, which threw IllegalArgumentException on every craft there. The 2x2 is laid out in
     * the top-left corner of a 3x3; any other size returns null and the lookup is skipped.
     *
     * ES: Bukkit.getCraftingRecipe solo acepta una matriz 3x3. La cuadrícula 2x2 del inventario
     * entrega 4 ranuras y lanzaba IllegalArgumentException en cada crafteo. La 2x2 se coloca en
     * la esquina superior izquierda de una 3x3; cualquier otro tamaño devuelve null.
     */
    public static ItemStack[] asThreeByThree(ItemStack[] matrix) {
        if (matrix == null) {
            return null;
        }
        if (matrix.length == 9) {
            return matrix;
        }
        if (matrix.length != 4) {
            return null;
        }
        ItemStack[] grid = new ItemStack[9];
        java.util.Arrays.fill(grid, ItemStack.empty());
        grid[0] = matrix[0];
        grid[1] = matrix[1];
        grid[3] = matrix[2];
        grid[4] = matrix[3];
        return grid;
    }
}

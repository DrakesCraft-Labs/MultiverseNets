package com.chagui68.multiversenets.listen;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.Keys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

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
     * Intercepts 3x3 crafting grid preparation.
     * <p>
     * 1. Quantum Cell upgrades: If 8 Diamonds surround a Quantum Cell (T1-T5), produce the next tier cell
     *    and seamlessly transfer any stored cargo/lore from the input cell.
     * </p>
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();
        ItemStack[] matrix = inv.getMatrix();
        if (matrix == null || matrix.length != 9) {
            return;
        }

        ItemStack center = matrix[4];
        if (center == null || center.getType().isAir()) {
            return;
        }

        DeviceType centerType = Items.typeOf(center);
        if (centerType != null && centerType.isCell() && centerType.cellTier() >= 1 && centerType.cellTier() < 6) {
            boolean allDiamonds = true;
            for (int i = 0; i < 9; i++) {
                if (i == 4) {
                    continue;
                }
                ItemStack ing = matrix[i];
                if (ing == null || ing.getType() != Material.DIAMOND || ing.getAmount() < 1) {
                    allDiamonds = false;
                    break;
                }
            }

            if (allDiamonds) {
                int nextTier = centerType.cellTier() + 1;
                DeviceType nextType = DeviceType.parse("MVN_CELL_T" + nextTier);
                if (nextType != null) {
                    ItemStack result = Items.create(nextType);
                    // Preserve stored cargo if the cell had cargo inside!
                    if (center.hasItemMeta()) {
                        String cargo = center.getItemMeta().getPersistentDataContainer()
                                .get(Keys.CELL_CARGO, PersistentDataType.STRING);
                        if (cargo != null) {
                            NodeBlob blob = NodeStore.decode(cargo);
                            if (blob != null) {
                                var resMeta = result.getItemMeta();
                                resMeta.getPersistentDataContainer().set(Keys.CELL_CARGO, PersistentDataType.STRING, cargo);
                                List<Component> lore = new ArrayList<>();
                                if (resMeta.hasLore()) {
                                    lore.addAll(resMeta.lore());
                                }
                                if (blob.cellSample != null && blob.cellAmount > 0) {
                                    lore.add(Component.text("Cargo: " + Items.formatAmount(blob.cellAmount) + " x "
                                            + blob.cellSample.getType().name(), NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
                                }
                                resMeta.lore(lore);
                                result.setItemMeta(resMeta);
                            }
                        }
                    }
                    inv.setResult(result);
                }
            }
        }
    }
}

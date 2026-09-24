package com.chagui68.multiversenets;

import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.util.Keys;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.*;

class RecipeTest {

    private ServerMock server;
    private MultiverseNets plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        plugin = MockBukkit.load(MultiverseNets.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void allBasicRecipesAreRegistered() {
        String[] keys = {
                "controller", "cable", "terminal", "cell_t1", "cell_t2", "cell_t3",
                "cell_t4", "cell_t5", "cell_t6", "grabber", "pusher", "vacuum",
                "purger", "probe", "crafter", "wireless_terminal", "monitor",
                "transmitter", "receiver", "greedy_cell", "grabber_ht", "pusher_ht",
                "encoder", "crafting_grid", "blueprint", "configurator", "rake",
                "crayon", "quantum_workbench", "infinity_barrel"
        };
        for (String key : keys) {
            NamespacedKey nk = new NamespacedKey(plugin, key);
            Recipe recipe = Bukkit.getRecipe(nk);
            assertNotNull(recipe, "Recipe " + key + " should be registered in Bukkit");
        }
    }

    @Test
    void reRegisteringRecipesDoesNotThrowOrDuplicate() {
        assertDoesNotThrow(() -> Items.registerRecipes(plugin));
    }

    @Test
    void canCraftCableInMatrix() {
        ItemStack[] matrix = new ItemStack[9];
        // 8 Glass and 1 Redstone in the center
        for (int i = 0; i < 9; i++) {
            if (i == 4) {
                matrix[i] = new ItemStack(Material.REDSTONE);
            } else {
                matrix[i] = new ItemStack(Material.GLASS);
            }
        }
        Recipe recipe = Bukkit.getCraftingRecipe(matrix, Bukkit.getWorlds().get(0));
        assertNotNull(recipe, "Crafting matrix should match cable recipe");
        assertEquals(Material.GLASS, recipe.getResult().getType());
        assertEquals(16, recipe.getResult().getAmount());
    }

    @Test
    void canCraftCellT1InMatrix() {
        ItemStack[] matrix = new ItemStack[9];
        // 8 Glass and 1 Diamond in the center
        for (int i = 0; i < 9; i++) {
            if (i == 4) {
                matrix[i] = new ItemStack(Material.DIAMOND);
            } else {
                matrix[i] = new ItemStack(Material.GLASS);
            }
        }
        Recipe recipe = Bukkit.getCraftingRecipe(matrix, Bukkit.getWorlds().get(0));
        assertNotNull(recipe, "Crafting matrix should match cell_t1 recipe");
        assertEquals(Material.TERRACOTTA, recipe.getResult().getType());
    }

    @Test
    void canCraftCellT2InMatrixUsingCellT1() {
        ItemStack cellT1 = Items.create(DeviceType.MVN_CELL_T1);
        ItemStack[] matrix = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            if (i == 4) {
                matrix[i] = cellT1;
            } else {
                matrix[i] = new ItemStack(Material.DIAMOND);
            }
        }
        Recipe recipe = Bukkit.getCraftingRecipe(matrix, Bukkit.getWorlds().get(0));
        assertNotNull(recipe, "Crafting matrix should match cell_t2 recipe with exact choice");
        assertEquals(Material.ORANGE_TERRACOTTA, recipe.getResult().getType());
    }

    @Test
    void craftCellT2FailsInVanillaMatrixIfCellHasCargo() {
        ItemStack cellT1 = Items.create(DeviceType.MVN_CELL_T1);
        var meta = cellT1.getItemMeta();
        meta.getPersistentDataContainer().set(Keys.CELL_CARGO, org.bukkit.persistence.PersistentDataType.STRING, "cargo");
        cellT1.setItemMeta(meta);

        ItemStack[] matrix = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            if (i == 4) {
                matrix[i] = cellT1;
            } else {
                matrix[i] = new ItemStack(Material.DIAMOND);
            }
        }
        Recipe recipe = Bukkit.getCraftingRecipe(matrix, Bukkit.getWorlds().get(0));
        assertNull(recipe, "ExactChoice should not match a cell with cargo in vanilla matrix");
    }

    @Test
    void prepareCraftUpgradesCellWithCargoSeamlessly() {
        com.chagui68.multiversenets.persist.NodeBlob blob = com.chagui68.multiversenets.persist.NodeBlob.create(DeviceType.MVN_CELL_T1.name());
        blob.cellSample = new ItemStack(Material.EMERALD);
        blob.cellAmount = 5000L;
        String encodedCargo = com.chagui68.multiversenets.persist.NodeStore.encode(blob);

        ItemStack cellT1 = Items.create(DeviceType.MVN_CELL_T1);
        var meta = cellT1.getItemMeta();
        meta.getPersistentDataContainer().set(Keys.CELL_CARGO, org.bukkit.persistence.PersistentDataType.STRING, encodedCargo);
        cellT1.setItemMeta(meta);

        ItemStack[] matrix = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            if (i == 4) {
                matrix[i] = cellT1;
            } else {
                matrix[i] = new ItemStack(Material.DIAMOND);
            }
        }

        org.mockbukkit.mockbukkit.entity.PlayerMock player = server.addPlayer();
        org.bukkit.inventory.CraftingInventory inv = (org.bukkit.inventory.CraftingInventory) player.openWorkbench(null, true).getTopInventory();
        inv.setMatrix(matrix);

        com.chagui68.multiversenets.listen.CraftingListener listener = new com.chagui68.multiversenets.listen.CraftingListener(plugin);
        org.bukkit.event.inventory.PrepareItemCraftEvent event = new org.bukkit.event.inventory.PrepareItemCraftEvent(
                inv, player.getOpenInventory(), false
        );
        listener.onPrepareCraft(event);

        ItemStack result = inv.getResult();
        assertNotNull(result, "Result should be set by CraftingListener");
        assertEquals(Material.ORANGE_TERRACOTTA, result.getType());
        assertEquals(DeviceType.MVN_CELL_T2, Items.typeOf(result));
        assertTrue(result.hasItemMeta());
        assertEquals(encodedCargo, result.getItemMeta().getPersistentDataContainer().get(Keys.CELL_CARGO, org.bukkit.persistence.PersistentDataType.STRING));
    }
}

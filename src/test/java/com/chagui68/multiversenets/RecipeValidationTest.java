package com.chagui68.multiversenets;

import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.listen.CraftingListener;
import com.chagui68.multiversenets.net.MemoryModules;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.Keys;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EN] Recipes that take MultiverseNets devices as ingredients: only the real device counts, an
 * upgrade keeps the cargo, and devices never feed vanilla recipes.
 *
 * [ES] Recetas que usan dispositivos de MultiverseNets como ingredientes: solo cuenta el
 * dispositivo real, una mejora conserva la carga y los dispositivos nunca alimentan recetas vanilla.
 */
class RecipeValidationTest {

    private ServerMock server;
    private MultiverseNets plugin;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        plugin = MockBukkit.load(MultiverseNets.class);
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private ItemStack prepare(ItemStack[] matrix) {
        CraftingInventory inv = (CraftingInventory) server.createInventory(player, org.bukkit.event.inventory.InventoryType.WORKBENCH);
        player.openInventory(inv);
        inv.setMatrix(matrix);
        Recipe recipe = Bukkit.getCraftingRecipe(matrix, player.getWorld());
        inv.setResult(recipe == null ? null : recipe.getResult().clone());
        new CraftingListener(plugin).onPrepareCraft(new PrepareItemCraftEvent(inv, player.getOpenInventory(), false));
        return inv.getResult();
    }

    private static ItemStack[] ring(Material around, ItemStack center) {
        ItemStack[] matrix = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            matrix[i] = i == 4 ? center : new ItemStack(around);
        }
        return matrix;
    }

    @Test
    void newRecipesAreRegisteredWithTheirDeviceIngredients() {
        for (String key : new String[]{"dram_bay", "fluid_dram", "chicken_sorter"}) {
            assertNotNull(Bukkit.getRecipe(new NamespacedKey(plugin, key)), key + " is registered");
        }
        assertEquals(Map.of(DeviceType.MVN_FLUID_CELL, 2), Items.deviceIngredients(new NamespacedKey(plugin, "fluid_dram")));
        assertEquals(Map.of(DeviceType.MVN_PUSHER_HT, 1), Items.deviceIngredients(new NamespacedKey(plugin, "chicken_sorter")));
        assertEquals(Map.of(DeviceType.MVN_CELL_T1, 1), Items.deviceIngredients(new NamespacedKey(plugin, "cell_t2")));
    }

    @Test
    void aPlainMaterialCannotStandInForADevice() {
        assertNull(prepare(ring(Material.DIAMOND, new ItemStack(Material.TERRACOTTA))),
                "plain terracotta is not a Quantum Cell T1");
        ItemStack ok = prepare(ring(Material.DIAMOND, Items.create(DeviceType.MVN_CELL_T1)));
        assertEquals(DeviceType.MVN_CELL_T2, Items.typeOf(ok));
    }

    @Test
    void upgradingACellOrModuleKeepsItsCargo() {
        NodeBlob blob = NodeBlob.create(DeviceType.MVN_CELL_T1.name());
        blob.cellSample = new ItemStack(Material.EMERALD);
        blob.cellAmount = 5000L;
        String cargo = NodeStore.encode(blob);
        ItemStack cell = Items.create(DeviceType.MVN_CELL_T1);
        var meta = cell.getItemMeta();
        meta.getPersistentDataContainer().set(Keys.CELL_CARGO, PersistentDataType.STRING, cargo);
        cell.setItemMeta(meta);

        ItemStack upgraded = prepare(ring(Material.DIAMOND, cell));
        assertEquals(DeviceType.MVN_CELL_T2, Items.typeOf(upgraded), "a loaded cell matches the recipe now");
        assertEquals(cargo, upgraded.getItemMeta().getPersistentDataContainer().get(Keys.CELL_CARGO, PersistentDataType.STRING));

        NodeBlob bay = NodeBlob.create(DeviceType.MVN_DRAM_BAY.name());
        MemoryModules.install(bay, Items.create(DeviceType.MVN_CACHE_L1));
        bay.addVirtualItem(new ItemStack(Material.DIRT), 100);
        ItemStack loadedL1 = MemoryModules.eject(bay);
        ItemStack[] matrix = {
                new ItemStack(Material.GOLD_INGOT), new ItemStack(Material.LAPIS_LAZULI), new ItemStack(Material.GOLD_INGOT),
                new ItemStack(Material.LAPIS_LAZULI), loadedL1, new ItemStack(Material.LAPIS_LAZULI),
                new ItemStack(Material.GOLD_INGOT), new ItemStack(Material.LAPIS_LAZULI), new ItemStack(Material.GOLD_INGOT)};
        ItemStack l2 = prepare(matrix);
        assertEquals(DeviceType.MVN_CACHE_L2, Items.typeOf(l2));
        assertEquals(100L, MemoryModules.cargoOf(l2).totalVirtualAmount(), "the items move to the upgraded module");
    }

    @Test
    void aFluidCellWithFluidIsNotCraftedAway() {
        NodeBlob blob = NodeBlob.create(DeviceType.MVN_FLUID_CELL.name());
        blob.fluidType = "WATER";
        blob.fluidAmount = 8000;
        ItemStack full = Items.create(DeviceType.MVN_FLUID_CELL);
        var meta = full.getItemMeta();
        meta.getPersistentDataContainer().set(Keys.CELL_CARGO, PersistentDataType.STRING, NodeStore.encode(blob));
        full.setItemMeta(meta);
        ItemStack empty = Items.create(DeviceType.MVN_FLUID_CELL);

        ItemStack[] matrix = {
                new ItemStack(Material.DIAMOND), new ItemStack(Material.BUCKET), new ItemStack(Material.DIAMOND),
                empty, new ItemStack(Material.ENDER_EYE), full,
                new ItemStack(Material.DIAMOND), new ItemStack(Material.BUCKET), new ItemStack(Material.DIAMOND)};
        assertNull(prepare(matrix), "a cell that still holds fluid is refused");
        matrix[5] = Items.create(DeviceType.MVN_FLUID_CELL);
        assertEquals(DeviceType.MVN_FLUID_DRAM, Items.typeOf(prepare(matrix)));
    }

    @Test
    void devicesNeverFeedVanillaRecipes() {
        // Copper block from 9 copper ingots: an L1 module is a copper ingot underneath.
        ItemStack[] matrix = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            matrix[i] = i == 0 ? Items.create(DeviceType.MVN_CACHE_L1) : new ItemStack(Material.COPPER_INGOT);
        }
        Recipe vanilla = Bukkit.getCraftingRecipe(matrix, player.getWorld());
        if (vanilla != null) {
            assertNull(prepare(matrix), "a module is not eaten as a copper ingot");
        }
        ItemStack[] plain = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            plain[i] = new ItemStack(Material.COPPER_INGOT);
        }
        if (Bukkit.getCraftingRecipe(plain, player.getWorld()) != null) {
            assertTrue(prepare(plain) != null, "plain copper still crafts");
        }
    }
}

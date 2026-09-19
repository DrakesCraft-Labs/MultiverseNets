package com.chagui68.multiversenets;

import com.chagui68.multiversenets.gui.QuantumWorkbenchMenu;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.Keys;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.*;

/**
 * [EN] Tests Quantum Workbench crafting: upgrading storage cells while preserving stored cargo, and returning ingredients on close.
 * [ES] Pruebas de crafteo en la Mesa de Trabajo Cuántica: mejora de celdas preservando la carga, y devolución de ingredientes al cerrar.
 */
class QuantumWorkbenchTest {

    private ServerMock server;
    private MultiverseNets plugin;
    private WorldMock world;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(MultiverseNets.class);
        world = server.addSimpleWorld("world");
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    /**
     * [EN] Upgrading a Quantum Cell tier in Quantum Workbench transfers stored items and cargo safely to the upgraded cell.
     * [ES] Mejorar el nivel de una Quantum Cell en la mesa transfiere la carga y los items almacenados de forma segura a la celda mejorada.
     */
    @Test
    void upgradeQuantumStoragePreservesCargoAndTransfersItems() {
        Block benchBlock = world.getBlockAt(0, 64, 0);
        benchBlock.setType(Material.BRAIN_CORAL_BLOCK);
        NodeStore.put(benchBlock, NodeBlob.create(DeviceType.MVN_QUANTUM_WORKBENCH.name()));

        QuantumWorkbenchMenu menu = new QuantumWorkbenchMenu(plugin, player, benchBlock);
        menu.openMenu();

        // 1) Create T1 cell with pre-existing cargo (500 iron ingots)
        ItemStack cellT1 = Items.create(DeviceType.MVN_CELL_T1);
        NodeBlob blobT1 = NodeBlob.create(DeviceType.MVN_CELL_T1.name());
        blobT1.cellSample = new ItemStack(Material.IRON_INGOT);
        blobT1.cellAmount = 500;
        var metaT1 = cellT1.getItemMeta();
        metaT1.getPersistentDataContainer().set(Keys.CELL_CARGO, PersistentDataType.STRING, NodeStore.encode(blobT1));
        cellT1.setItemMeta(metaT1);

        // 2) Place T1 cell in center slot (20) and diamonds around it
        for (int slot : QuantumWorkbenchMenu.RECIPE_SLOTS) {
            if (slot == QuantumWorkbenchMenu.CENTER_SLOT) {
                player.getOpenInventory().getTopInventory().setItem(slot, cellT1);
            } else {
                player.getOpenInventory().getTopInventory().setItem(slot, new ItemStack(Material.DIAMOND, 1));
            }
        }

        // 3) Click craft button (slot 23)
        InventoryClickEvent clickCraft = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, QuantumWorkbenchMenu.CRAFT_SLOT, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(clickCraft);

        // 4) Verify output slot contains T2 cell with preserved cargo
        ItemStack result = player.getOpenInventory().getTopInventory().getItem(QuantumWorkbenchMenu.OUTPUT_SLOT);
        assertNotNull(result, "output slot must contain craft result");
        assertEquals(DeviceType.MVN_CELL_T2, Items.typeOf(result), "result must be Quantum Cell T2");

        String cargoDecoded = result.getItemMeta().getPersistentDataContainer()
                .get(Keys.CELL_CARGO, PersistentDataType.STRING);
        assertNotNull(cargoDecoded, "result must contain CELL_CARGO tag");
        NodeBlob blobT2 = NodeStore.decode(cargoDecoded);
        assertNotNull(blobT2, "cargo blob must decode cleanly");
        assertEquals(500, blobT2.cellAmount, "stored item amount must remain intact");
        assertEquals(Material.IRON_INGOT, blobT2.cellSample.getType(), "stored item sample type must remain intact");

        // 5) Verify recipe ingredients were consumed
        for (int slot : QuantumWorkbenchMenu.RECIPE_SLOTS) {
            assertNull(player.getOpenInventory().getTopInventory().getItem(slot),
                    "consumed ingredients must be removed from crafting grid");
        }
    }

    /**
     * [EN] Closing the workbench menu returns uncrafted ingredients to the player.
     * [ES] Cerrar el menú de la mesa de trabajo devuelve los ingredientes no crafteados al jugador.
     */
    @Test
    void closingMenuReturnsIngredientsToPlayer() {
        Block benchBlock = world.getBlockAt(0, 64, 0);
        benchBlock.setType(Material.BRAIN_CORAL_BLOCK);
        NodeStore.put(benchBlock, NodeBlob.create(DeviceType.MVN_QUANTUM_WORKBENCH.name()));

        QuantumWorkbenchMenu menu = new QuantumWorkbenchMenu(plugin, player, benchBlock);
        menu.openMenu();

        player.getOpenInventory().getTopInventory().setItem(QuantumWorkbenchMenu.CENTER_SLOT, new ItemStack(Material.GOLD_BLOCK, 2));
        player.closeInventory();

        assertTrue(player.getInventory().contains(Material.GOLD_BLOCK),
                "closing workbench returns placed grid items to player inventory");
    }
}

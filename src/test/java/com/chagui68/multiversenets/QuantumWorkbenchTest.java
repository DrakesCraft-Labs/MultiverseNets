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

    @Test
    void upgradeQuantumStoragePreservaCargaYTransfiereItems() {
        Block benchBlock = world.getBlockAt(0, 64, 0);
        benchBlock.setType(Material.BRAIN_CORAL_BLOCK);
        NodeStore.put(benchBlock, NodeBlob.create(DeviceType.QUANTUM_WORKBENCH.name()));

        QuantumWorkbenchMenu menu = new QuantumWorkbenchMenu(plugin, player, benchBlock);
        menu.openMenu();

        // 1) Crear celda T1 con carga previa (500 diamantes)
        ItemStack cellT1 = Items.create(DeviceType.CELL_T1);
        NodeBlob blobT1 = NodeBlob.create(DeviceType.CELL_T1.name());
        blobT1.cellSample = new ItemStack(Material.IRON_INGOT);
        blobT1.cellAmount = 500;
        var metaT1 = cellT1.getItemMeta();
        metaT1.getPersistentDataContainer().set(Keys.CELL_CARGO, PersistentDataType.STRING, NodeStore.encode(blobT1));
        cellT1.setItemMeta(metaT1);

        // 2) Colocar celda T1 en el centro (slot 20) y diamantes alrededor
        for (int slot : QuantumWorkbenchMenu.RECIPE_SLOTS) {
            if (slot == QuantumWorkbenchMenu.CENTER_SLOT) {
                player.getOpenInventory().getTopInventory().setItem(slot, cellT1);
            } else {
                player.getOpenInventory().getTopInventory().setItem(slot, new ItemStack(Material.DIAMOND, 1));
            }
        }

        // 3) Clic en el botón de crafteo (slot 23)
        InventoryClickEvent clickCraft = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, QuantumWorkbenchMenu.CRAFT_SLOT, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(clickCraft);

        // 4) Verificar que el output slot tiene la celda T2 con la carga preservada
        ItemStack result = player.getOpenInventory().getTopInventory().getItem(QuantumWorkbenchMenu.OUTPUT_SLOT);
        assertNotNull(result, "el slot de salida debe contener el resultado");
        assertEquals(DeviceType.CELL_T2, Items.typeOf(result), "el resultado debe ser Quantum Cell T2");

        String cargoDecoded = result.getItemMeta().getPersistentDataContainer()
                .get(Keys.CELL_CARGO, PersistentDataType.STRING);
        assertNotNull(cargoDecoded, "el resultado debe contener el tag CELL_CARGO");
        NodeBlob blobT2 = NodeStore.decode(cargoDecoded);
        assertNotNull(blobT2, "el blob de carga debe decodificarse correctamente");
        assertEquals(500, blobT2.cellAmount, "la cantidad de items almacenados debe preservarse intacta");
        assertEquals(Material.IRON_INGOT, blobT2.cellSample.getType(), "el tipo de item debe preservarse");

        // 5) Verificar que los ingredientes de la receta se consumieron
        for (int slot : QuantumWorkbenchMenu.RECIPE_SLOTS) {
            assertNull(player.getOpenInventory().getTopInventory().getItem(slot),
                    "los ingredientes consumidos deben desaparecer del grid");
        }
    }

    @Test
    void cerrarMenuDevuelveIngredientesAlJugador() {
        Block benchBlock = world.getBlockAt(0, 64, 0);
        benchBlock.setType(Material.BRAIN_CORAL_BLOCK);
        NodeStore.put(benchBlock, NodeBlob.create(DeviceType.QUANTUM_WORKBENCH.name()));

        QuantumWorkbenchMenu menu = new QuantumWorkbenchMenu(plugin, player, benchBlock);
        menu.openMenu();

        player.getOpenInventory().getTopInventory().setItem(QuantumWorkbenchMenu.CENTER_SLOT, new ItemStack(Material.GOLD_BLOCK, 2));
        player.closeInventory();

        assertTrue(player.getInventory().contains(Material.GOLD_BLOCK),
                "al cerrar la mesa de trabajo, los items en el grid deben volver al jugador");
    }
}

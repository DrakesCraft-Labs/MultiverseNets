package com.chagui68.multiversenets;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import com.chagui68.multiversenets.gui.CellMenu;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EN] Tests Quantum Cell GUI interactions: setting item template, quick depositing, withdrawing, and capacity safety.
 * [ES] Pruebas de interacción con la GUI de Quantum Cell: fijar plantilla de item, depósito rápido, extracción y seguridad de capacidad.
 */
class CellGuiTest {

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

    private Block placeCell(int x, int y, int z, DeviceType type) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(type.material());
        NodeStore.put(block, NodeBlob.create(type.name()));
        return block;
    }

    /**
     * [EN] Right clicking a cell opens its CellMenu GUI.
     * [ES] Hacer clic derecho en una celda abre su menú CellMenu.
     */
    @Test
    void cellOpensOnRightClick() {
        Block cell = placeCell(0, 64, 0, DeviceType.MVN_CELL_T1);
        PlayerInteractEvent interact = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK,
                null, cell, BlockFace.NORTH, EquipmentSlot.HAND, null);
        server.getPluginManager().callEvent(interact);
        assertTrue(player.getOpenInventory().getTopInventory().getHolder() instanceof CellMenu,
                "right click on cell should open its menu");
    }

    /**
     * [EN] Clicking Set Item button with item on cursor registers the item template without consuming cursor.
     * [ES] Clic en el botón Set Item con item en cursor registra la plantilla sin consumir el cursor.
     */
    @Test
    void setItemButtonSetsFilterSample() {
        Block cell = placeCell(0, 64, 0, DeviceType.MVN_CELL_T1);

        CellMenu menu = new CellMenu(plugin, player, cell, DeviceType.MVN_CELL_T1);
        menu.openMenu();
        player.getOpenInventory().setCursor(new ItemStack(Material.DIAMOND, 5));

        InventoryClickEvent click = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 13, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(click);

        NodeBlob blob = NodeStore.get(cell);
        assertNotNull(blob.cellSample, "cell sample must be set after clicking Set Item");
        assertEquals(Material.DIAMOND, blob.cellSample.getType());
        assertEquals(5, player.getItemOnCursor().getAmount());
    }

    /**
     * [EN] Quick Deposit button deposits matching items from player inventory into cell storage.
     * [ES] El botón Quick Deposit guarda items coincidentes del inventario del jugador en la celda.
     */
    @Test
    void quickDepositDepositsInventoryItemsIntoCell() {
        Block cell = placeCell(0, 64, 0, DeviceType.MVN_CELL_T1);
        NodeBlob blob = NodeStore.get(cell);
        blob.cellSample = new ItemStack(Material.COBBLESTONE);
        blob.cellAmount = 0;
        NodeStore.put(cell, blob);

        player.getInventory().addItem(new ItemStack(Material.COBBLESTONE, 64));
        CellMenu menu = new CellMenu(plugin, player, cell, DeviceType.MVN_CELL_T1);
        menu.openMenu();

        InventoryClickEvent click = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, CellMenu.DEPOSIT_ALL_SLOT, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(click);

        NodeBlob after = NodeStore.get(cell);
        assertEquals(64, after.cellAmount, "quick deposit saves matching items to cell");
        assertFalse(player.getInventory().contains(Material.COBBLESTONE), "items removed from player inventory");
    }

    /**
     * [EN] Right clicking stored item slot extracts 1 stack to cursor.
     * [ES] Clic derecho en el slot de item almacenado saca 1 stack al cursor.
     */
    @Test
    void storedItemSlotAllowsWithdrawingItems() {
        Block cell = placeCell(0, 64, 0, DeviceType.MVN_CELL_T1);
        NodeBlob blob = NodeStore.get(cell);
        blob.cellSample = new ItemStack(Material.DIAMOND);
        blob.cellAmount = 100;
        NodeStore.put(cell, blob);

        CellMenu menu = new CellMenu(plugin, player, cell, DeviceType.MVN_CELL_T1);
        menu.openMenu();

        InventoryClickEvent click = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, CellMenu.ITEM_SLOT, ClickType.RIGHT, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(click);

        NodeBlob after = NodeStore.get(cell);
        assertEquals(36, after.cellAmount, "withdrew 64 items from cell");
        assertNotNull(player.getItemOnCursor());
        assertEquals(64, player.getItemOnCursor().getAmount());
    }

    /**
     * [EN] Shift clicking stored item slot extracts items directly into player inventory.
     * [ES] Shift-clic en el slot de item extrae items directamente al inventario del jugador.
     */
    @Test
    void storedItemSlotShiftClickExtractsToInventory() {
        Block cell = placeCell(0, 64, 0, DeviceType.MVN_CELL_T1);
        NodeBlob blob = NodeStore.get(cell);
        blob.cellSample = new ItemStack(Material.EMERALD);
        blob.cellAmount = 100;
        NodeStore.put(cell, blob);

        CellMenu menu = new CellMenu(plugin, player, cell, DeviceType.MVN_CELL_T1);
        menu.openMenu();

        InventoryClickEvent click = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, CellMenu.ITEM_SLOT, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY);
        server.getPluginManager().callEvent(click);

        NodeBlob after = NodeStore.get(cell);
        assertEquals(0, after.cellAmount, "all 100 items moved to player inventory");
        assertTrue(player.getInventory().contains(Material.EMERALD));
    }

    /**
     * [EN] CellMenu top inventory has 18 slots (2 rows).
     * [ES] El menú CellMenu tiene 18 ranuras (2 filas).
     */
    @Test
    void cellMenuHasTwoRows() {
        assertEquals(18, cellMenuSize());
    }

    private int cellMenuSize() {
        Block cell = placeCell(0, 64, 0, DeviceType.MVN_CELL_T1);
        CellMenu menu = new CellMenu(plugin, player, cell, DeviceType.MVN_CELL_T1);
        menu.openMenu();
        return player.getOpenInventory().getTopInventory().getSize();
    }

    /**
     * [EN] Set Item does not overwrite the template of a non-empty cell.
     * [ES] Set Item no sobrescribe el tipo de una celda que contiene items.
     */
    @Test
    void setItemDoesNotOverwriteNonEmptyCell() {
        Block cell = placeCell(0, 64, 0, DeviceType.MVN_CELL_T1);
        NodeBlob blob = NodeStore.get(cell);
        blob.cellSample = new ItemStack(Material.COBBLESTONE);
        blob.cellAmount = 100;
        NodeStore.put(cell, blob);

        player.setItemOnCursor(new ItemStack(Material.DIAMOND, 1));
        CellMenu menu = new CellMenu(plugin, player, cell, DeviceType.MVN_CELL_T1);
        menu.openMenu();
        InventoryClickEvent click = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 13, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(click);

        NodeBlob after = NodeStore.get(cell);
        assertEquals(Material.COBBLESTONE, after.cellSample.getType(),
                "cell with items cannot have its template changed");
    }

    /**
     * [EN] Shift clicking item from player inventory into cell deposits it into cell storage.
     * [ES] Shift-clic en un item del inventario del jugador hacia la celda lo deposita en su almacenamiento.
     */
    @Test
    void shiftClickFromPlayerInventoryDepositsIntoCell() {
        Block cell = placeCell(0, 64, 0, DeviceType.MVN_CELL_T1);
        NodeBlob blob = NodeStore.get(cell);
        blob.cellSample = new ItemStack(Material.COBBLESTONE);
        blob.cellAmount = 0;
        NodeStore.put(cell, blob);

        player.getInventory().setItem(0, new ItemStack(Material.COBBLESTONE, 32));

        CellMenu menu = new CellMenu(plugin, player, cell, DeviceType.MVN_CELL_T1);
        menu.openMenu();

        InventoryClickEvent shift = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 18, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY);
        server.getPluginManager().callEvent(shift);

        NodeBlob after = NodeStore.get(cell);
        assertEquals(32, after.cellAmount, "cell should have absorbed 32 cobblestone");
        assertNull(player.getInventory().getItem(0), "player inventory slot 0 should be cleared");
    }
}

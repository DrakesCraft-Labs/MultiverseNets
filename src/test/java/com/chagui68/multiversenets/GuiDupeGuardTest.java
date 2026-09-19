package com.chagui68.multiversenets;

import com.chagui68.multiversenets.gui.BarrelMenu;
import com.chagui68.multiversenets.gui.CellMenu;
import com.chagui68.multiversenets.gui.TerminalMenu;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EN] Tests anti-duplication security guards against dangerous click actions in custom GUIs.
 * [ES] Pruebas de guardas de seguridad anti-duplicación contra acciones de clic peligrosas en GUIs personalizadas.
 */
class GuiDupeGuardTest {

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
     * [EN] Dangerous inventory actions (number keys, swap offhand, drop, double-click, middle click) are cancelled in menus.
     * [ES] Las acciones de inventario peligrosas (teclas numéricas, offhand swap, soltar, doble clic, clic central) son canceladas en menús.
     */
    @Test
    void dangerousActionsAreCancelledInMenus() {
        Block ctrl = world.getBlockAt(0, 64, 0);
        ctrl.setType(Material.LODESTONE);
        NodeStore.put(ctrl, NodeBlob.create(DeviceType.MVN_CONTROLLER.name()));
        plugin.networks().registerController(ctrl);
        Network net = plugin.networks().networkAt(ctrl);

        TerminalMenu menu = new TerminalMenu(plugin, player, net);
        menu.openMenu();

        // 1) NUMBER_KEY
        InventoryClickEvent numKey = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 0, ClickType.NUMBER_KEY, InventoryAction.HOTBAR_SWAP);
        server.getPluginManager().callEvent(numKey);
        assertTrue(numKey.isCancelled(), "NUMBER_KEY must be cancelled");

        // 2) SWAP_OFFHAND (F)
        InventoryClickEvent offhand = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 0, ClickType.SWAP_OFFHAND, InventoryAction.HOTBAR_SWAP);
        server.getPluginManager().callEvent(offhand);
        assertTrue(offhand.isCancelled(), "SWAP_OFFHAND must be cancelled");

        // 3) DROP (Q) and CONTROL_DROP
        InventoryClickEvent drop = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 0, ClickType.DROP, InventoryAction.DROP_ONE_SLOT);
        server.getPluginManager().callEvent(drop);
        assertTrue(drop.isCancelled(), "DROP must be cancelled");

        InventoryClickEvent ctrlDrop = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 0, ClickType.CONTROL_DROP, InventoryAction.DROP_ALL_SLOT);
        server.getPluginManager().callEvent(ctrlDrop);
        assertTrue(ctrlDrop.isCancelled(), "CONTROL_DROP must be cancelled");

        // 4) DOUBLE_CLICK / COLLECT_TO_CURSOR
        InventoryClickEvent collect = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 0, ClickType.DOUBLE_CLICK, InventoryAction.COLLECT_TO_CURSOR);
        server.getPluginManager().callEvent(collect);
        assertTrue(collect.isCancelled(), "COLLECT_TO_CURSOR / DOUBLE_CLICK must be cancelled");

        // 5) MIDDLE (creative pick block)
        InventoryClickEvent middle = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 0, ClickType.MIDDLE, InventoryAction.CLONE_STACK);
        server.getPluginManager().callEvent(middle);
        assertTrue(middle.isCancelled(), "MIDDLE click must be cancelled");
    }

    /**
     * [EN] Dangerous click actions are cancelled in CellMenu and BarrelMenu.
     * [ES] Las acciones de clic peligrosas son canceladas en CellMenu y BarrelMenu.
     */
    @Test
    void dangerousActionsCancelledInCellMenuAndBarrelMenu() {
        Block cellBlock = world.getBlockAt(1, 64, 0);
        cellBlock.setType(Material.TERRACOTTA);
        NodeStore.put(cellBlock, NodeBlob.create(DeviceType.MVN_CELL_T1.name()));

        CellMenu cellMenu = new CellMenu(plugin, player, cellBlock, DeviceType.MVN_CELL_T1);
        cellMenu.openMenu();

        InventoryClickEvent numKey = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 4, ClickType.NUMBER_KEY, InventoryAction.HOTBAR_SWAP);
        server.getPluginManager().callEvent(numKey);
        assertTrue(numKey.isCancelled(), "NUMBER_KEY in CellMenu must be cancelled");

        Block barrelBlock = world.getBlockAt(2, 64, 0);
        barrelBlock.setType(Material.BARREL);
        NodeStore.put(barrelBlock, NodeBlob.create(DeviceType.MVN_INFINITY_BARREL.name()));

        BarrelMenu barrelMenu = new BarrelMenu(plugin, player, barrelBlock);
        barrelMenu.openMenu();

        InventoryClickEvent drop = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 4, ClickType.DROP, InventoryAction.DROP_ONE_SLOT);
        server.getPluginManager().callEvent(drop);
        assertTrue(drop.isCancelled(), "DROP in BarrelMenu must be cancelled");
    }

    /**
     * [EN] Shift-clicking (left or right) an item from player inventory removes it properly without duplicating.
     * [ES] Shift-clic (izquierdo o derecho) en un ítem del inventario del jugador lo retira adecuadamente sin duplicarlo.
     */
    @Test
    void shiftClickDepositsAndRemovesFromPlayerInventoryWithoutDupe() {
        Block ctrl = world.getBlockAt(0, 64, 0);
        ctrl.setType(Material.LODESTONE);
        NodeStore.put(ctrl, NodeBlob.create(DeviceType.MVN_CONTROLLER.name()));
        plugin.networks().registerController(ctrl);

        Block cellBlock = world.getBlockAt(1, 64, 0);
        cellBlock.setType(Material.TERRACOTTA);
        NodeStore.put(cellBlock, NodeBlob.create(DeviceType.MVN_CELL_T2.name()));

        Network net = plugin.networks().networkAt(ctrl);
        net.scan();

        // 1) TerminalMenu with SHIFT_LEFT on slot 0 (Hotbar slot 1)
        TerminalMenu terminalMenu = new TerminalMenu(plugin, player, net);
        terminalMenu.openMenu();

        player.getInventory().setItem(0, new ItemStack(Material.GOLD_INGOT, 16));
        InventoryClickEvent shiftLeft = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 54, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY);
        server.getPluginManager().callEvent(shiftLeft);

        assertEquals(16, net.storage().count(i -> i.getType() == Material.GOLD_INGOT),
                "items deposited into network");
        assertNull(player.getInventory().getItem(0),
                "slot 0 in player inventory must be cleared after deposit, preventing duplication");

        // Firing again when slot 0 is empty must not duplicate anything
        server.getPluginManager().callEvent(shiftLeft);
        assertEquals(16, net.storage().count(i -> i.getType() == Material.GOLD_INGOT),
                "second click on empty slot must not add any items");

        // 2) TerminalMenu with SHIFT_RIGHT on slot 3 (more gold into the same cell)
        player.getInventory().setItem(3, new ItemStack(Material.GOLD_INGOT, 8));
        InventoryClickEvent shiftRight = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 54 + 3, ClickType.SHIFT_RIGHT, InventoryAction.MOVE_TO_OTHER_INVENTORY);
        server.getPluginManager().callEvent(shiftRight);

        assertEquals(24, net.storage().count(i -> i.getType() == Material.GOLD_INGOT),
                "items deposited into network via shift-right click");
        assertNull(player.getInventory().getItem(3),
                "slot 3 in player inventory must be cleared after shift-right deposit");

        // 3) CellMenu with SHIFT_LEFT on a dedicated cell block
        player.closeInventory();
        Block emptyCellBlock = world.getBlockAt(5, 64, 0);
        emptyCellBlock.setType(Material.TERRACOTTA);
        NodeStore.put(emptyCellBlock, NodeBlob.create(DeviceType.MVN_CELL_T2.name()));

        CellMenu cellMenu = new CellMenu(plugin, player, emptyCellBlock, DeviceType.MVN_CELL_T2);
        cellMenu.openMenu();

        player.getInventory().setItem(0, new ItemStack(Material.EMERALD, 12));
        InventoryClickEvent cellShift = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 18, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY);
        server.getPluginManager().callEvent(cellShift);

        NodeBlob cellBlob = NodeStore.get(emptyCellBlock);
        assertEquals(12, cellBlob.cellAmount, "items deposited into cell");
        assertNull(player.getInventory().getItem(0), "slot 0 cleared from player inventory in CellMenu");

        // 4) BarrelMenu with SHIFT_RIGHT
        player.closeInventory();
        Block barrelBlock = world.getBlockAt(2, 64, 0);
        barrelBlock.setType(Material.BARREL);
        NodeBlob bBlob = NodeBlob.create(DeviceType.MVN_INFINITY_BARREL.name());
        bBlob.cellSample = new ItemStack(Material.IRON_INGOT, 1);
        NodeStore.put(barrelBlock, bBlob);

        BarrelMenu barrelMenu = new BarrelMenu(plugin, player, barrelBlock);
        barrelMenu.openMenu();

        player.getInventory().setItem(0, new ItemStack(Material.IRON_INGOT, 20));
        InventoryClickEvent barrelShift = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 18, ClickType.SHIFT_RIGHT, InventoryAction.MOVE_TO_OTHER_INVENTORY);
        server.getPluginManager().callEvent(barrelShift);

        NodeBlob afterBarrel = NodeStore.get(barrelBlock);
        assertEquals(20, afterBarrel.cellAmount, "items absorbed into barrel");
        assertNull(player.getInventory().getItem(0), "slot 0 cleared from player inventory in BarrelMenu");
    }
}

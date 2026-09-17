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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

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
        NodeStore.put(ctrl, NodeBlob.create(DeviceType.CONTROLLER.name()));
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
        NodeStore.put(cellBlock, NodeBlob.create(DeviceType.CELL_T1.name()));

        CellMenu cellMenu = new CellMenu(plugin, player, cellBlock, DeviceType.CELL_T1);
        cellMenu.openMenu();

        InventoryClickEvent numKey = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 4, ClickType.NUMBER_KEY, InventoryAction.HOTBAR_SWAP);
        server.getPluginManager().callEvent(numKey);
        assertTrue(numKey.isCancelled(), "NUMBER_KEY in CellMenu must be cancelled");

        Block barrelBlock = world.getBlockAt(2, 64, 0);
        barrelBlock.setType(Material.BARREL);
        NodeStore.put(barrelBlock, NodeBlob.create(DeviceType.INFINITY_BARREL.name()));

        BarrelMenu barrelMenu = new BarrelMenu(plugin, player, barrelBlock);
        barrelMenu.openMenu();

        InventoryClickEvent drop = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 4, ClickType.DROP, InventoryAction.DROP_ONE_SLOT);
        server.getPluginManager().callEvent(drop);
        assertTrue(drop.isCancelled(), "DROP in BarrelMenu must be cancelled");
    }
}

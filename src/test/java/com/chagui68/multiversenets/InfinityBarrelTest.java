package com.chagui68.multiversenets;

import com.chagui68.multiversenets.gui.BarrelMenu;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.Keys;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
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
 * [EN] Tests Infinity Barrel operations: 2-billion item capacity, depositing, withdrawing, and breaking/placing persistence.
 * [ES] Pruebas de la Barrica Infinita: capacidad de 2 billones de items, depósitos, retiros y persistencia al romper/colocar.
 */
class InfinityBarrelTest {

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
     * [EN] Infinity Barrel has a capacity of 2,000,000,000 items.
     * [ES] La barrica infinita tiene una capacidad de 2,000,000,000 de items.
     */
    @Test
    void barrelHasTwoBillionCapacity() {
        assertEquals(2_000_000_000L, Items.capacityOf(DeviceType.MVN_INFINITY_BARREL),
                "infinity barrel must have capacity of 2 billion");
    }

    /**
     * [EN] Opening the barrel menu, setting item template, and quick-depositing items.
     * [ES] Abrir el menú de la barrica, fijar plantilla de item y hacer depósito rápido.
     */
    @Test
    void openBarrelAndDepositItems() {
        Block barrelBlock = world.getBlockAt(0, 64, 0);
        barrelBlock.setType(Material.BARREL);
        NodeStore.put(barrelBlock, NodeBlob.create(DeviceType.MVN_INFINITY_BARREL.name()));

        BarrelMenu menu = new BarrelMenu(plugin, player, barrelBlock);
        menu.openMenu();

        // 1) Set item template with cursor
        player.getOpenInventory().setCursor(new ItemStack(Material.EMERALD, 1));
        InventoryClickEvent clickSet = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, BarrelMenu.SET_SLOT, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(clickSet);

        NodeBlob blob = NodeStore.get(barrelBlock);
        assertNotNull(blob.cellSample, "item sample must be set");
        assertEquals(Material.EMERALD, blob.cellSample.getType());

        // 2) Quick Deposit with 64 emeralds in player inventory
        player.getInventory().setItem(0, new ItemStack(Material.EMERALD, 64));
        InventoryClickEvent clickDeposit = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, BarrelMenu.DEPOSIT_ALL_SLOT, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(clickDeposit);

        blob = NodeStore.get(barrelBlock);
        assertEquals(64, blob.cellAmount, "barrel must store 64 emeralds");
        assertNull(player.getInventory().getItem(0), "player inventory slot must be empty after deposit");
    }

    /**
     * [EN] Infinity Barrel integrates seamlessly into network storage for bulk deposit and withdrawal.
     * [ES] La barrica infinita se integra en la red de almacenamiento para depósitos y retiros masivos.
     */
    @Test
    void barrelIntegratesIntoNetworkStorage() {
        Block ctrl = world.getBlockAt(0, 64, 0);
        ctrl.setType(Material.LODESTONE);
        NodeStore.put(ctrl, NodeBlob.create(DeviceType.MVN_CONTROLLER.name()));
        plugin.networks().registerController(ctrl);

        Block cable = world.getBlockAt(1, 64, 0);
        cable.setType(Material.GLASS);
        NodeStore.put(cable, NodeBlob.create(DeviceType.MVN_CABLE.name()));

        Block barrel = world.getBlockAt(2, 64, 0);
        barrel.setType(Material.BARREL);
        NodeStore.put(barrel, NodeBlob.create(DeviceType.MVN_INFINITY_BARREL.name()));

        Network net = plugin.networks().networkAt(ctrl);
        assertNotNull(net);
        net.scan();

        // Deposit 5000 diamonds into network
        int leftover = net.storage().deposit(new ItemStack(Material.DIAMOND, 5000));
        assertEquals(0, leftover, "all diamonds must enter infinity barrel");

        NodeBlob blob = NodeStore.get(barrel);
        assertEquals(5000, blob.cellAmount, "barrel stores 5000 diamonds");
        assertEquals(Material.DIAMOND, blob.cellSample.getType());

        // Withdraw 64 diamonds
        ItemStack withdrawn = net.storage().withdraw(item -> item.getType() == Material.DIAMOND, 64);
        assertNotNull(withdrawn);
        assertEquals(64, withdrawn.getAmount());

        blob = NodeStore.get(barrel);
        assertEquals(4936, blob.cellAmount, "4936 diamonds must remain in barrel");
    }

    /**
     * [EN] Breaking and placing an Infinity Barrel preserves stored item amount and type.
     * [ES] Romper y colocar una barrica infinita preserva la cantidad y tipo de items guardados.
     */
    @Test
    void breakAndPlaceBarrelPreservesStoredItems() {
        Block barrelBlock = world.getBlockAt(0, 64, 0);
        barrelBlock.setType(Material.BARREL);
        NodeBlob blob = NodeBlob.create(DeviceType.MVN_INFINITY_BARREL.name());
        blob.cellSample = new ItemStack(Material.NETHERITE_INGOT);
        blob.cellAmount = 15000;
        NodeStore.put(barrelBlock, blob);

        // Break
        BlockBreakEvent breakEvent = new BlockBreakEvent(barrelBlock, player);
        server.getPluginManager().callEvent(breakEvent);

        // Simulate placing item with embedded cargo
        ItemStack itemDropped = Items.create(DeviceType.MVN_INFINITY_BARREL);
        var meta = itemDropped.getItemMeta();
        meta.getPersistentDataContainer().set(Keys.CELL_CARGO, PersistentDataType.STRING, NodeStore.encode(blob));
        itemDropped.setItemMeta(meta);

        player.getInventory().setItemInMainHand(itemDropped);
        Block newPos = world.getBlockAt(10, 64, 10);
        BlockPlaceEvent placeEvent = new BlockPlaceEvent(newPos, newPos.getState(), barrelBlock,
                itemDropped, player, true, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(placeEvent);

        NodeBlob restored = NodeStore.get(newPos);
        assertNotNull(restored, "placed blob must exist");
        assertEquals(15000, restored.cellAmount, "amount of 15000 netherite ingots must be preserved");
        assertEquals(Material.NETHERITE_INGOT, restored.cellSample.getType());
    }
}

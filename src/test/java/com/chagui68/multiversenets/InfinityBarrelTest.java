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

    @Test
    void barrelTieneCapacidadDeDosBillones() {
        assertEquals(2_000_000_000L, Items.capacityOf(DeviceType.INFINITY_BARREL),
                "la barrica infinita debe tener capacidad de 2 billones");
    }

    @Test
    void abrirBarrelYDepositarItems() {
        Block barrelBlock = world.getBlockAt(0, 64, 0);
        barrelBlock.setType(Material.BARREL);
        NodeStore.put(barrelBlock, NodeBlob.create(DeviceType.INFINITY_BARREL.name()));

        BarrelMenu menu = new BarrelMenu(plugin, player, barrelBlock);
        menu.openMenu();

        // 1) Establecer tipo con cursor
        player.getOpenInventory().setCursor(new ItemStack(Material.EMERALD, 1));
        InventoryClickEvent clickSet = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, BarrelMenu.SET_SLOT, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(clickSet);

        NodeBlob blob = NodeStore.get(barrelBlock);
        assertNotNull(blob.cellSample, "el tipo de item debe haberse fijado");
        assertEquals(Material.EMERALD, blob.cellSample.getType());

        // 2) Quick Deposit con 64 esmeraldas en el inventario del jugador
        player.getInventory().setItem(0, new ItemStack(Material.EMERALD, 64));
        InventoryClickEvent clickDeposit = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, BarrelMenu.DEPOSIT_ALL_SLOT, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(clickDeposit);

        blob = NodeStore.get(barrelBlock);
        assertEquals(64, blob.cellAmount, "la barrica debe almacenar 64 esmeraldas");
        assertNull(player.getInventory().getItem(0), "el inventario del jugador debe quedar vacio tras depositar");
    }

    @Test
    void barrelSeIntegraEnLaRedDeAlmacenamiento() {
        // Controlador en (0,64,0)
        Block ctrl = world.getBlockAt(0, 64, 0);
        ctrl.setType(Material.LODESTONE);
        NodeStore.put(ctrl, NodeBlob.create(DeviceType.CONTROLLER.name()));
        plugin.networks().registerController(ctrl);

        // Cable en (1,64,0)
        Block cable = world.getBlockAt(1, 64, 0);
        cable.setType(Material.GLASS);
        NodeStore.put(cable, NodeBlob.create(DeviceType.CABLE.name()));

        // Infinity Barrel en (2,64,0)
        Block barrel = world.getBlockAt(2, 64, 0);
        barrel.setType(Material.BARREL);
        NodeStore.put(barrel, NodeBlob.create(DeviceType.INFINITY_BARREL.name()));

        Network net = plugin.networks().networkAt(ctrl);
        assertNotNull(net);
        net.scan();

        // Depositar 5000 diamantes a la red
        int leftover = net.storage().deposit(new ItemStack(Material.DIAMOND, 5000));
        assertEquals(0, leftover, "todos los diamantes deben entrar a la barrica infinita de la red");

        NodeBlob blob = NodeStore.get(barrel);
        assertEquals(5000, blob.cellAmount, "la barrica debe guardar los 5000 diamantes");
        assertEquals(Material.DIAMOND, blob.cellSample.getType());

        // Retirar 64 diamantes de la red
        ItemStack sacado = net.storage().withdraw(item -> item.getType() == Material.DIAMOND, 64);
        assertNotNull(sacado);
        assertEquals(64, sacado.getAmount());

        blob = NodeStore.get(barrel);
        assertEquals(4936, blob.cellAmount, "deben quedar 4936 diamantes en la barrica");
    }

    @Test
    void romperYColocarBarrelPreservaLosItemsGuardados() {
        Block barrelBlock = world.getBlockAt(0, 64, 0);
        barrelBlock.setType(Material.BARREL);
        NodeBlob blob = NodeBlob.create(DeviceType.INFINITY_BARREL.name());
        blob.cellSample = new ItemStack(Material.NETHERITE_INGOT);
        blob.cellAmount = 15000;
        NodeStore.put(barrelBlock, blob);

        // Romper
        BlockBreakEvent breakEvent = new BlockBreakEvent(barrelBlock, player);
        server.getPluginManager().callEvent(breakEvent);

        // Simular colocar de nuevo el bloque con el ítem caído
        ItemStack itemDropped = Items.create(DeviceType.INFINITY_BARREL);
        var meta = itemDropped.getItemMeta();
        meta.getPersistentDataContainer().set(Keys.CELL_CARGO, PersistentDataType.STRING, NodeStore.encode(blob));
        itemDropped.setItemMeta(meta);

        player.getInventory().setItemInMainHand(itemDropped);
        Block newPos = world.getBlockAt(10, 64, 10);
        BlockPlaceEvent placeEvent = new BlockPlaceEvent(newPos, newPos.getState(), barrelBlock,
                itemDropped, player, true, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(placeEvent);

        NodeBlob restored = NodeStore.get(newPos);
        assertNotNull(restored, "el blob colocado debe existir");
        assertEquals(15000, restored.cellAmount, "la cantidad de 15000 netherite ingots debe preservarse");
        assertEquals(Material.NETHERITE_INGOT, restored.cellSample.getType());
    }
}

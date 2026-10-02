package com.chagui68.multiversenets;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import com.chagui68.multiversenets.gui.DramBayMenu;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.MemoryModules;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
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
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EN] DRAM Bay: memory modules live in their own block, and a module taken out carries its whole
 * stock to whichever network it is installed in next.
 *
 * [ES] DRAM Bay: los módulos de memoria viven en su propio bloque, y un módulo que se saca lleva
 * todo su stock a la red donde se instale después.
 */
class DramBayTest {

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

    private Block place(int x, int y, int z, DeviceType type) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(type.material());
        NodeStore.put(block, NodeBlob.create(type.name()));
        if (type == DeviceType.MVN_CONTROLLER) {
            plugin.networks().registerController(block);
        } else {
            plugin.networks().invalidateNear(block);
        }
        return block;
    }

    private Network network(Block controller) {
        Network net = plugin.networks().networkByController(controller.getLocation());
        net.scan();
        return net;
    }

    private void rightClick(Block block, ItemStack held) {
        player.getInventory().setItemInMainHand(held);
        server.getPluginManager().callEvent(new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, held, block,
                BlockFace.NORTH, EquipmentSlot.HAND, null));
    }

    @Test
    void aModuleInstalledInABayStoresItemsForTheNetwork() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        Block bay = place(1, 64, 0, DeviceType.MVN_DRAM_BAY);
        Network net = network(controller);
        assertEquals(10, net.storage().deposit(new ItemStack(Material.DIRT, 10)), "no storage yet");

        rightClick(bay, Items.create(DeviceType.MVN_CACHE_L1));

        assertEquals(DeviceType.MVN_CACHE_L1, MemoryModules.installed(NodeStore.get(bay)));
        assertTrue(player.getInventory().getItemInMainHand().getType().isAir(), "the module left the hand");
        assertEquals(0, net.storage().deposit(new ItemStack(Material.DIRT, 10)));
        assertEquals(10, net.storage().count(i -> i.getType() == Material.DIRT));
    }

    @Test
    void anEjectedModuleMovesItsStockToAnotherNetwork() {
        Block controllerA = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        Block bayA = place(1, 64, 0, DeviceType.MVN_DRAM_BAY);
        Block controllerB = place(0, 64, 20, DeviceType.MVN_CONTROLLER);
        Block bayB = place(1, 64, 20, DeviceType.MVN_DRAM_BAY);
        Network a = network(controllerA);
        Network b = network(controllerB);
        rightClick(bayA, Items.create(DeviceType.MVN_CACHE_L2));
        a.storage().deposit(new ItemStack(Material.IRON_INGOT, 300));
        a.storage().deposit(new ItemStack(Material.GOLD_INGOT, 40));

        new DramBayMenu(plugin, player, bayA).openMenu();
        server.getPluginManager().callEvent(new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, DramBayMenu.EJECT_SLOT, ClickType.LEFT, InventoryAction.PICKUP_ALL));
        player.closeInventory();

        assertNull(MemoryModules.installed(NodeStore.get(bayA)), "the bay is empty");
        assertEquals(0, a.storage().count(i -> true), "the stock left network A");
        ItemStack module = null;
        for (ItemStack it : player.getInventory().getContents()) {
            if (Items.typeOf(it) == DeviceType.MVN_CACHE_L2) {
                module = it;
            }
        }
        assertNotNull(module, "the player got the module back");
        assertNotNull(MemoryModules.cargoOf(module), "the module carries the stock");

        rightClick(bayB, module);

        assertEquals(300, b.storage().count(i -> i.getType() == Material.IRON_INGOT), "the stock appears in network B");
        assertEquals(40, b.storage().count(i -> i.getType() == Material.GOLD_INGOT));
        assertEquals(0, a.storage().count(i -> true));
    }

    @Test
    void breakingABayDropsTheModuleWithItsStock() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        Block bay = place(1, 64, 0, DeviceType.MVN_DRAM_BAY);
        Network net = network(controller);
        rightClick(bay, Items.create(DeviceType.MVN_CACHE_L1));
        net.storage().deposit(new ItemStack(Material.DIRT, 25));

        server.getPluginManager().callEvent(new BlockBreakEvent(bay, player));

        ItemStack module = world.getEntities().stream()
                .filter(e -> e instanceof org.bukkit.entity.Item item && Items.typeOf(item.getItemStack()) == DeviceType.MVN_CACHE_L1)
                .map(e -> ((org.bukkit.entity.Item) e).getItemStack())
                .findFirst().orElse(null);
        assertNotNull(module, "the module dropped");
        NodeBlob cargo = MemoryModules.cargoOf(module);
        assertNotNull(cargo);
        assertEquals(25L, cargo.totalVirtualAmount());
    }

    @Test
    void theFluidDramHoldsSeveralFluidsAndTravelsWithThem() {
        Block controllerA = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        Block bayA = place(1, 64, 0, DeviceType.MVN_DRAM_BAY);
        Block controllerB = place(0, 64, 20, DeviceType.MVN_CONTROLLER);
        Block bayB = place(1, 64, 20, DeviceType.MVN_DRAM_BAY);
        Network a = network(controllerA);
        Network b = network(controllerB);
        rightClick(bayA, Items.create(DeviceType.MVN_FLUID_DRAM));

        assertEquals(0, a.fluidStorage().deposit("WATER", 5000));
        assertEquals(0, a.fluidStorage().deposit("LAVA", 3000));
        assertEquals(5000, a.fluidStorage().count("WATER"));
        assertEquals(3000, a.fluidStorage().count("LAVA"));
        assertEquals(1, a.storage().deposit(new ItemStack(Material.DIRT)), "a Fluid DRAM stores no items");

        NodeBlob blob = NodeStore.get(bayA);
        ItemStack module = MemoryModules.eject(blob);
        NodeStore.put(bayA, blob);
        assertEquals(0, a.fluidStorage().count("WATER"));

        rightClick(bayB, module);
        assertEquals(5000, b.fluidStorage().count("WATER"));
        assertEquals(3000, b.fluidStorage().count("LAVA"));
    }

    @Test
    void anOldControllerModuleWaitsInTheTerminalAsATemporaryItem() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        Block bay = place(1, 64, 0, DeviceType.MVN_DRAM_BAY);
        Network net = network(controller);
        rightClick(controller, Items.create(DeviceType.MVN_CACHE_L1));
        assertEquals(0, NodeStore.get(controller).virtualCacheTier, "right-clicking the controller installs nothing");

        // A controller from before the DRAM Bay, with its module and items inside.
        NodeBlob blob = NodeStore.get(controller);
        blob.virtualCacheTier = 3;
        blob.addVirtualItem(new ItemStack(Material.EMERALD), 77);
        NodeStore.put(controller, blob);

        net.scan();

        NodeBlob after = NodeStore.get(controller);
        assertEquals(0, after.virtualCacheTier, "the module no longer lives in the controller");
        assertEquals(1, after.recoveredModules.size(), "it waits as a recovered module");
        assertEquals(0, net.storage().count(i -> true), "its items travel inside the module, not in the network");

        player.getInventory().clear();
        new com.chagui68.multiversenets.gui.TerminalMenu(plugin, player, net).openMenu();
        ItemStack shown = player.getOpenInventory().getTopInventory().getItem(0);
        assertEquals(DeviceType.MVN_CACHE_L3, Items.typeOf(shown), "the Terminal shows it first, apart from the stock");
        server.getPluginManager().callEvent(new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 0, ClickType.LEFT, InventoryAction.PICKUP_ALL));

        ItemStack taken = player.getOpenInventory().getCursor();
        assertEquals(DeviceType.MVN_CACHE_L3, Items.typeOf(taken), "clicking it hands the module over");
        assertTrue(NodeStore.get(controller).recoveredModules.isEmpty(), "and it leaves the Terminal");
        assertEquals(77L, MemoryModules.cargoOf(taken).totalVirtualAmount());
        player.setItemOnCursor(null);
        player.closeInventory();

        rightClick(bay, taken);
        assertEquals(77, net.storage().count(i -> i.getType() == Material.EMERALD), "installed in a bay, the items are back");
    }

    @Test
    void theTerminalTellsHowMuchOfAnItemIsInDram() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        Block bay = place(1, 64, 0, DeviceType.MVN_DRAM_BAY);
        Network net = network(controller);
        rightClick(bay, Items.create(DeviceType.MVN_CACHE_L1));
        net.storage().deposit(new ItemStack(Material.DIRT, 10));

        new com.chagui68.multiversenets.gui.TerminalMenu(plugin, player, net).openMenu();
        ItemStack icon = player.getOpenInventory().getTopInventory().getItem(0);
        assertNotNull(icon);
        boolean found = false;
        for (Component line : icon.getItemMeta().lore()) {
            String text = PlainTextComponentSerializer.plainText().serialize(line);
            if (text.contains("In DRAM: 10")) {
                found = true;
            }
        }
        assertTrue(found, "the lore shows the amount kept in DRAM, like the Greedy Buffer line");
    }

    @Test
    void breakingAControllerDropsItsUncollectedModules() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        Network net = network(controller);
        NodeBlob blob = NodeStore.get(controller);
        blob.virtualCacheTier = 1;
        blob.addVirtualItem(new ItemStack(Material.DIRT), 5);
        NodeStore.put(controller, blob);
        net.scan();

        server.getPluginManager().callEvent(new BlockBreakEvent(controller, player));

        boolean dropped = world.getEntities().stream().anyMatch(e -> e instanceof org.bukkit.entity.Item item
                && Items.typeOf(item.getItemStack()) == DeviceType.MVN_CACHE_L1
                && MemoryModules.cargoOf(item.getItemStack()) != null);
        assertTrue(dropped, "the recovered module drops with its items");
    }
}

package com.chagui68.multiversenets;

import com.chagui68.multiversenets.gui.GreedyMenu;
import com.chagui68.multiversenets.gui.TerminalMenu;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.net.NetworkStorage;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.Keys;
import com.chagui68.multiversenets.util.Settings;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Greedy Cell multi-item shared capacity storage,
 * dedicated GUI (GreedyMenu) with monitor, and TerminalMenu purger/greedy enhancements.
 */
class GreedyCellTest {

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
    void multiItemGreedyStorageAndSharedCapacity() {
        NodeBlob blob = NodeBlob.create(DeviceType.MVN_GREEDY_CELL.name());
        assertEquals(0, blob.totalGreedyAmount());

        ItemStack stone = new ItemStack(Material.STONE);
        ItemStack diamond = new ItemStack(Material.DIAMOND);

        // Add 10,000 stones and 5,000 diamonds
        blob.addGreedyItem(stone, 10_000);
        blob.addGreedyItem(diamond, 5_000);

        assertEquals(15_000, blob.totalGreedyAmount(), "Total amount should be shared sum of all items");
        assertEquals(2, blob.greedySamples.size());
        assertEquals(10_000, blob.greedyAmounts.get(0));
        assertEquals(5_000, blob.greedyAmounts.get(1));

        // Add more stone
        blob.addGreedyItem(stone, 2_000);
        assertEquals(17_000, blob.totalGreedyAmount());
        assertEquals(12_000, blob.greedyAmounts.get(0));

        // Remove 10,000 stone
        long removedStone = blob.removeGreedyItem(0, 10_000);
        assertEquals(10_000, removedStone);
        assertEquals(2_000, blob.greedyAmounts.get(0));
        assertEquals(7_000, blob.totalGreedyAmount());

        // Remove rest of stone (2,000) -> should remove index 0 and diamond becomes index 0
        long removedRest = blob.removeGreedyItem(0, 2_000);
        assertEquals(2_000, removedRest);
        assertEquals(1, blob.greedySamples.size());
        assertEquals(Material.DIAMOND, blob.greedySamples.get(0).getType());
        assertEquals(5_000, blob.greedyAmounts.get(0));
        assertEquals(5_000, blob.totalGreedyAmount());
    }

    @Test
    void legacyGreedyCellMigration() {
        NodeBlob legacy = NodeBlob.create(DeviceType.MVN_GREEDY_CELL.name());
        legacy.cellSample = new ItemStack(Material.GOLD_INGOT);
        legacy.cellAmount = 64_000;
        legacy.greedySamples = null;
        legacy.greedyAmounts = null;

        String encoded = NodeStore.encode(legacy);
        NodeBlob decoded = NodeStore.decode(encoded);

        assertNotNull(decoded);
        assertNull(decoded.cellSample);
        assertEquals(0, decoded.cellAmount);
        assertNotNull(decoded.greedySamples);
        assertNotNull(decoded.greedyAmounts);
        assertEquals(1, decoded.greedySamples.size());
        assertEquals(Material.GOLD_INGOT, decoded.greedySamples.get(0).getType());
        assertEquals(64_000, decoded.greedyAmounts.get(0));
        assertEquals(64_000, decoded.totalGreedyAmount());
    }

    @Test
    void greedyMenuOpensAndDrawsMonitor() {
        Block block = world.getBlockAt(0, 64, 0);
        block.setType(Material.SLIME_BLOCK);
        NodeBlob blob = NodeBlob.create(DeviceType.MVN_GREEDY_CELL.name());
        blob.addGreedyItem(new ItemStack(Material.IRON_INGOT), 1_000);
        NodeStore.put(block, blob);

        GreedyMenu menu = new GreedyMenu(plugin, player, block);
        menu.openMenu();

        assertNotNull(player.getOpenInventory().getTopInventory());
        assertEquals(54, player.getOpenInventory().getTopInventory().getSize());

        // Slot 0 should have the stored item
        ItemStack slot0 = player.getOpenInventory().getTopInventory().getItem(0);
        assertNotNull(slot0);
        assertEquals(Material.IRON_INGOT, slot0.getType());

        // Slot 49 should have the monitor icon
        ItemStack monitor = player.getOpenInventory().getTopInventory().getItem(49);
        assertNotNull(monitor);
        assertEquals(Material.RESPAWN_ANCHOR, monitor.getType());
        assertTrue(monitor.hasItemMeta());
        assertTrue(monitor.getItemMeta().hasLore());
    }

    @Test
    void greedyMenuPagination() {
        Block block = world.getBlockAt(0, 64, 0);
        block.setType(Material.SLIME_BLOCK);
        NodeBlob blob = NodeBlob.create(DeviceType.MVN_GREEDY_CELL.name());

        // Add 40 different types of items (more than the 36 slots per page)
        Material[] materials = {
                Material.STONE, Material.COBBLESTONE, Material.ANDESITE, Material.DIORITE, Material.GRANITE,
                Material.DIRT, Material.COARSE_DIRT, Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG,
                Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.MANGROVE_LOG, Material.CHERRY_LOG,
                Material.SAND, Material.RED_SAND, Material.GRAVEL, Material.OAK_PLANKS, Material.SPRUCE_PLANKS,
                Material.BIRCH_PLANKS, Material.JUNGLE_PLANKS, Material.ACACIA_PLANKS, Material.DARK_OAK_PLANKS, Material.GLASS,
                Material.WHITE_WOOL, Material.ORANGE_WOOL, Material.MAGENTA_WOOL, Material.LIGHT_BLUE_WOOL, Material.YELLOW_WOOL,
                Material.LIME_WOOL, Material.PINK_WOOL, Material.GRAY_WOOL, Material.LIGHT_GRAY_WOOL, Material.CYAN_WOOL,
                Material.PURPLE_WOOL, // index 35 (last slot of page 1)
                Material.BLUE_WOOL,   // index 36 (first slot of page 2)
                Material.BROWN_WOOL,  // index 37
                Material.GREEN_WOOL,  // index 38
                Material.RED_WOOL     // index 39
        };

        for (int i = 0; i < materials.length; i++) {
            blob.addGreedyItem(new ItemStack(materials[i]), (i + 1) * 100);
        }
        NodeStore.put(block, blob);

        GreedyMenu menu = new GreedyMenu(plugin, player, block);
        menu.openMenu();

        // Page 1 assertions
        assertEquals(Material.STONE, player.getOpenInventory().getTopInventory().getItem(0).getType());
        assertEquals(Material.PURPLE_WOOL, player.getOpenInventory().getTopInventory().getItem(35).getType());

        // Check page indicator at slot 40
        ItemStack indicator = player.getOpenInventory().getTopInventory().getItem(40);
        assertNotNull(indicator);
        assertEquals(Material.PAPER, indicator.getType());

        // Click slot 44 (Next Page)
        InventoryClickEvent nextPageClick = new InventoryClickEvent(
                player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER,
                44,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL
        );
        server.getPluginManager().callEvent(nextPageClick);
        server.getScheduler().performOneTick();

        // Page 2 assertions: slot 0 should now be BLUE_WOOL (index 36)
        assertEquals(Material.BLUE_WOOL, player.getOpenInventory().getTopInventory().getItem(0).getType());
        assertEquals(Material.BROWN_WOOL, player.getOpenInventory().getTopInventory().getItem(1).getType());
        assertEquals(Material.GREEN_WOOL, player.getOpenInventory().getTopInventory().getItem(2).getType());
        assertEquals(Material.RED_WOOL, player.getOpenInventory().getTopInventory().getItem(3).getType());
        // Slot 4 should be empty slot icon
        assertEquals(Material.LIGHT_GRAY_STAINED_GLASS_PANE, player.getOpenInventory().getTopInventory().getItem(4).getType());

        // Click slot 36 (Previous Page)
        InventoryClickEvent prevPageClick = new InventoryClickEvent(
                player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER,
                36,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL
        );
        server.getPluginManager().callEvent(prevPageClick);
        server.getScheduler().performOneTick();

        // Back to Page 1
        assertEquals(Material.STONE, player.getOpenInventory().getTopInventory().getItem(0).getType());
    }

    @Test
    void terminalMenuPurgerToggleAndGreedyBadge() {
        // Build a network with Controller, Terminal, Greedy Cell, and Purger
        Block controller = world.getBlockAt(0, 64, 0);
        controller.setType(Material.LODESTONE);
        NodeStore.put(controller, NodeBlob.create(DeviceType.MVN_CONTROLLER.name()));
        plugin.networks().registerController(controller);

        Block cable = world.getBlockAt(1, 64, 0);
        cable.setType(Material.IRON_BARS);
        NodeStore.put(cable, NodeBlob.create(DeviceType.MVN_CABLE.name()));

        Block terminal = world.getBlockAt(2, 64, 0);
        terminal.setType(Material.GLOWSTONE);
        NodeStore.put(terminal, NodeBlob.create(DeviceType.MVN_TERMINAL.name()));

        Block greedy = world.getBlockAt(3, 64, 0);
        greedy.setType(Material.SLIME_BLOCK);
        NodeBlob greedyBlob = NodeBlob.create(DeviceType.MVN_GREEDY_CELL.name());
        greedyBlob.addGreedyItem(new ItemStack(Material.EMERALD), 500);
        NodeStore.put(greedy, greedyBlob);

        Block purger = world.getBlockAt(4, 64, 0);
        purger.setType(Material.MAGMA_BLOCK);
        NodeBlob purgerBlob = NodeBlob.create(DeviceType.MVN_PURGER.name());
        purgerBlob.filterMaterials.add(Material.ROTTEN_FLESH.name());
        NodeStore.put(purger, purgerBlob);

        Network net = plugin.networks().networkAt(controller);
        assertNotNull(net, "Network must be discovered");
        net.scan();

        assertEquals(1, net.storage().countActiveGreedyCells());
        assertEquals(1, net.storage().countActivePurgers());
        assertEquals(500, net.storage().getGreedyStoredAmount(new ItemStack(Material.EMERALD)));
        assertTrue(net.storage().isItemPurged(new ItemStack(Material.ROTTEN_FLESH)));
        assertFalse(net.storage().isItemPurged(new ItemStack(Material.DIAMOND)));

        // Open TerminalMenu
        TerminalMenu menu = new TerminalMenu(plugin, player, net);
        menu.openMenu();

        // Slot 17 is PURGER_TOGGLE_SLOT
        ItemStack slot17 = player.getOpenInventory().getTopInventory().getItem(17);
        assertNotNull(slot17);
        assertEquals(Material.MAGMA_BLOCK, slot17.getType(), "Normal view should show MAGMA_BLOCK toggle");

        // Click slot 17 to switch to Purger Mode
        InventoryClickEvent toggleClick = new InventoryClickEvent(
                player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER,
                17,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL
        );
        server.getPluginManager().callEvent(toggleClick);
        server.getScheduler().performOneTick();

        ItemStack slot17PurgerMode = player.getOpenInventory().getTopInventory().getItem(17);
        assertNotNull(slot17PurgerMode);
        assertEquals(Material.LAVA_BUCKET, slot17PurgerMode.getType(), "Purger view should show LAVA_BUCKET toggle");

        // Click again to return to Normal Mode
        InventoryClickEvent toggleClick2 = new InventoryClickEvent(
                player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER,
                17,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL
        );
        server.getPluginManager().callEvent(toggleClick2);
        server.getScheduler().performOneTick();
        ItemStack slot17NormalAgain = player.getOpenInventory().getTopInventory().getItem(17);
        assertNotNull(slot17NormalAgain);
        assertEquals(Material.MAGMA_BLOCK, slot17NormalAgain.getType());
    }

    @Test
    void purgedItemsViewReadsSingleIdentityPerFilterEntry() {
        // Build a network with a Controller and a Purger whose filter holds the SAME entry
        // in both lists (custom template in filterItems + vanilla material name in
        // filterMaterials), exactly like FilterMenu.addFilterItem produces.
        Block controller = world.getBlockAt(0, 64, 0);
        controller.setType(Material.LODESTONE);
        NodeStore.put(controller, NodeBlob.create(DeviceType.MVN_CONTROLLER.name()));
        plugin.networks().registerController(controller);

        Block purger = world.getBlockAt(1, 64, 0);
        purger.setType(Material.MAGMA_BLOCK);
        NodeBlob purgerBlob = NodeBlob.create(DeviceType.MVN_PURGER.name());
        ItemStack custom = Items.create(DeviceType.MVN_CELL_T1);
        purgerBlob.filterItems.add(custom);
        purgerBlob.filterMaterials.add(custom.getType().name());
        NodeStore.put(purger, purgerBlob);

        Network net = plugin.networks().networkAt(controller);
        assertNotNull(net, "Network must be discovered");
        net.scan();

        List<NetworkStorage.View> purged = net.storage().getPurgedItemsView();
        assertEquals(1, purged.size(),
                "A single filter entry must be read once, not twice (custom id + vanilla id)");
        ItemStack sample = purged.get(0).sample();
        assertEquals(Material.TERRACOTTA, sample.getType());
        assertEquals(DeviceType.MVN_CELL_T1, Items.typeOf(sample),
                "The read identity must be the custom id, not a vanilla reconstruction");
    }

    @Test
    void breakingGreedyCellPreservesMultiItemCargo() {
        Block block = world.getBlockAt(0, 64, 0);
        block.setType(Material.SLIME_BLOCK);
        NodeBlob blob = NodeBlob.create(DeviceType.MVN_GREEDY_CELL.name());
        blob.addGreedyItem(new ItemStack(Material.COPPER_INGOT), 128);
        blob.addGreedyItem(new ItemStack(Material.GOLD_INGOT), 256);
        NodeStore.put(block, blob);

        BlockBreakEvent breakEvent = new BlockBreakEvent(block, player);
        server.getPluginManager().callEvent(breakEvent);

        // World should contain dropped entity
        org.bukkit.entity.Item droppedEntity = (org.bukkit.entity.Item) world.getEntities().stream()
                .filter(e -> e instanceof org.bukkit.entity.Item)
                .findFirst()
                .orElse(null);
        assertNotNull(droppedEntity, "World must contain dropped entity");
        ItemStack dropped = droppedEntity.getItemStack();
        assertNotNull(dropped);
        assertEquals(Material.SLIME_BLOCK, dropped.getType());
        assertTrue(dropped.hasItemMeta());
        String cargo = dropped.getItemMeta().getPersistentDataContainer().get(Keys.CELL_CARGO, PersistentDataType.STRING);
        assertNotNull(cargo, "CELL_CARGO PDC key must be populated");

        NodeBlob decoded = NodeStore.decode(cargo);
        assertNotNull(decoded);
        assertEquals(384, decoded.totalGreedyAmount());
        assertEquals(2, decoded.greedySamples.size());

        // Restore cargo by placing block
        Block newPos = world.getBlockAt(10, 64, 10);
        newPos.setType(Material.SLIME_BLOCK);
        player.getInventory().setItemInMainHand(dropped);
        org.bukkit.event.block.BlockPlaceEvent placeEvent = new org.bukkit.event.block.BlockPlaceEvent(
                newPos, newPos.getState(), block, dropped, player, true, org.bukkit.inventory.EquipmentSlot.HAND
        );
        server.getPluginManager().callEvent(placeEvent);

        NodeBlob restored = NodeStore.get(newPos);
        assertNotNull(restored);
        assertEquals(384, restored.totalGreedyAmount());
        assertEquals(2, restored.greedySamples.size());
        assertEquals(Material.COPPER_INGOT, restored.greedySamples.get(0).getType());
        assertEquals(128, restored.greedyAmounts.get(0));
        assertEquals(Material.GOLD_INGOT, restored.greedySamples.get(1).getType());
        assertEquals(256, restored.greedyAmounts.get(1));
    }
}

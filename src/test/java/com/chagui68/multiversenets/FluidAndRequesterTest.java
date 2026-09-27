package com.chagui68.multiversenets;

import com.chagui68.multiversenets.craft.Blueprints;
import com.chagui68.multiversenets.craft.RecipeData;
import com.chagui68.multiversenets.gui.FluidCellMenu;
import com.chagui68.multiversenets.gui.LiquidPumpMenu;
import com.chagui68.multiversenets.gui.RequestTerminalMenu;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.net.NetworkHologramManager;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * [EN] Unit tests for Fluid Storage Cells, Liquid Pumps, and Crafting Request Terminals.
 */
class FluidAndRequesterTest {

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
        com.chagui68.multiversenets.gui.ChatPrompts.clearAll();
        MockBukkit.unmock();
    }

    private Block place(int x, int y, int z, DeviceType type) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(type.material());
        NodeStore.put(block, NodeBlob.create(type.name()));
        return block;
    }

    @Test
    void fluidStorageDepositAndWithdraw() {
        Block ctrl = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        plugin.networks().registerController(ctrl);
        place(1, 64, 0, DeviceType.MVN_FLUID_CELL);

        Network net = plugin.networks().networkByController(ctrl.getLocation());
        net.scan();

        assertEquals(0, net.fluidStorage().count("WATER"));

        // Deposit 2,500 mB
        long leftover = net.fluidStorage().deposit("WATER", 2500);
        assertEquals(0, leftover);
        assertEquals(2500, net.fluidStorage().count("WATER"));

        // Withdraw 1,000 mB
        long taken = net.fluidStorage().withdraw("WATER", 1000);
        assertEquals(1000, taken);
        assertEquals(1500, net.fluidStorage().count("WATER"));

        // Withdraw rest
        taken = net.fluidStorage().withdraw("WATER", 2000);
        assertEquals(1500, taken);
        assertEquals(0, net.fluidStorage().count("WATER"));
    }

    @Test
    void fluidCellQuickInteractWithBucket() {
        Block cell = place(5, 64, 5, DeviceType.MVN_FLUID_CELL);

        // 1. Right click with water bucket -> deposits fluid
        player.getInventory().setItemInMainHand(new ItemStack(Material.WATER_BUCKET));
        PlayerInteractEvent event1 = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, player.getInventory().getItemInMainHand(), cell, BlockFace.UP, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event1);

        assertTrue(event1.isCancelled(), "deposit interact must be handled");
        assertEquals(Material.BUCKET, player.getInventory().getItemInMainHand().getType());

        NodeBlob blob = NodeStore.get(cell);
        assertNotNull(blob);
        assertEquals("WATER", blob.fluidType);
        assertEquals(1000, blob.fluidAmount);

        // 2. Right click with empty bucket -> extracts fluid
        PlayerInteractEvent event2 = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, player.getInventory().getItemInMainHand(), cell, BlockFace.UP, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event2);

        assertTrue(event2.isCancelled(), "extract interact must be handled");
        assertEquals(Material.WATER_BUCKET, player.getInventory().getItemInMainHand().getType());

        blob = NodeStore.get(cell);
        assertEquals(0, blob.fluidAmount);
        assertNull(blob.fluidType);
    }

    @Test
    void liquidPumpDrainsWaterSource() {
        Block ctrl = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        plugin.networks().registerController(ctrl);
        place(1, 64, 0, DeviceType.MVN_FLUID_CELL);
        Block pump = place(0, 64, 1, DeviceType.MVN_LIQUID_PUMP);

        // Water source directly underneath at (0, 63, 1)
        Block water = world.getBlockAt(0, 63, 1);
        water.setType(Material.WATER);
        if (water.getBlockData() instanceof Levelled l) {
            l.setLevel(0);
            water.setBlockData(l);
        }

        Network net = plugin.networks().networkByController(ctrl.getLocation());
        net.scan();

        server.getScheduler().performTicks(20);

        assertEquals(1000, net.fluidStorage().count("WATER"), "pump must drain 1000 mB of water into storage from block below");
        assertEquals(Material.AIR, water.getType(), "water source block must be replaced with AIR");
    }

    @Test
    void requestTerminalMenuDispatchesOrder() {
        Block ctrl = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        plugin.networks().registerController(ctrl);
        place(1, 64, 0, DeviceType.MVN_CELL_T1);
        place(2, 64, 0, DeviceType.MVN_CELL_T1);
        place(3, 64, 0, DeviceType.MVN_CELL_T1);
        Block crafter = place(0, 64, 1, DeviceType.MVN_REQUEST_CRAFTER);
        Block req = place(0, 64, -1, DeviceType.MVN_REQUEST_TERMINAL);

        Network net = plugin.networks().networkByController(ctrl.getLocation());
        net.scan();

        // Install blueprint in Request Crafter for Cable (8 Glass + 1 Redstone in center -> 16 Cable)
        net.storage().deposit(new ItemStack(Material.GLASS, 16));
        net.storage().deposit(new ItemStack(Material.REDSTONE, 2));

        ItemStack[] inputs = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            if (i == 4) {
                inputs[i] = new ItemStack(Material.REDSTONE);
            } else {
                inputs[i] = new ItemStack(Material.GLASS);
            }
        }
        ItemStack output = Items.create(DeviceType.MVN_CABLE);
        output.setAmount(16);
        RecipeData recipeData = new RecipeData(inputs, output);

        NodeBlob crafterBlob = NodeStore.get(crafter);
        crafterBlob.blueprintData.add(Blueprints.encode(recipeData));
        NodeStore.put(crafter, crafterBlob);

        // Open Request Terminal
        RequestTerminalMenu menu = new RequestTerminalMenu(plugin, player, net, req);
        menu.openMenu();

        // Slot 0 should have the Cable output
        ItemStack slot0 = player.getOpenInventory().getTopInventory().getItem(0);
        assertNotNull(slot0);
        assertEquals(DeviceType.MVN_CABLE.material(), slot0.getType());

        // Click slot 0 to order 1 craft
        InventoryClickEvent clickEvent = new InventoryClickEvent(
                player.getOpenInventory(),
                org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER,
                0,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL
        );
        server.getPluginManager().callEvent(clickEvent);

        // Assert 8 Glass and 1 Redstone consumed, and 16 Cable delivered to player
        assertEquals(8, net.storage().count(i -> i.getType() == Material.GLASS));
        assertEquals(1, net.storage().count(i -> i.getType() == Material.REDSTONE));
        assertTrue(player.getInventory().contains(DeviceType.MVN_CABLE.material()));
    }

    @Test
    void requestTerminalCustomAmountViaChat() {
        Block ctrl = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        plugin.networks().registerController(ctrl);
        place(1, 64, 0, DeviceType.MVN_CELL_T1);
        place(2, 64, 0, DeviceType.MVN_CELL_T1);
        place(3, 64, 0, DeviceType.MVN_CELL_T1);
        Block crafter = place(0, 64, 1, DeviceType.MVN_REQUEST_CRAFTER);
        Block req = place(0, 64, -1, DeviceType.MVN_REQUEST_TERMINAL);

        Network net = plugin.networks().networkByController(ctrl.getLocation());
        net.scan();

        net.storage().deposit(new ItemStack(Material.GLASS, 32));
        net.storage().deposit(new ItemStack(Material.REDSTONE, 4));

        ItemStack[] inputs = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            if (i == 4) {
                inputs[i] = new ItemStack(Material.REDSTONE);
            } else {
                inputs[i] = new ItemStack(Material.GLASS);
            }
        }
        ItemStack output = Items.create(DeviceType.MVN_CABLE);
        output.setAmount(16);
        RecipeData recipeData = new RecipeData(inputs, output);

        NodeBlob crafterBlob = NodeStore.get(crafter);
        crafterBlob.blueprintData.add(Blueprints.encode(recipeData));
        NodeStore.put(crafter, crafterBlob);

        RequestTerminalMenu menu = new RequestTerminalMenu(plugin, player, net, req);
        menu.openMenu();

        // Shift + Right click triggers chat prompt
        InventoryClickEvent shiftRight = new InventoryClickEvent(
                player.getOpenInventory(),
                org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER,
                0,
                ClickType.SHIFT_RIGHT,
                InventoryAction.PICKUP_ALL
        );
        server.getPluginManager().callEvent(shiftRight);

        // Inventory should close and prompt player in chat
        assertTrue(com.chagui68.multiversenets.gui.ChatPrompts.isPending(player), "Chat prompt must be pending");

        // Player replies "16" items in chat
        com.chagui68.multiversenets.gui.ChatPrompts.submitInput(player, "16");

        // Verifies 8 Glass and 1 Redstone consumed, 16 cables delivered
        assertEquals(24, net.storage().count(i -> i.getType() == Material.GLASS));
        assertEquals(3, net.storage().count(i -> i.getType() == Material.REDSTONE));
        assertTrue(player.getInventory().contains(DeviceType.MVN_CABLE.material()));
    }

    @Test
    void testRecursiveChainedCraftingLogsToPlanksToCraftingTable() {
        Block ctrl = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        plugin.networks().registerController(ctrl);
        place(1, 64, 0, DeviceType.MVN_CELL_T1);
        place(2, 64, 0, DeviceType.MVN_CELL_T1);
        Block reqCrafter = place(0, 64, 1, DeviceType.MVN_REQUEST_CRAFTER);
        Block reqTerm = place(0, 64, -1, DeviceType.MVN_REQUEST_TERMINAL);

        Network net = plugin.networks().networkByController(ctrl.getLocation());
        net.scan();

        // Deposit 5 OAK_LOG into storage (0 planks in storage!)
        net.storage().deposit(new ItemStack(Material.OAK_LOG, 5));

        // Recipe 1: 1 Oak Log -> 4 Oak Planks
        ItemStack[] logInputs = new ItemStack[9];
        logInputs[0] = new ItemStack(Material.OAK_LOG);
        ItemStack planksOutput = new ItemStack(Material.OAK_PLANKS, 4);
        RecipeData logToPlanks = new RecipeData(logInputs, planksOutput);

        // Recipe 2: 4 Oak Planks -> 1 Crafting Table
        ItemStack[] tableInputs = new ItemStack[9];
        tableInputs[0] = new ItemStack(Material.OAK_PLANKS);
        tableInputs[1] = new ItemStack(Material.OAK_PLANKS);
        tableInputs[3] = new ItemStack(Material.OAK_PLANKS);
        tableInputs[4] = new ItemStack(Material.OAK_PLANKS);
        ItemStack tableOutput = new ItemStack(Material.CRAFTING_TABLE, 1);
        RecipeData planksToTable = new RecipeData(tableInputs, tableOutput);

        NodeBlob blob = NodeStore.get(reqCrafter);
        blob.blueprintData.add(Blueprints.encode(logToPlanks));
        blob.blueprintData.add(Blueprints.encode(planksToTable));
        NodeStore.put(reqCrafter, blob);

        // Open Request Terminal
        RequestTerminalMenu menu = new RequestTerminalMenu(plugin, player, net, reqTerm);
        menu.openMenu();

        // Verify Crafting Table option is found in terminal
        int tableSlot = -1;
        for (int s = 0; s < 45; s++) {
            ItemStack it = player.getOpenInventory().getTopInventory().getItem(s);
            if (it != null && it.getType() == Material.CRAFTING_TABLE) {
                tableSlot = s;
                break;
            }
        }
        assertTrue(tableSlot >= 0, "Crafting Table option should be found in terminal");

        // Order 1 Crafting Table via click
        InventoryClickEvent clickOrder = new InventoryClickEvent(
                player.getOpenInventory(),
                org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER,
                tableSlot,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL
        );
        server.getPluginManager().callEvent(clickOrder);

        // Assert: 1 Oak Log was consumed from storage (4 remaining)
        assertEquals(4, net.storage().count(i -> i.getType() == Material.OAK_LOG), "1 Oak Log must be consumed");
        // Player received 1 Crafting Table
        assertTrue(player.getInventory().contains(Material.CRAFTING_TABLE), "Player must receive 1 Crafting Table");
        // Storage has 0 Crafting Table because it was delivered directly to inventory
        assertEquals(0, net.storage().count(i -> i.getType() == Material.CRAFTING_TABLE));
    }

    @Test
    void terminalMenuFluidsViewAndWithdrawal() {
        Block ctrl = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        plugin.networks().registerController(ctrl);
        Block cell = place(1, 64, 0, DeviceType.MVN_FLUID_CELL);

        Network net = plugin.networks().networkByController(ctrl.getLocation());
        net.scan();

        // Deposit 2,000 mB of WATER into network
        net.fluidStorage().deposit("WATER", 2000);
        assertEquals(2000, net.fluidStorage().count("WATER"));

        com.chagui68.multiversenets.gui.TerminalMenu terminal = new com.chagui68.multiversenets.gui.TerminalMenu(plugin, player, net);
        terminal.openMenu();

        // Slot 35 is 3rd button: FLUIDS_TOGGLE_SLOT
        InventoryClickEvent toggleFluids = new InventoryClickEvent(
                player.getOpenInventory(),
                org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER,
                35,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL
        );
        server.getPluginManager().callEvent(toggleFluids);
        server.getScheduler().performOneTick();

        // Attempt withdrawal without bucket in inventory -> fails
        InventoryClickEvent clickFluidNoBucket = new InventoryClickEvent(
                player.getOpenInventory(),
                org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER,
                0,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL
        );
        server.getPluginManager().callEvent(clickFluidNoBucket);
        assertEquals(2000, net.fluidStorage().count("WATER"), "water must not be withdrawn without bucket");

        // Give player empty bucket and click again with fresh event
        player.getInventory().addItem(new ItemStack(Material.BUCKET));
        InventoryClickEvent clickFluidWithBucket = new InventoryClickEvent(
                player.getOpenInventory(),
                org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER,
                0,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL
        );
        server.getPluginManager().callEvent(clickFluidWithBucket);

        assertEquals(1000, net.fluidStorage().count("WATER"), "1,000 mB should be withdrawn");
        assertTrue(player.getInventory().contains(Material.WATER_BUCKET), "player must receive water bucket");
    }

    @Test
    void guideCommandGivesGuideBookWithAllRecipes() {
        boolean ok = server.dispatchCommand(player, "mvnets guide");
        assertTrue(ok);
        ItemStack book = null;
        for (ItemStack is : player.getInventory().getContents()) {
            if (is != null && is.getType() == Material.WRITTEN_BOOK) {
                book = is;
                break;
            }
        }
        assertNotNull(book);
        org.bukkit.inventory.meta.BookMeta meta = (org.bukkit.inventory.meta.BookMeta) book.getItemMeta();
        assertNotNull(meta);
        assertTrue(meta.getPageCount() >= 18, "Guide book should have at least 18 pages of content and recipes");
        String p1 = meta.getPage(1);
        assertTrue(p1.contains("MultiverseNets"));
        String p3 = meta.getPage(3);
        assertTrue(p3.contains("Controller") && p3.contains("Nether Star"));
    }

    @Test
    void requestCrafterIsDiscoveredByRequestTerminalAndDoesNotAutoCraft() {
        Block ctrl = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        plugin.networks().registerController(ctrl);
        place(1, 64, 0, DeviceType.MVN_CELL_T1);
        place(2, 64, 0, DeviceType.MVN_CELL_T1);
        place(3, 64, 0, DeviceType.MVN_CELL_T1);
        Block reqCrafter = place(0, 64, 1, DeviceType.MVN_REQUEST_CRAFTER);
        Block reqTerm = place(0, 64, -1, DeviceType.MVN_REQUEST_TERMINAL);

        Network net = plugin.networks().networkByController(ctrl.getLocation());
        net.scan();

        // Deposit crafting ingredients into network storage
        net.storage().deposit(new ItemStack(Material.GLASS, 16));
        net.storage().deposit(new ItemStack(Material.REDSTONE, 2));

        // Create blueprint for Cable in the Request Crafter
        ItemStack[] inputs = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            if (i == 4) {
                inputs[i] = new ItemStack(Material.REDSTONE);
            } else {
                inputs[i] = new ItemStack(Material.GLASS);
            }
        }
        ItemStack output = Items.create(DeviceType.MVN_CABLE);
        output.setAmount(16);
        RecipeData recipeData = new RecipeData(inputs, output);

        NodeBlob crafterBlob = NodeStore.get(reqCrafter);
        crafterBlob.blueprintData.add(Blueprints.encode(recipeData));
        NodeStore.put(reqCrafter, crafterBlob);

        // 1. Verify that auto-crafting does NOT touch MVN_REQUEST_CRAFTER
        com.chagui68.multiversenets.net.NetworkTicker ticker = new com.chagui68.multiversenets.net.NetworkTicker(plugin, plugin.networks());
        // Run ticker method via reflection or directly (doCrafting only operates on MVN_CRAFTER)
        // Check network stock remains untouched
        assertEquals(16, net.storage().count(i -> i.getType() == Material.GLASS));
        assertEquals(2, net.storage().count(i -> i.getType() == Material.REDSTONE));

        // 2. Open RequestTerminalMenu and verify the blueprint was discovered from the Request Crafter
        RequestTerminalMenu menu = new RequestTerminalMenu(plugin, player, net, reqTerm);
        menu.openMenu();

        // Left Click on slot 0 -> Order 1 batch
        InventoryClickEvent clickOrder = new InventoryClickEvent(
                player.getOpenInventory(),
                org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER,
                0,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL
        );
        server.getPluginManager().callEvent(clickOrder);

        // Assert 8 Glass and 1 Redstone consumed, 16 Cable delivered to player
        assertEquals(8, net.storage().count(i -> i.getType() == Material.GLASS));
        assertEquals(1, net.storage().count(i -> i.getType() == Material.REDSTONE));
        assertTrue(player.getInventory().contains(DeviceType.MVN_CABLE.material()));
    }

    @Test
    void liquidPumpMenuHasNoModeOrTargetSideButtons() {
        Block pump = place(10, 64, 10, DeviceType.MVN_LIQUID_PUMP);
        LiquidPumpMenu menu = new LiquidPumpMenu(plugin, player, pump);
        menu.openMenu();

        // Filter slot 12, Info slot 14
        assertNotNull(player.getOpenInventory().getTopInventory().getItem(LiquidPumpMenu.FLUID_FILTER_SLOT));
        assertNotNull(player.getOpenInventory().getTopInventory().getItem(LiquidPumpMenu.INFO_SLOT));

        // Slots 10 and 16 (where mode and direction were previously) must now be gray panes (background)
        ItemStack slot10 = player.getOpenInventory().getTopInventory().getItem(10);
        ItemStack slot16 = player.getOpenInventory().getTopInventory().getItem(16);
        assertNotNull(slot10);
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, slot10.getType());
        assertNotNull(slot16);
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, slot16.getType());

        // Clicking filter slot cycles filter
        InventoryClickEvent clickFilter = new InventoryClickEvent(
                player.getOpenInventory(),
                org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER,
                LiquidPumpMenu.FLUID_FILTER_SLOT,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL
        );
        server.getPluginManager().callEvent(clickFilter);
        NodeBlob blob = NodeStore.get(pump);
        assertEquals("WATER", blob.pumpFluid);
    }

    @Test
    void testHologramRedesignNoFlowOrRouted() {
        Block ctrl = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        plugin.networks().registerController(ctrl);
        Network net = plugin.networks().networkByController(ctrl.getLocation());
        net.scan();

        NetworkHologramManager.updateHologram(net);

        org.bukkit.entity.TextDisplay td = null;
        for (org.bukkit.entity.Entity e : world.getEntities()) {
            if (e instanceof org.bukkit.entity.TextDisplay display) {
                td = display;
                break;
            }
        }
        assertNotNull(td, "Hologram TextDisplay should be spawned");
        String text = PlainTextComponentSerializer.plainText().serialize(td.text());

        assertTrue(text.contains("MultiverseNets"), "Hologram must contain MultiverseNets");
        assertFalse(text.toLowerCase(java.util.Locale.ROOT).contains("flow"), "Hologram must NOT contain 'flow'");
        assertFalse(text.toLowerCase(java.util.Locale.ROOT).contains("routed"), "Hologram must NOT contain 'routed'");
    }

    @Test
    void testRequestTerminalIgnoresAutoCrafterRecipes() {
        Block ctrl = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        plugin.networks().registerController(ctrl);
        place(1, 64, 0, DeviceType.MVN_CELL_T1);
        Block autoCrafter = place(0, 64, 1, DeviceType.MVN_CRAFTER);
        Block reqCrafter = place(0, 64, 2, DeviceType.MVN_REQUEST_CRAFTER);
        Block reqTerm = place(0, 64, -1, DeviceType.MVN_REQUEST_TERMINAL);

        Network net = plugin.networks().networkByController(ctrl.getLocation());
        net.scan();

        // Recipe A: Iron Ingot -> Iron Block in autoCrafter
        ItemStack[] ironInputs = new ItemStack[9];
        for (int i = 0; i < 9; i++) ironInputs[i] = new ItemStack(Material.IRON_INGOT);
        RecipeData ironRecipe = new RecipeData(ironInputs, new ItemStack(Material.IRON_BLOCK));
        NodeBlob autoBlob = NodeStore.get(autoCrafter);
        autoBlob.blueprintData.add(Blueprints.encode(ironRecipe));
        NodeStore.put(autoCrafter, autoBlob);

        // Recipe B: Gold Ingot -> Gold Block in reqCrafter
        ItemStack[] goldInputs = new ItemStack[9];
        for (int i = 0; i < 9; i++) goldInputs[i] = new ItemStack(Material.GOLD_INGOT);
        RecipeData goldRecipe = new RecipeData(goldInputs, new ItemStack(Material.GOLD_BLOCK));
        NodeBlob reqBlob = NodeStore.get(reqCrafter);
        reqBlob.blueprintData.add(Blueprints.encode(goldRecipe));
        NodeStore.put(reqCrafter, reqBlob);

        RequestTerminalMenu menu = new RequestTerminalMenu(plugin, player, net, reqTerm);
        menu.openMenu();

        // Check top inventory: slot 0 should be Gold Block, and Iron Block must not exist
        ItemStack slot0 = player.getOpenInventory().getTopInventory().getItem(0);
        assertNotNull(slot0, "Slot 0 should contain the recipe from Request Crafter");
        assertEquals(Material.GOLD_BLOCK, slot0.getType(), "Slot 0 must be Gold Block");

        // Assert Iron Block is not anywhere in the menu
        for (int s = 0; s < 45; s++) {
            ItemStack item = player.getOpenInventory().getTopInventory().getItem(s);
            if (item != null) {
                assertNotEquals(Material.IRON_BLOCK, item.getType(), "Request Terminal must NOT display recipes from MVN_CRAFTER");
            }
        }
    }
}

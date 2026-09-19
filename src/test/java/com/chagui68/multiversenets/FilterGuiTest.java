package com.chagui68.multiversenets;

import com.chagui68.multiversenets.gui.FilterMenu;
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
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EN] Tests FilterMenu GUI interactions: adding items, removing filters, toggling whitelist/blacklist, shift-clicking, and directional faces.
 * [ES] Pruebas de la GUI FilterMenu: añadir items, retirar filtros, alternar whitelist/blacklist, shift-clic y caras direccionales.
 */
class FilterGuiTest {

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

    private Block place(DeviceType type) {
        Block block = world.getBlockAt(0, 64, 0);
        block.setType(type.material());
        NodeStore.put(block, NodeBlob.create(type.name()));
        return block;
    }

    private void clickTop(int raw, ClickType type, InventoryAction action) {
        InventoryClickEvent click = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, raw, type, action);
        server.getPluginManager().callEvent(click);
    }

    /**
     * [EN] Right clicking a pusher opens its FilterMenu GUI.
     * [ES] Clic derecho en un pusher abre su menú FilterMenu.
     */
    @Test
    void pusherOpensFilterMenu() {
        Block pusher = place(DeviceType.MVN_PUSHER);
        PlayerInteractEvent interact = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK,
                null, pusher, BlockFace.NORTH, EquipmentSlot.HAND, null);
        server.getPluginManager().callEvent(interact);
        assertTrue(interact.isCancelled(), "interact event must be cancelled to prevent block placement");
        assertTrue(player.getOpenInventory().getTopInventory().getHolder() instanceof FilterMenu,
                "right click on pusher opens its filter menu");
    }

    /**
     * [EN] Clicking a filter slot with an item on cursor adds the material without consuming the item.
     * [ES] Clic con un item en el cursor añade el material sin consumir el item.
     */
    @Test
    void clickWithItemAddsMaterialToFilter() {
        Block pusher = place(DeviceType.MVN_PUSHER);
        new FilterMenu(plugin, player, pusher, DeviceType.MVN_PUSHER).openMenu();
        player.getOpenInventory().setCursor(new ItemStack(Material.DIAMOND, 7));

        clickTop(0, ClickType.LEFT, InventoryAction.PICKUP_ALL);

        NodeBlob blob = NodeStore.get(pusher);
        assertTrue(blob.filterMaterials.contains("DIAMOND"),
                "cursor item material must be added to filter");
        assertEquals(7, player.getItemOnCursor().getAmount(), "cursor item must not be consumed");
    }

    /**
     * [EN] Clicking an existing filter icon with empty cursor removes it from the filter.
     * [ES] Clic sobre un icono existente con el cursor vacío lo elimina del filtro.
     */
    @Test
    void clickWithEmptyCursorRemovesMaterial() {
        Block pusher = place(DeviceType.MVN_PUSHER);
        NodeBlob blob = NodeStore.get(pusher);
        blob.filterMaterials.add("DIAMOND");
        NodeStore.put(pusher, blob);

        new FilterMenu(plugin, player, pusher, DeviceType.MVN_PUSHER).openMenu();
        player.getOpenInventory().setCursor(null);
        clickTop(0, ClickType.LEFT, InventoryAction.PICKUP_ALL);

        assertFalse(NodeStore.get(pusher).filterMaterials.contains("DIAMOND"),
                "click with empty cursor removes material from filter");
    }

    /**
     * [EN] Clicking mode toggle switches between whitelist and blacklist modes.
     * [ES] Clic en el botón de modo alterna entre whitelist y blacklist.
     */
    @Test
    void modeButtonTogglesBlacklist() {
        Block pusher = place(DeviceType.MVN_PUSHER);
        new FilterMenu(plugin, player, pusher, DeviceType.MVN_PUSHER).openMenu();
        clickTop(17, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        assertTrue(NodeStore.get(pusher).filterBlacklist, "first click activates blacklist mode");
        clickTop(17, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        assertFalse(NodeStore.get(pusher).filterBlacklist, "second click deactivates blacklist mode");
    }

    /**
     * [EN] Shift clicking a player inventory stack adds it to filter without moving the item.
     * [ES] Shift-clic en un item del inventario del jugador lo añade al filtro sin moverlo.
     */
    @Test
    void shiftClickOnOwnStackAddsToFilterWithoutMoving() {
        Block pusher = place(DeviceType.MVN_PUSHER);
        player.getInventory().setItem(0, new ItemStack(Material.REDSTONE, 16));
        new FilterMenu(plugin, player, pusher, DeviceType.MVN_PUSHER).openMenu();

        InventoryClickEvent shift = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 27, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY);
        server.getPluginManager().callEvent(shift);

        NodeBlob blob = NodeStore.get(pusher);
        assertTrue(blob.filterMaterials.contains("REDSTONE"),
                "shift click adds material to filter");
        assertTrue(shift.isCancelled(), "item move event cancelled to prevent dragging into GUI");
        assertEquals(16, player.getInventory().getItem(0).getAmount(), "player stack remains intact");
    }

    /**
     * [EN] Player inventory remains interactive while menu is open.
     * [ES] El inventario del jugador permanece interactivo mientras el menú está abierto.
     */
    @Test
    void playerInventoryRemainsInteractiveWithMenuOpen() {
        Block pusher = place(DeviceType.MVN_PUSHER);
        player.getInventory().setItem(0, new ItemStack(Material.STONE, 3));
        new FilterMenu(plugin, player, pusher, DeviceType.MVN_PUSHER).openMenu();
        InventoryClickEvent click = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 27, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(click);
        assertFalse(click.isCancelled(),
                "player inventory clicks should not be blocked");
    }

    /**
     * [EN] Filter matcher distinguishes Quantum Cell custom item from vanilla terracotta.
     * [ES] El matcher distingue un item custom Quantum Cell de terracota vanilla.
     */
    @Test
    void filterDistinguishesQuantumCellFromVanillaTerracotta() {
        ItemStack cellT1 = com.chagui68.multiversenets.item.Items.create(DeviceType.MVN_CELL_T1);
        ItemStack vanillaTerracotta = new ItemStack(Material.CYAN_TERRACOTTA);

        assertTrue(com.chagui68.multiversenets.net.NetworkManager.matchesFilter(cellT1, cellT1.clone()),
                "Quantum Cell T1 matches itself");
        assertFalse(com.chagui68.multiversenets.net.NetworkManager.matchesFilter(cellT1, vanillaTerracotta),
                "Quantum Cell T1 does not match vanilla terracotta");

        assertTrue(com.chagui68.multiversenets.net.NetworkManager.matchesFilter(vanillaTerracotta, vanillaTerracotta.clone()),
                "Vanilla terracotta matches itself");
        assertFalse(com.chagui68.multiversenets.net.NetworkManager.matchesFilter(vanillaTerracotta, cellT1),
                "Vanilla terracotta does not match Quantum Cell T1");
    }

    /**
     * [EN] Filter registers custom items into filterItems list upon shift click.
     * [ES] El filtro registra items custom en filterItems con shift-clic.
     */
    @Test
    void filterRegistersCustomItemsWithShiftClick() {
        Block grabber = place(DeviceType.MVN_GRABBER);
        ItemStack cellT1 = com.chagui68.multiversenets.item.Items.create(DeviceType.MVN_CELL_T1);
        player.getInventory().setItem(0, cellT1);

        new FilterMenu(plugin, player, grabber, DeviceType.MVN_GRABBER).openMenu();

        InventoryClickEvent shift = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 27, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY);
        server.getPluginManager().callEvent(shift);

        NodeBlob blob = NodeStore.get(grabber);
        assertFalse(blob.filterItems.isEmpty(), "filterItems must register custom item");
        assertEquals(DeviceType.MVN_CELL_T1, com.chagui68.multiversenets.item.Items.typeOf(blob.filterItems.get(0)),
                "registered item in filterItems must match CELL_T1");
    }

    /**
     * [EN] Clear button empties both filterItems and filterMaterials lists.
     * [ES] El botón Clear vacía tanto filterItems como filterMaterials.
     */
    @Test
    void clearButtonClearsAllFilters() {
        Block pusher = place(DeviceType.MVN_PUSHER);
        NodeBlob blob = NodeStore.get(pusher);
        blob.filterItems.add(new ItemStack(Material.IRON_INGOT));
        blob.filterMaterials.add("IRON_INGOT");
        NodeStore.put(pusher, blob);

        new FilterMenu(plugin, player, pusher, DeviceType.MVN_PUSHER).openMenu();
        clickTop(FilterMenu.CLEAR_SLOT, ClickType.LEFT, InventoryAction.PICKUP_ALL);

        NodeBlob after = NodeStore.get(pusher);
        assertTrue(after.filterItems.isEmpty(), "clear must empty filterItems");
        assertTrue(after.filterMaterials.isEmpty(), "clear must empty filterMaterials");
    }

    /**
     * [EN] Clicking a directional face button sets targetFace on advanced devices.
     * [ES] Clic en un botón de dirección establece targetFace en dispositivos avanzados.
     */
    @Test
    void directionalSelectionSetsTargetFace() {
        Block grabber = place(DeviceType.MVN_GRABBER_HT);
        Block northBlock = world.getBlockAt(0, 64, -1);
        northBlock.setType(Material.CHEST);

        new FilterMenu(plugin, player, grabber, DeviceType.MVN_GRABBER_HT).openMenu();

        clickTop(20, ClickType.LEFT, InventoryAction.PICKUP_ALL);

        NodeBlob blob = NodeStore.get(grabber);
        assertEquals("NORTH", blob.targetFace, "clicking NORTH slot sets targetFace to NORTH");

        clickTop(FilterMenu.ALL_DIRECTIONS_SLOT, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        NodeBlob blobAll = NodeStore.get(grabber);
        assertEquals("ALL", blobAll.targetFace, "clicking ALL slot sets targetFace to ALL");
    }

    /**
     * [EN] Simple grabber does not show interactive directional buttons.
     * [ES] El grabber simple no muestra botones direccionales interactivos.
     */
    @Test
    void simpleGrabberDoesNotShowDirectionalButtons() {
        Block grabber = place(DeviceType.MVN_GRABBER);
        new FilterMenu(plugin, player, grabber, DeviceType.MVN_GRABBER).openMenu();

        ItemStack slot20 = player.getOpenInventory().getTopInventory().getItem(20);
        assertNotNull(slot20);
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, slot20.getType());

        clickTop(20, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        NodeBlob blob = NodeStore.get(grabber);
        assertNull(blob.targetFace, "clicking decorative pane must not modify targetFace");
    }
}

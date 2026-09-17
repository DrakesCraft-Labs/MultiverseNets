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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La GUI de filtro (pusher, grabber, vacuum, purgador, greedy, receptor) cubierta con clicks
 * de verdad: anadir, quitar y el toggle whitelist/blacklist.
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

    private Block colocar(DeviceType tipo) {
        Block block = world.getBlockAt(0, 64, 0);
        block.setType(tipo.material());
        NodeStore.put(block, NodeBlob.create(tipo.name()));
        return block;
    }

    private void clickTop(int raw, ClickType type, InventoryAction action) {
        InventoryClickEvent click = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, raw, type, action);
        server.getPluginManager().callEvent(click);
    }

    @Test
    void elPusherAbreSuMenuDeFiltro() {
        Block pusher = colocar(DeviceType.PUSHER);
        PlayerInteractEvent interact = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK,
                null, pusher, BlockFace.NORTH, EquipmentSlot.HAND, null);
        server.getPluginManager().callEvent(interact);
        assertTrue(player.getOpenInventory().getTopInventory().getHolder() instanceof FilterMenu,
                "clic derecho al pusher abre su menu de filtro");
    }

    @Test
    void clicConItemAniadeElMaterial() {
        Block pusher = colocar(DeviceType.PUSHER);
        new FilterMenu(plugin, player, pusher, DeviceType.PUSHER).openMenu();
        player.getOpenInventory().setCursor(new ItemStack(Material.DIAMOND, 7));

        clickTop(0, ClickType.LEFT, InventoryAction.PICKUP_ALL);

        NodeBlob blob = NodeStore.get(pusher);
        assertTrue(blob.filterMaterials.contains("DIAMOND"),
                "clic con item en el cursor anade su material al filtro");
        assertEquals(7, player.getItemOnCursor().getAmount(), "el item del cursor no se consume");
    }

    @Test
    void clicSinCursorSobreIconoLoQuita() {
        Block pusher = colocar(DeviceType.PUSHER);
        NodeBlob blob = NodeStore.get(pusher);
        blob.filterMaterials.add("DIAMOND");
        NodeStore.put(pusher, blob);

        new FilterMenu(plugin, player, pusher, DeviceType.PUSHER).openMenu();
        // El icono del diamante esta en el hueco 0.
        player.getOpenInventory().setCursor(null);
        clickTop(0, ClickType.LEFT, InventoryAction.PICKUP_ALL);

        assertFalse(NodeStore.get(pusher).filterMaterials.contains("DIAMOND"),
                "clic sin cursor sobre el icono retira el material");
    }

    @Test
    void elBotonDeModoAlternaBlacklist() {
        Block pusher = colocar(DeviceType.PUSHER);
        new FilterMenu(plugin, player, pusher, DeviceType.PUSHER).openMenu();
        clickTop(17, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        assertTrue(NodeStore.get(pusher).filterBlacklist, "una pulsacion activa la blacklist");
        clickTop(17, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        assertFalse(NodeStore.get(pusher).filterBlacklist, "la segunda la desactiva");
    }

    @Test
    void shiftClickSobreUnStackPropioLoAnadeAlFiltroSinMoverlo() {
        Block pusher = colocar(DeviceType.PUSHER);
        player.getInventory().setItem(0, new ItemStack(Material.REDSTONE, 16));
        new FilterMenu(plugin, player, pusher, DeviceType.PUSHER).openMenu();

        InventoryClickEvent shift = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 27, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY);
        server.getPluginManager().callEvent(shift);

        NodeBlob blob = NodeStore.get(pusher);
        assertTrue(blob.filterMaterials.contains("REDSTONE"),
                "shift+clic sobre un stack propio lo anade al filtro");
        assertTrue(shift.isCancelled(), "el stack no se mueve a la GUI pintada");
        assertEquals(16, player.getInventory().getItem(0).getAmount(), "el stack se queda donde estaba");
    }

    @Test
    void elInventarioDelJugadorQuedaLibreConElMenuAbierto() {
        Block pusher = colocar(DeviceType.PUSHER);
        player.getInventory().setItem(0, new ItemStack(Material.STONE, 3));
        new FilterMenu(plugin, player, pusher, DeviceType.PUSHER).openMenu();
        // Clic normal sobre el inventario propio (raw 27+): vanilla, no cancelado.
        InventoryClickEvent click = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 27, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(click);
        assertFalse(click.isCancelled(),
                "los clics sobre el inventario del jugador no se tocan (no mas GUI congelada)");
    }

    @Test
    void filtroDistingueQuantumCellDeTerracotaVanilla() {
        ItemStack cellT1 = com.chagui68.multiversenets.item.Items.create(DeviceType.CELL_T1);
        ItemStack vanillaTerracotta = new ItemStack(Material.CYAN_TERRACOTTA);

        // Comprobar matcher con template de Quantum Cell T1
        assertTrue(com.chagui68.multiversenets.net.NetworkManager.matchesFilter(cellT1, cellT1.clone()),
                "Quantum Cell T1 debe coincidir consigo misma");
        assertFalse(com.chagui68.multiversenets.net.NetworkManager.matchesFilter(cellT1, vanillaTerracotta),
                "Quantum Cell T1 NO debe coincidir con terracota vanilla");

        // Comprobar matcher con template de terracota vanilla
        assertTrue(com.chagui68.multiversenets.net.NetworkManager.matchesFilter(vanillaTerracotta, vanillaTerracotta.clone()),
                "Terracota vanilla debe coincidir con terracota vanilla");
        assertFalse(com.chagui68.multiversenets.net.NetworkManager.matchesFilter(vanillaTerracotta, cellT1),
                "Terracota vanilla NO debe coincidir con Quantum Cell T1");
    }

    @Test
    void filtroRegistraItemsPersonalizadosConShiftClick() {
        Block grabber = colocar(DeviceType.GRABBER);
        ItemStack cellT1 = com.chagui68.multiversenets.item.Items.create(DeviceType.CELL_T1);
        player.getInventory().setItem(0, cellT1);

        new FilterMenu(plugin, player, grabber, DeviceType.GRABBER).openMenu();

        InventoryClickEvent shift = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 27, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY);
        server.getPluginManager().callEvent(shift);

        NodeBlob blob = NodeStore.get(grabber);
        assertFalse(blob.filterItems.isEmpty(), "blob.filterItems debe registrar el item custom");
        assertEquals(DeviceType.CELL_T1, com.chagui68.multiversenets.item.Items.typeOf(blob.filterItems.get(0)),
                "el item registrado en filterItems debe ser CELL_T1");
    }

    @Test
    void botonClearLimpiaTodosLosFiltros() {
        Block pusher = colocar(DeviceType.PUSHER);
        NodeBlob blob = NodeStore.get(pusher);
        blob.filterItems.add(new ItemStack(Material.IRON_INGOT));
        blob.filterMaterials.add("IRON_INGOT");
        NodeStore.put(pusher, blob);

        new FilterMenu(plugin, player, pusher, DeviceType.PUSHER).openMenu();
        clickTop(FilterMenu.CLEAR_SLOT, ClickType.LEFT, InventoryAction.PICKUP_ALL);

        NodeBlob after = NodeStore.get(pusher);
        assertTrue(after.filterItems.isEmpty(), "clear debe vaciar filterItems");
        assertTrue(after.filterMaterials.isEmpty(), "clear debe vaciar filterMaterials");
    }
}

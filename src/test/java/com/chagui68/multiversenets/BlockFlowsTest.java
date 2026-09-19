package com.chagui68.multiversenets;

import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.Keys;
import com.chagui68.multiversenets.util.PosUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EN] Tests block lifecycle events: breaking, placing, cargo retention, piston protections, explosion safety, and tools.
 * [ES] Pruebas del ciclo de vida de bloques: rotura, colocación, retención de carga, protección contra pistones/explosiones y herramientas.
 */
class BlockFlowsTest {

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
        return block;
    }

    private ItemStack breakBlock(Block block) {
        BlockBreakEvent event = new BlockBreakEvent(block, player);
        server.getPluginManager().callEvent(event);
        Item drop = null;
        for (var entity : world.getEntities()) {
            if (entity instanceof Item item && item.getLocation().distance(block.getLocation().add(0.5, 0.5, 0.5)) < 2) {
                drop = item;
            }
        }
        return drop == null ? null : drop.getItemStack();
    }

    // ---------------------------------------------------------------- cells and cargo

    /**
     * [EN] Breaking a cell embeds its stored cargo in the dropped item stack.
     * [ES] Romper una celda guarda su carga almacenada en el item soltado.
     */
    @Test
    void breakingCellWithCargoEmbedsCargoInItem() {
        Block cell = place(0, 64, 0, DeviceType.MVN_CELL_T2);
        NodeBlob blob = NodeStore.get(cell);
        blob.cellSample = new ItemStack(Material.REDSTONE);
        blob.cellAmount = 12345;
        NodeStore.put(cell, blob);

        ItemStack drop = breakBlock(cell);
        assertNotNull(drop, "breaking cell should drop cell item");
        assertEquals(DeviceType.MVN_CELL_T2, Items.typeOf(drop));
        String cargo = drop.getItemMeta().getPersistentDataContainer()
                .get(Keys.CELL_CARGO, PersistentDataType.STRING);
        assertNotNull(cargo, "dropped item contains embedded cargo");
        NodeBlob embedded = NodeStore.decode(cargo);
        assertNotNull(embedded);
        assertEquals(Material.REDSTONE, embedded.cellSample.getType());
        assertEquals(12345, embedded.cellAmount);

        assertNull(NodeStore.get(cell), "node is removed from chunk");
    }

    /**
     * [EN] Placing a cell restores the embedded cargo into block metadata.
     * [ES] Colocar una celda restaura la carga embebida en la metadata del bloque.
     */
    @Test
    void placingCellWithCargoRestoresState() {
        ItemStack item = Items.create(DeviceType.MVN_CELL_T1);
        NodeBlob saved = NodeBlob.create(DeviceType.MVN_CELL_T1.name());
        saved.cellSample = new ItemStack(Material.GOLD_INGOT);
        saved.cellAmount = 777;
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(Keys.CELL_CARGO, PersistentDataType.STRING, NodeStore.encode(saved));
        item.setItemMeta(meta);

        Block target = world.getBlockAt(10, 64, 0);
        org.bukkit.block.BlockState previous = target.getState();
        target.setType(DeviceType.MVN_CELL_T1.material());
        BlockPlaceEvent placeEvent = new BlockPlaceEvent(target, previous, target.getRelative(BlockFace.DOWN),
                item, player, true, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(placeEvent);

        NodeBlob restored = NodeStore.get(target);
        assertNotNull(restored);
        assertEquals(Material.GOLD_INGOT, restored.cellSample.getType());
        assertEquals(777, restored.cellAmount, "embedded cargo returns to placed block");
    }

    /**
     * [EN] Breaking a receiver preserves its transmitter binding coordinates in the dropped item.
     * [ES] Romper un receptor conserva las coordenadas de enlace del transmisor en el item caído.
     */
    @Test
    void breakingReceiverPreservesWirelessLink() {
        Block tx = place(0, 64, 0, DeviceType.MVN_TRANSMITTER);
        place(1, 64, 0, DeviceType.MVN_CABLE);
        Block rx = place(2, 64, 0, DeviceType.MVN_RECEIVER);
        NodeBlob blob = NodeStore.get(rx);
        blob.txWorld = world.getUID().toString();
        blob.txX = 0;
        blob.txY = 64;
        blob.txZ = 0;
        NodeStore.put(rx, blob);

        ItemStack drop = breakBlock(rx);
        assertNotNull(drop);
        String cargo = drop.getItemMeta().getPersistentDataContainer()
                .get(Keys.CELL_CARGO, PersistentDataType.STRING);
        assertNotNull(cargo, "broken receiver preserves wireless link in item PDC");
        assertEquals(world.getUID().toString(), NodeStore.decode(cargo).txWorld);
    }

    // ---------------------------------------------------------------- protections

    /**
     * [EN] Explosions do not destroy network node blocks.
     * [ES] Las explosiones no destruyen bloques nodo de la red.
     */
    @Test
    void explosionsDoNotBreakNodes() {
        Block cable = place(5, 64, 5, DeviceType.MVN_CABLE);
        Block other = world.getBlockAt(5, 65, 5);
        other.setType(Material.STONE);
        List<Block> affected = new ArrayList<>(List.of(cable, other));

        EntityExplodeEvent boom = new EntityExplodeEvent(null, other.getLocation(), affected, 1.0f,
                org.bukkit.ExplosionResult.DESTROY);
        server.getPluginManager().callEvent(boom);

        assertFalse(affected.contains(cable), "nodes are removed from explosion radius");
        assertTrue(affected.contains(other));
    }

    /**
     * [EN] Pistons are prevented from pushing network nodes.
     * [ES] Se cancela la extensión de pistones que empujen nodos de red.
     */
    @Test
    void pistonsDoNotMoveNodes() {
        Block grabber = place(6, 64, 6, DeviceType.MVN_GRABBER);
        Block piston = world.getBlockAt(6, 64, 7);
        piston.setType(Material.PISTON);
        BlockPistonExtendEvent extend = new BlockPistonExtendEvent(piston, List.of(grabber), BlockFace.NORTH);
        server.getPluginManager().callEvent(extend);
        assertTrue(extend.isCancelled(), "piston moving a network node must be cancelled");
    }

    // ---------------------------------------------------------------- tools and interactions

    /**
     * [EN] Binding and opening a wireless terminal via shift-click on a controller.
     * [ES] Vinculación y apertura de terminal inalámbrica con shift-click en un controlador.
     */
    @Test
    void bindAndOpenWirelessTerminal() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        plugin.networks().registerController(controller);
        place(1, 64, 0, DeviceType.MVN_CELL_T1);

        ItemStack terminal = Items.create(DeviceType.MVN_WIRELESS_TERMINAL);
        assertNotNull(terminal.getItemMeta().lore(), "initial lore must be present");
        player.setSneaking(true);
        player.getInventory().setItemInMainHand(terminal);
        var bindEvent = new PlayerInteractEvent(player,
                Action.RIGHT_CLICK_BLOCK, terminal, controller, BlockFace.NORTH, EquipmentSlot.HAND, null);
        server.getPluginManager().callEvent(bindEvent);
        player.setSneaking(false);

        Location bind = Items.readWirelessBind(terminal);
        assertNotNull(bind, "shift+click on controller binds terminal");
        assertEquals(controller.getLocation(), bind);
        assertNotNull(terminal.getItemMeta().lore(), "lore updates upon binding");
        boolean hasLinked = terminal.getItemMeta().lore().stream()
                .anyMatch(c -> c.toString().contains("Linked"));
        assertTrue(hasLinked, "lore indicates terminal is linked");
    }

    /**
     * [EN] Wireless terminal binds to network when shift-clicked on a terminal block.
     * [ES] La terminal inalámbrica se vincula al hacer shift-click sobre un bloque terminal.
     */
    @Test
    void wirelessTerminalBindsViaShiftClickOnTerminalBlock() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        plugin.networks().registerController(controller);
        Block terminalBlock = place(1, 64, 0, DeviceType.MVN_TERMINAL);
        plugin.networks().networkFor(world, PosUtil.pack(0, 64, 0)).scan();

        ItemStack wireless = Items.create(DeviceType.MVN_WIRELESS_TERMINAL);
        player.setSneaking(true);
        player.getInventory().setItemInMainHand(wireless);
        var bindEvent = new PlayerInteractEvent(player,
                Action.RIGHT_CLICK_BLOCK, wireless, terminalBlock, BlockFace.NORTH, EquipmentSlot.HAND, null);
        server.getPluginManager().callEvent(bindEvent);
        player.setSneaking(false);

        Location bind = Items.readWirelessBind(wireless);
        assertNotNull(bind, "shift+click on terminal block binds wireless terminal to controller");
        assertEquals(controller.getLocation(), bind);
    }

    /**
     * [EN] Right-clicking air with a bound wireless terminal opens the terminal GUI.
     * [ES] Hacer clic al aire con la terminal vinculada abre el menú de la terminal.
     */
    @Test
    void boundWirelessTerminalOpensTerminalInAir() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        plugin.networks().registerController(controller);
        place(1, 64, 0, DeviceType.MVN_CELL_T1);

        ItemStack terminal = Items.create(DeviceType.MVN_WIRELESS_TERMINAL);
        Items.bindWireless(terminal, controller.getLocation());
        player.getInventory().setItemInMainHand(terminal);

        var air = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, terminal, null,
                BlockFace.SELF, EquipmentSlot.HAND, null);
        server.getPluginManager().callEvent(air);

        assertTrue(player.getOpenInventory().getTopInventory().getHolder()
                        instanceof com.chagui68.multiversenets.gui.TerminalMenu,
                "right click in air with bound terminal opens TerminalMenu");
    }

    /**
     * [EN] Rake tool dismantles a node block immediately and consumes durability/uses.
     * [ES] La herramienta rastrillo desmantela un nodo de inmediato y gasta un uso.
     */
    @Test
    void rakeDismantlesNodeAndConsumesUse() {
        Block grabber = place(0, 64, 0, DeviceType.MVN_GRABBER);
        ItemStack rake = Items.rake();
        int usesBefore = Items.rakeUses(rake);
        player.getInventory().setItemInMainHand(rake);

        var event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, rake, grabber,
                BlockFace.NORTH, EquipmentSlot.HAND, null);
        server.getPluginManager().callEvent(event);

        assertTrue(event.isCancelled());
        assertEquals(Material.AIR, grabber.getType(), "rake removes block");
        assertNull(NodeStore.get(grabber), "node removed from chunk store");
        assertEquals(usesBefore - 1, Items.rakeUses(rake), "rake consumes one use");
    }

    /**
     * [EN] Rake tool rejects dismantling controllers or cells containing items.
     * [ES] El rastrillo rechaza desmantelar controladores o celdas con carga.
     */
    @Test
    void rakeRejectsControllerAndLoadedCell() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        Block cell = place(1, 64, 0, DeviceType.MVN_CELL_T2);
        NodeBlob blob = NodeStore.get(cell);
        blob.cellSample = new ItemStack(Material.DIAMOND);
        blob.cellAmount = 5;
        NodeStore.put(cell, blob);

        ItemStack rake = Items.rake();
        int uses = Items.rakeUses(rake);
        player.getInventory().setItemInMainHand(rake);

        server.getPluginManager().callEvent(new PlayerInteractEvent(player,
                Action.RIGHT_CLICK_BLOCK, rake, controller, BlockFace.NORTH, EquipmentSlot.HAND, null));
        assertNotEquals(Material.AIR, controller.getType());
        assertNotNull(NodeStore.get(controller));

        server.getPluginManager().callEvent(new PlayerInteractEvent(player,
                Action.RIGHT_CLICK_BLOCK, rake, cell, BlockFace.NORTH, EquipmentSlot.HAND, null));
        assertNotEquals(Material.AIR, cell.getType());
        assertNotNull(NodeStore.get(cell), "cell with items is preserved");
        assertEquals(uses, Items.rakeUses(rake), "no uses consumed on rejection");
    }

    /**
     * [EN] Configurator/wrench copies filter settings on shift-click and pastes on right-click.
     * [ES] El configurador/llave copia filtros con shift-click y los pega con clic derecho.
     */
    @Test
    void wrenchCopiesAndPastesFilters() {
        Block source = place(0, 64, 0, DeviceType.MVN_GRABBER);
        NodeBlob blobSource = NodeStore.get(source);
        blobSource.filterMaterials.add("diamond");
        blobSource.filterMaterials.add("gold_ingot");
        blobSource.filterBlacklist = true;
        NodeStore.put(source, blobSource);

        Block target = place(1, 64, 0, DeviceType.MVN_GRABBER);

        ItemStack wrench = Items.create(DeviceType.MVN_CONFIGURATOR);
        player.getInventory().setItemInMainHand(wrench);

        player.setSneaking(true);
        server.getPluginManager().callEvent(new PlayerInteractEvent(player,
                Action.RIGHT_CLICK_BLOCK, wrench, source, BlockFace.NORTH, EquipmentSlot.HAND, null));
        player.setSneaking(false);
        assertNotNull(Items.readConfig(wrench), "wrench stores copied configuration");

        server.getPluginManager().callEvent(new PlayerInteractEvent(player,
                Action.RIGHT_CLICK_BLOCK, wrench, target, BlockFace.NORTH, EquipmentSlot.HAND, null));

        NodeBlob blobTarget = NodeStore.get(target);
        assertEquals(java.util.List.of("diamond", "gold_ingot"), blobTarget.filterMaterials);
        assertTrue(blobTarget.filterBlacklist, "blacklist mode is copied as well");
    }

    /**
     * [EN] Crayon tool toggles visual beacon/particle indicators on the controller.
     * [ES] El crayón conmuta las partículas indicadoras en el controlador.
     */
    @Test
    void crayonTogglesControllerParticles() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        ItemStack crayon = Items.create(DeviceType.MVN_CRAYON);
        player.getInventory().setItemInMainHand(crayon);

        assertFalse(NodeStore.get(controller).crayon);
        server.getPluginManager().callEvent(new PlayerInteractEvent(player,
                Action.RIGHT_CLICK_BLOCK, crayon, controller, BlockFace.NORTH, EquipmentSlot.HAND, null));
        assertTrue(NodeStore.get(controller).crayon, "crayon enables particles");
        server.getPluginManager().callEvent(new PlayerInteractEvent(player,
                Action.RIGHT_CLICK_BLOCK, crayon, controller, BlockFace.NORTH, EquipmentSlot.HAND, null));
        assertFalse(NodeStore.get(controller).crayon, "second click disables particles");
    }

    /**
     * [EN] Advanced grabber with targetFace set only extracts from the specified container face.
     * [ES] El grabber avanzado con targetFace configurado solo extrae de la cara indicada.
     */
    @Test
    void grabberWithTargetFaceOnlyExtractsFromDesignatedFace() {
        Block ctrl = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        plugin.networks().registerController(ctrl);
        Block cell = place(1, 64, 0, DeviceType.MVN_CELL_T1);
        plugin.networks().invalidateNear(cell);
        Block grabber = place(0, 64, 1, DeviceType.MVN_GRABBER_HT);
        plugin.networks().invalidateNear(grabber);

        Block westChest = world.getBlockAt(-1, 64, 1);
        westChest.setType(Material.CHEST);
        org.bukkit.block.Chest wChestState = (org.bukkit.block.Chest) westChest.getState();
        wChestState.getInventory().addItem(new ItemStack(Material.DIAMOND, 10));

        Block southChest = world.getBlockAt(0, 64, 2);
        southChest.setType(Material.CHEST);
        org.bukkit.block.Chest sChestState = (org.bukkit.block.Chest) southChest.getState();
        sChestState.getInventory().addItem(new ItemStack(Material.EMERALD, 10));

        NodeBlob blob = NodeStore.get(grabber);
        blob.targetFace = "WEST";
        NodeStore.put(grabber, blob);

        server.getScheduler().performOneTick();
        server.getScheduler().performTicks(20);

        Network net = plugin.networks().networkByController(ctrl.getLocation());
        assertNotNull(net);
        assertEquals(10, net.storage().count(i -> i.getType() == Material.DIAMOND),
                "must extract diamonds from selected west chest");
        assertEquals(0, net.storage().count(i -> i.getType() == Material.EMERALD),
                "must not touch unselected south chest");
    }

    /**
     * [EN] Vacuum device picks up items from the ground respecting whitelist and blacklist filters.
     * [ES] El dispositivo Vacuum recoge items del suelo respetando filtros whitelist y blacklist.
     */
    @Test
    void vacuumPicksUpGroundItemsAndRespectsFilters() {
        Block ctrl = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        plugin.networks().registerController(ctrl);
        place(1, 64, 0, DeviceType.MVN_CELL_T1);
        Block vacuum = place(0, 64, 1, DeviceType.MVN_VACUUM);
        Network net = plugin.networks().networkByController(ctrl.getLocation());
        net.scan();

        NodeBlob blob = NodeStore.get(vacuum);
        blob.filterItems = new ArrayList<>(List.of(new ItemStack(Material.IRON_INGOT)));
        blob.filterMaterials = new ArrayList<>(List.of("IRON_INGOT"));
        blob.filterBlacklist = false;
        NodeStore.put(vacuum, blob);

        Location dropLoc = vacuum.getLocation().clone().add(0.5, 1.0, 0.5);
        Item ironDrop = world.dropItem(dropLoc, new ItemStack(Material.IRON_INGOT, 16));
        ironDrop.setPickupDelay(0);
        Item goldDrop = world.dropItem(dropLoc, new ItemStack(Material.GOLD_INGOT, 8));
        goldDrop.setPickupDelay(0);

        server.getScheduler().performTicks(50);

        assertEquals(16, net.storage().count(i -> i.getType() == Material.IRON_INGOT),
                "vacuum collects matching filtered item");
        assertTrue(ironDrop.isDead(), "collected iron entity is consumed");

        assertEquals(0, net.storage().count(i -> i.getType() == Material.GOLD_INGOT),
                "vacuum does not collect unallowed items");
        assertFalse(goldDrop.isDead(), "unfiltered gold remains on the ground");
    }

    /**
     * [EN] Purger discards matching items from the network storage only when configured with a filter.
     * [ES] El purgador descarta items coincidentes de la red únicamente cuando tiene un filtro configurado.
     */
    @Test
    void purgerDiscardsNetworkItemsOnlyWhenFiltered() {
        Block ctrl = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        plugin.networks().registerController(ctrl);
        place(1, 64, 0, DeviceType.MVN_CELL_T1);
        place(2, 64, 0, DeviceType.MVN_CELL_T1);
        Block purger = place(0, 64, 1, DeviceType.MVN_PURGER);
        Network net = plugin.networks().networkByController(ctrl.getLocation());
        net.scan();

        net.storage().deposit(new ItemStack(Material.COBBLESTONE, 64));
        net.storage().deposit(new ItemStack(Material.DIAMOND, 10));

        // 1) Without filter: nothing purged
        server.getScheduler().performTicks(50);
        assertEquals(64, net.storage().count(i -> i.getType() == Material.COBBLESTONE),
                "without filter nothing is purged");
        assertEquals(10, net.storage().count(i -> i.getType() == Material.DIAMOND),
                "without filter nothing is purged");

        // 2) With whitelist filter for COBBLESTONE: cobblestone purged, diamond preserved
        NodeBlob blob = NodeStore.get(purger);
        blob.filterMaterials = new ArrayList<>(List.of("COBBLESTONE"));
        blob.filterBlacklist = false;
        NodeStore.put(purger, blob);

        server.getScheduler().performTicks(50);

        assertEquals(0, net.storage().count(i -> i.getType() == Material.COBBLESTONE),
                "cobblestone must be purged");
        assertEquals(10, net.storage().count(i -> i.getType() == Material.DIAMOND),
                "diamonds must remain untouched in network");
    }

    /**
     * [EN] Fast type and presence lookups in NodeStore operate accurately and self-heal missing type tags.
     * [ES] Las consultas rápidas de tipo y presencia en NodeStore funcionan con precisión y autoreparan tags ausentes.
     */
    @Test
    void nodeStoreFastTypeAndPresenceOperations() {
        Block cable = place(10, 64, 10, DeviceType.MVN_CABLE);
        Block empty = world.getBlockAt(10, 64, 11);

        assertTrue(NodeStore.hasNode(cable));
        assertFalse(NodeStore.hasNode(empty));
        assertEquals(DeviceType.MVN_CABLE, NodeStore.getType(cable));

        // Test self-healing fallback when nodeTypeKey is removed
        org.bukkit.NamespacedKey typeKey = new org.bukkit.NamespacedKey(plugin, "t10_64_10");
        cable.getChunk().getPersistentDataContainer().remove(typeKey);
        assertFalse(cable.getChunk().getPersistentDataContainer().has(typeKey, PersistentDataType.STRING));

        // getType should fall back to blob decode and restore typeKey
        assertEquals(DeviceType.MVN_CABLE, NodeStore.getType(cable));
        assertTrue(cable.getChunk().getPersistentDataContainer().has(typeKey, PersistentDataType.STRING));
    }

    /**
     * [EN] NetworkStorage view aggregation correctly groups and sums items by material in O(N).
     * [ES] La agregación de vistas de NetworkStorage agrupa y suma correctamente ítems por material en O(N).
     */
    @Test
    void networkStorageViewAggregatesBucketsCorrectly() {
        Block ctrl = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        plugin.networks().registerController(ctrl);
        place(1, 64, 0, DeviceType.MVN_CELL_T1);
        place(2, 64, 0, DeviceType.MVN_CELL_T1);
        Network net = plugin.networks().networkByController(ctrl.getLocation());
        net.scan();

        net.storage().deposit(new ItemStack(Material.IRON_INGOT, 50));
        net.storage().deposit(new ItemStack(Material.IRON_INGOT, 30));
        net.storage().deposit(new ItemStack(Material.GOLD_INGOT, 15));

        var views = net.storage().view();
        assertEquals(2, views.size(), "should have 2 distinct item entries (IRON and GOLD)");

        long ironCount = views.stream()
                .filter(v -> v.sample().getType() == Material.IRON_INGOT)
                .mapToLong(com.chagui68.multiversenets.net.NetworkStorage.View::amount)
                .sum();
        long goldCount = views.stream()
                .filter(v -> v.sample().getType() == Material.GOLD_INGOT)
                .mapToLong(com.chagui68.multiversenets.net.NetworkStorage.View::amount)
                .sum();

        assertEquals(80, ironCount);
        assertEquals(15, goldCount);
    }
}

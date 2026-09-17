package com.chagui68.multiversenets;

import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.Keys;
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
 * Flujos de romper y colocar nodos: el contenido viaja dentro del item (como el quantum de
 * NetworksV6) y se restaura al recolocar, y las protecciones contra pistones y explosiones no
 * dejan nodos fantasmas.
 */
class FlujosBloquesTest {

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

    private Block colocar(int x, int y, int z, DeviceType tipo) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(tipo.material());
        NodeStore.put(block, NodeBlob.create(tipo.name()));
        return block;
    }

    private ItemStack romperBloque(Block block) {
        BlockBreakEvent event = new BlockBreakEvent(block, player);
        server.getPluginManager().callEvent(event);
        // El servidor consumiria el evento rompiendo el bloque; el drop es lo relevante.
        Item drop = null;
        for (var entity : world.getEntities()) {
            if (entity instanceof Item item && item.getLocation().distance(block.getLocation().add(0.5, 0.5, 0.5)) < 2) {
                drop = item;
            }
        }
        return drop == null ? null : drop.getItemStack();
    }

    // ---------------------------------------------------------------- celdas y carga

    @Test
    void romperCeldaConCargaLaEmbarcaEnElItem() {
        Block celda = colocar(0, 64, 0, DeviceType.CELL_T2);
        NodeBlob blob = NodeStore.get(celda);
        blob.cellSample = new ItemStack(Material.REDSTONE);
        blob.cellAmount = 12345;
        NodeStore.put(celda, blob);

        ItemStack drop = romperBloque(celda);
        assertNotNull(drop, "romper la celda debe soltar el item de la celda");
        assertEquals(DeviceType.CELL_T2, Items.typeOf(drop));
        String cargo = drop.getItemMeta().getPersistentDataContainer()
                .get(Keys.CELL_CARGO, PersistentDataType.STRING);
        assertNotNull(cargo, "el item suelto lleva la carga embebida");
        NodeBlob embebido = NodeStore.decode(cargo);
        assertNotNull(embebido);
        assertEquals(Material.REDSTONE, embebido.cellSample.getType());
        assertEquals(12345, embebido.cellAmount);

        // Y el inventario del jugador NO recibe el contenido por separado: nada por el suelo salvo
        // el propio item.
        assertNull(NodeStore.get(celda), "el nodo queda eliminado del chunk");
    }

    @Test
    void recolocarCeldaConCargaLaRestaura() {
        // Celda "rota" con cargo embebido en el item (DEVICE_TYPE ya lo pone Items.create).
        ItemStack item = Items.create(DeviceType.CELL_T1);
        NodeBlob guardado = NodeBlob.create(DeviceType.CELL_T1.name());
        guardado.cellSample = new ItemStack(Material.GOLD_INGOT);
        guardado.cellAmount = 777;
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(Keys.CELL_CARGO, PersistentDataType.STRING, NodeStore.encode(guardado));
        item.setItemMeta(meta);

        Block destino = world.getBlockAt(10, 64, 0);
        org.bukkit.block.BlockState previo = destino.getState();
        destino.setType(DeviceType.CELL_T1.material());
        BlockPlaceEvent place = new BlockPlaceEvent(destino, previo, destino.getRelative(BlockFace.DOWN),
                item, player, true, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(place);

        NodeBlob restaurado = NodeStore.get(destino);
        assertNotNull(restaurado);
        assertEquals(Material.GOLD_INGOT, restaurado.cellSample.getType());
        assertEquals(777, restaurado.cellAmount, "la carga embebida vuelve al bloque al colocarlo");
    }

    @Test
    void romperReceptorConservaElEnlace() {
        Block tx = colocar(0, 64, 0, DeviceType.TRANSMITTER);
        colocar(1, 64, 0, DeviceType.CABLE);
        Block rx = colocar(2, 64, 0, DeviceType.RECEIVER);
        NodeBlob blob = NodeStore.get(rx);
        blob.txWorld = world.getUID().toString();
        blob.txX = 0;
        blob.txY = 64;
        blob.txZ = 0;
        NodeStore.put(rx, blob);

        ItemStack drop = romperBloque(rx);
        assertNotNull(drop);
        String cargo = drop.getItemMeta().getPersistentDataContainer()
                .get(Keys.CELL_CARGO, PersistentDataType.STRING);
        assertNotNull(cargo, "el receptor roto conserva su enlace en el item");
        assertEquals(world.getUID().toString(), NodeStore.decode(cargo).txWorld);
    }

    // ---------------------------------------------------------------- protecciones

    @Test
    void lasExplosionesNoRompenNodos() {
        Block cable = colocar(5, 64, 5, DeviceType.CABLE);
        Block otro = world.getBlockAt(5, 65, 5);
        otro.setType(Material.STONE);
        List<Block> afectados = new ArrayList<>(List.of(cable, otro));

        EntityExplodeEvent boom = new EntityExplodeEvent(null, otro.getLocation(), afectados, 1.0f,
                org.bukkit.ExplosionResult.DESTROY);
        server.getPluginManager().callEvent(boom);

        assertFalse(afectados.contains(cable), "los nodos se retiran del radio de explosion");
        assertTrue(afectados.contains(otro));
    }

    @Test
    void losPistonesNoMuevenNodos() {
        Block pusherNode = colocar(6, 64, 6, DeviceType.GRABBER);
        Block piston = world.getBlockAt(6, 64, 7);
        piston.setType(Material.PISTON);
        BlockPistonExtendEvent extend = new BlockPistonExtendEvent(piston, List.of(pusherNode), BlockFace.NORTH);
        server.getPluginManager().callEvent(extend);
        assertTrue(extend.isCancelled(), "un piston que tocara un nodo se cancela");
    }

    // ---------------------------------------------------------------- herramientas

    @Test
    void enlazarYAbrirTerminalInalambrica() {
        // Red con controlador + celda.
        Block controller = colocar(0, 64, 0, DeviceType.CONTROLLER);
        plugin.networks().registerController(controller);
        colocar(1, 64, 0, DeviceType.CELL_T1);

        ItemStack terminal = Items.create(DeviceType.WIRELESS_TERMINAL);
        // Shift+click sobre el controlador: vincula.
        player.setSneaking(true);
        player.getInventory().setItemInMainHand(terminal);
        var bindEvent = new PlayerInteractEvent(player,
                Action.RIGHT_CLICK_BLOCK, terminal, controller, BlockFace.NORTH, EquipmentSlot.HAND, null);
        server.getPluginManager().callEvent(bindEvent);
        player.setSneaking(false);

        Location bind = Items.readWirelessBind(terminal);
        assertNotNull(bind, "shift+clic sobre el controlador vincula la terminal");
        assertEquals(controller.getLocation(), bind);
    }

    @Test
    void terminalInalambricaVinculadaAbreLaTerminalAlAire() {
        Block controller = colocar(0, 64, 0, DeviceType.CONTROLLER);
        plugin.networks().registerController(controller);
        colocar(1, 64, 0, DeviceType.CELL_T1);

        ItemStack terminal = Items.create(DeviceType.WIRELESS_TERMINAL);
        Items.bindWireless(terminal, controller.getLocation());
        player.getInventory().setItemInMainHand(terminal);

        var air = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, terminal, null,
                BlockFace.SELF, EquipmentSlot.HAND, null);
        server.getPluginManager().callEvent(air);

        assertTrue(player.getOpenInventory().getTopInventory().getHolder()
                        instanceof com.chagui68.multiversenets.gui.TerminalMenu,
                "click al aire con la terminal vinculada abre la TerminalMenu");
    }

    @Test
    void rakeDesmontaNodoYGastaUso() {
        Block grabber = colocar(0, 64, 0, DeviceType.GRABBER);
        ItemStack rake = Items.rake();
        int usosAntes = Items.rakeUses(rake);
        player.getInventory().setItemInMainHand(rake);

        var event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, rake, grabber,
                BlockFace.NORTH, EquipmentSlot.HAND, null);
        server.getPluginManager().callEvent(event);

        assertTrue(event.isCancelled());
        assertEquals(Material.AIR, grabber.getType(), "el rake retira el bloque");
        assertNull(NodeStore.get(grabber), "el nodo desaparece del chunk");
        assertEquals(usosAntes - 1, Items.rakeUses(rake), "el rake gasta un uso");
    }

    @Test
    void rakeRechazaControladorYCeldaConCarga() {
        Block controller = colocar(0, 64, 0, DeviceType.CONTROLLER);
        Block celda = colocar(1, 64, 0, DeviceType.CELL_T2);
        NodeBlob blob = NodeStore.get(celda);
        blob.cellSample = new ItemStack(Material.DIAMOND);
        blob.cellAmount = 5;
        NodeStore.put(celda, blob);

        ItemStack rake = Items.rake();
        int usos = Items.rakeUses(rake);
        player.getInventory().setItemInMainHand(rake);

        // Controlador: no se puede rastrillar.
        server.getPluginManager().callEvent(new PlayerInteractEvent(player,
                Action.RIGHT_CLICK_BLOCK, rake, controller, BlockFace.NORTH, EquipmentSlot.HAND, null));
        assertNotEquals(Material.AIR, controller.getType());
        assertNotNull(NodeStore.get(controller));

        // Celda con carga: tampoco.
        server.getPluginManager().callEvent(new PlayerInteractEvent(player,
                Action.RIGHT_CLICK_BLOCK, rake, celda, BlockFace.NORTH, EquipmentSlot.HAND, null));
        assertNotEquals(Material.AIR, celda.getType());
        assertNotNull(NodeStore.get(celda), "la celda cargada se conserva");
        assertEquals(usos, Items.rakeUses(rake), "no se gastan usos en fallos");
    }

    @Test
    void wrenchCopiaYPegaFiltros() {
        Block origen = colocar(0, 64, 0, DeviceType.GRABBER);
        NodeBlob blobOrigen = NodeStore.get(origen);
        blobOrigen.filterMaterials.add("diamond");
        blobOrigen.filterMaterials.add("gold_ingot");
        blobOrigen.filterBlacklist = true;
        NodeStore.put(origen, blobOrigen);

        Block destino = colocar(1, 64, 0, DeviceType.GRABBER);

        ItemStack wrench = Items.create(DeviceType.CONFIGURATOR);
        player.getInventory().setItemInMainHand(wrench);

        // Shift+click en el origen: copia.
        player.setSneaking(true);
        server.getPluginManager().callEvent(new PlayerInteractEvent(player,
                Action.RIGHT_CLICK_BLOCK, wrench, origen, BlockFace.NORTH, EquipmentSlot.HAND, null));
        player.setSneaking(false);
        assertNotNull(Items.readConfig(wrench), "el wrench guarda la configuracion");

        // Click normal en el destino: pega.
        server.getPluginManager().callEvent(new PlayerInteractEvent(player,
                Action.RIGHT_CLICK_BLOCK, wrench, destino, BlockFace.NORTH, EquipmentSlot.HAND, null));

        NodeBlob blobDestino = NodeStore.get(destino);
        assertEquals(java.util.List.of("diamond", "gold_ingot"), blobDestino.filterMaterials);
        assertTrue(blobDestino.filterBlacklist, "el modo blacklist se copia tambien");
    }

    @Test
    void crayonConmutaLasParticulasDelControlador() {
        Block controller = colocar(0, 64, 0, DeviceType.CONTROLLER);
        ItemStack crayon = Items.create(DeviceType.CRAYON);
        player.getInventory().setItemInMainHand(crayon);

        assertFalse(NodeStore.get(controller).crayon);
        server.getPluginManager().callEvent(new PlayerInteractEvent(player,
                Action.RIGHT_CLICK_BLOCK, crayon, controller, BlockFace.NORTH, EquipmentSlot.HAND, null));
        assertTrue(NodeStore.get(controller).crayon, "el crayon activa las particulas");
        server.getPluginManager().callEvent(new PlayerInteractEvent(player,
                Action.RIGHT_CLICK_BLOCK, crayon, controller, BlockFace.NORTH, EquipmentSlot.HAND, null));
        assertFalse(NodeStore.get(controller).crayon, "segundo clic las apaga");
    }

    @Test
    void grabberConTargetFaceSoloExtraeDeLaCaraIndicada() {
        Block ctrl = colocar(0, 64, 0, DeviceType.CONTROLLER);
        plugin.networks().registerController(ctrl);
        Block cell = colocar(1, 64, 0, DeviceType.CELL_T1);
        plugin.networks().invalidateNear(cell);
        Block grabber = colocar(0, 64, 1, DeviceType.GRABBER);
        plugin.networks().invalidateNear(grabber);

        // Cofre al Oeste del grabber (-1, 64, 1) con diamantes
        Block westChest = world.getBlockAt(-1, 64, 1);
        westChest.setType(Material.CHEST);
        org.bukkit.block.Chest wChestState = (org.bukkit.block.Chest) westChest.getState();
        wChestState.getInventory().addItem(new ItemStack(Material.DIAMOND, 10));

        // Cofre al Sur del grabber (0, 64, 2) con esmeraldas
        Block southChest = world.getBlockAt(0, 64, 2);
        southChest.setType(Material.CHEST);
        org.bukkit.block.Chest sChestState = (org.bukkit.block.Chest) southChest.getState();
        sChestState.getInventory().addItem(new ItemStack(Material.EMERALD, 10));

        // Fijar targetFace a "WEST"
        NodeBlob blob = NodeStore.get(grabber);
        blob.targetFace = "WEST";
        NodeStore.put(grabber, blob);

        // Tick del servidor para procesar transferencias
        server.getScheduler().performOneTick();
        server.getScheduler().performTicks(20);

        Network net = plugin.networks().networkByController(ctrl.getLocation());
        assertNotNull(net);
        // Debe haber extraído diamante (Oeste) pero NO esmeralda (Sur)
        assertEquals(10, net.storage().count(i -> i.getType() == Material.DIAMOND),
                "debe extraer del cofre oeste seleccionado");
        assertEquals(0, net.storage().count(i -> i.getType() == Material.EMERALD),
                "NO debe tocar el cofre sur");
    }
}

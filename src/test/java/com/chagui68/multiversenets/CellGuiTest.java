package com.chagui68.multiversenets;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import com.chagui68.multiversenets.gui.CellMenu;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reproduce el fallo reportado: el boton "Set Item" de la celda no hace nada.
 *
 * La GUI se abre de verdad, se hace click de verdad (el evento pasa por el GuiListener, como en
 * el servidor) y se verifica que el blob del bloque acuerda el tipo.
 */
class CellGuiTest {

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

    private Block colocarCelda(int x, int y, int z, DeviceType tipo) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(tipo.material());
        NodeStore.put(block, NodeBlob.create(tipo.name()));
        return block;
    }

    @Test
    void laCeldaSeAbreConClicDerecho() {
        Block celda = colocarCelda(0, 64, 0, DeviceType.CELL_T1);
        PlayerInteractEvent interact = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK,
                null, celda, BlockFace.NORTH, EquipmentSlot.HAND, null);
        server.getPluginManager().callEvent(interact);
        assertTrue(player.getOpenInventory().getTopInventory().getHolder() instanceof CellMenu,
                "el clic derecho sobre la celda tiene que abrir su menu");
    }

    @Test
    void elBotonSetItemFijaElTipo() {
        Block celda = colocarCelda(0, 64, 0, DeviceType.CELL_T1);

        CellMenu menu = new CellMenu(plugin, player, celda, DeviceType.CELL_T1);
        menu.openMenu();
        // MockBukkit limpia el cursor al abrir el inventario: se pone despues, como en juego.
        player.getOpenInventory().setCursor(new ItemStack(Material.DIAMOND, 5));

        InventoryClickEvent click = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 13, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(click);

        NodeBlob blob = NodeStore.get(celda);
        assertNotNull(blob.cellSample, "tras clicar Set Item con un item en el cursor, la celda debe registrar el tipo");
        assertEquals(Material.DIAMOND, blob.cellSample.getType());
        // El item del cursor no se consume: solo se registra como plantilla.
        assertEquals(5, player.getItemOnCursor().getAmount());
    }

    @Test
    void cerrarConItemsEnEntradaLosGuardaEnLaCelda() {
        Block celda = colocarCelda(0, 64, 0, DeviceType.CELL_T1);
        CellMenu menu = new CellMenu(plugin, player, celda, DeviceType.CELL_T1);
        menu.openMenu();
        player.getOpenInventory().getTopInventory().setItem(1, new ItemStack(Material.COBBLESTONE, 64));
        player.closeInventory();

        NodeBlob blob = NodeStore.get(celda);
        assertNotNull(blob.cellSample);
        assertEquals(Material.COBBLESTONE, blob.cellSample.getType());
        assertEquals(64, blob.cellAmount, "cerrar la celda con items en la entrada los guarda");
    }

    @Test
    void laCeldaTieneDosFilas() {
        assertEquals(18, cellMenuSize());
    }

    private int cellMenuSize() {
        Block celda = colocarCelda(0, 64, 0, DeviceType.CELL_T1);
        CellMenu menu = new CellMenu(plugin, player, celda, DeviceType.CELL_T1);
        menu.openMenu();
        return player.getOpenInventory().getTopInventory().getSize();
    }

    @Test
    void setItemNoSobrescribeUnaCeldaLlena() {
        Block celda = colocarCelda(0, 64, 0, DeviceType.CELL_T1);
        NodeBlob blob = NodeStore.get(celda);
        blob.cellSample = new ItemStack(Material.COBBLESTONE);
        blob.cellAmount = 100;
        NodeStore.put(celda, blob);

        player.setItemOnCursor(new ItemStack(Material.DIAMOND, 1));
        CellMenu menu = new CellMenu(plugin, player, celda, DeviceType.CELL_T1);
        menu.openMenu();
        InventoryClickEvent click = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 13, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(click);

        NodeBlob after = NodeStore.get(celda);
        assertEquals(Material.COBBLESTONE, after.cellSample.getType(),
                "una celda con contenido no puede cambiar de tipo");
    }
}

package com.chagui68.multiversenets;

import com.chagui68.multiversenets.craft.Blueprints;
import com.chagui68.multiversenets.craft.RecipeData;
import com.chagui68.multiversenets.gui.CrafterMenu;
import com.chagui68.multiversenets.gui.CraftingGridMenu;
import com.chagui68.multiversenets.gui.EncoderMenu;
import com.chagui68.multiversenets.gui.FilterMenu;
import com.chagui68.multiversenets.gui.MonitorMenu;
import com.chagui68.multiversenets.gui.TerminalMenu;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
import org.bukkit.inventory.ShapedRecipe;
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
 * Ronda completa de GUIs: se abren con clicks reales y se verifica el efecto en la blob/red.
 * Asi cada menu queda sujeto a la prueba, no a la intuicion.
 */
class GuiFlowsTest {

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

    // ------------------------------------------------------------ helpers

    private Block colocar(int x, int y, int z, DeviceType tipo) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(tipo.material());
        NodeStore.put(block, NodeBlob.create(tipo.name()));
        plugin.networks().invalidateNear(block);
        return block;
    }

    private void clicDerecho(Block block) {
        PlayerInteractEvent interact = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK,
                null, block, BlockFace.NORTH, EquipmentSlot.HAND, null);
        server.getPluginManager().callEvent(interact);
    }

    private void clickTop(int raw, ClickType type) {
        InventoryClickEvent click = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, raw, type, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(click);
    }

    private void clickBottomShift(int slotInv) {
        InventoryClickEvent click = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 54 + slotInv, ClickType.SHIFT_LEFT,
                InventoryAction.MOVE_TO_OTHER_INVENTORY);
        server.getPluginManager().callEvent(click);
    }

    private Network redConCelda() {
        Block controller = colocar(0, 64, 0, DeviceType.CONTROLLER);
        plugin.networks().registerController(controller);
        colocar(1, 64, 0, DeviceType.CELL_T2);
        return plugin.networks().networkByController(controller.getLocation());
    }

    // ------------------------------------------------------------ terminal

    @Test
    void controladorAbreTerminal() {
        redConCelda();
        clicDerecho(world.getBlockAt(0, 64, 0));
        assertTrue(player.getOpenInventory().getTopInventory().getHolder() instanceof TerminalMenu,
                "clic derecho al controlador abre la terminal");
    }

    @Test
    void terminalRetiraUnoConClickIzquierdoYStackConShift() {
        Network net = redConCelda();
        assertEquals(0, net.storage().deposit(new ItemStack(Material.COBBLESTONE, 128)));
        plugin.networks().invalidateNear(world.getBlockAt(1, 64, 0));

        clicDerecho(world.getBlockAt(0, 64, 0));

        clickTop(0, ClickType.LEFT);
        assertEquals(Material.COBBLESTONE, player.getItemOnCursor().getType(),
                "click izquierdo deja 1 en el cursor");
        assertEquals(1, player.getItemOnCursor().getAmount());

        player.setItemOnCursor(null);
        clickTop(0, ClickType.SHIFT_LEFT);
        assertEquals(Material.COBBLESTONE, player.getInventory().getItem(0) == null
                        ? null : player.getInventory().getItem(0).getType(),
                "shift manda hasta un stack al inventario");
        assertEquals(64, player.getInventory().getItem(0).getAmount());
        assertEquals(63, net.storage().count(i -> i.getType() == Material.COBBLESTONE));
    }

    @Test
    void terminalGuardaLaEntradaEnLaRedAlCerrar() {
        Network net = redConCelda();
        clicDerecho(world.getBlockAt(0, 64, 0));
        player.getOpenInventory().getTopInventory().setItem(8, new ItemStack(Material.IRON_INGOT, 32));
        player.closeInventory();
        assertEquals(32, net.storage().count(i -> i.getType() == Material.IRON_INGOT),
                "lo dejado en el hueco de entrada se guarda en la red al cerrar");
    }

    @Test
    void shiftSobreItemPropioLoDepositaEnLaRed() {
        Network net = redConCelda();
        clicDerecho(world.getBlockAt(0, 64, 0));
        player.getInventory().setItem(3, new ItemStack(Material.GOLD_INGOT, 10));
        clickBottomShift(3);
        assertEquals(10, net.storage().count(i -> i.getType() == Material.GOLD_INGOT));
        assertNull(player.getInventory().getItem(3));
    }

    // ------------------------------------------------------------ encoder + crafter

    private ShapedRecipe registrarReceta() {
        NamespacedKey key = new NamespacedKey(plugin, "test_blockcraft");
        ShapedRecipe recipe = new ShapedRecipe(key, new ItemStack(Material.CLOCK, 2));
        recipe.shape("DDD", "D D", "DDD");
        recipe.setIngredient('D', Material.DIAMOND);
        server.addRecipe(recipe);
        return recipe;
    }

    @Test
    void encoderCodificaMatrizEnBlueprint() {
        registrarReceta();
        Block encoder = colocar(5, 64, 0, DeviceType.ENCODER);
        clicDerecho(encoder);
        assertTrue(player.getOpenInventory().getTopInventory().getHolder() instanceof EncoderMenu);

        // Matriz 3x3: clic en los 8 huecos de la U con diamante en el cursor.
        int[] matriz = {12, 13, 14, 21, 23, 30, 31, 32};
        for (int slot : matriz) {
            player.getOpenInventory().setCursor(new ItemStack(Material.DIAMOND, 1));
            clickTop(slot, ClickType.LEFT);
        }
        player.getOpenInventory().setCursor(null);
        // Blueprint en blanco en el hueco vanilla 19.
        player.getOpenInventory().getTopInventory().setItem(19, Items.create(DeviceType.BLUEPRINT));

        clickTop(16, ClickType.LEFT); // encode

        ItemStack salida = player.getOpenInventory().getTopInventory().getItem(34);
        assertNotNull(salida, "codificar deja el blueprint en la salida");
        RecipeData data = Blueprints.read(salida);
        assertNotNull(data, "el blueprint lleva la receta embebida");
        assertEquals(Material.CLOCK, data.output.getType());
        assertEquals(Material.DIAMOND, data.inputs[0].getType());
        assertNull(data.inputs[4], "el centro de la matriz quedo vacio y asi debe persistir");

        // El modelo quedo persistido en el bloque.
        NodeBlob blob = NodeStore.get(encoder);
        assertNotNull(blob.craftingMatrix[0], "la matriz del encoder se guarda en el bloque");
    }

    @Test
    void crafterConBlueprintFabricaDeLaRedAtomicamente() {
        registrarReceta();
        Network net = redConCelda();
        colocar(1, 63, 0, DeviceType.CELL_T2); // celda vacia: recibira el resultado
        colocar(2, 64, 0, DeviceType.CRAFTER);
        Block crafter = world.getBlockAt(2, 64, 0);

        // Instalar blueprint por dato (el crafter lo codificaria el encoder).
        ItemStack[] matriz = new ItemStack[]{new ItemStack(Material.DIAMOND), new ItemStack(Material.DIAMOND),
                new ItemStack(Material.DIAMOND), new ItemStack(Material.DIAMOND), null,
                new ItemStack(Material.DIAMOND), new ItemStack(Material.DIAMOND), new ItemStack(Material.DIAMOND),
                new ItemStack(Material.DIAMOND)};
        RecipeData data = new RecipeData(matriz, new ItemStack(Material.CLOCK, 2));
        NodeBlob blob = NodeStore.get(crafter);
        blob.blueprintData.add(Blueprints.encode(data));
        NodeStore.put(crafter, blob);

        // Exactamente 7 diamantes: no alcanza (la receta pide 8) -> nada se toca.
        net.storage().deposit(new ItemStack(Material.DIAMOND, 7));
        assertEquals(false, com.chagui68.multiversenets.craft.CraftingSupport.tryCraftBlueprint(net, data));
        assertEquals(7, net.storage().count(i -> i.getType() == Material.DIAMOND),
                "sin ingredientes para todo, la red queda intacta (atomico)");

        net.storage().deposit(new ItemStack(Material.DIAMOND, 8));
        assertTrue(com.chagui68.multiversenets.craft.CraftingSupport.tryCraftBlueprint(net, data));
        assertEquals(7, net.storage().count(i -> i.getType() == Material.DIAMOND));
        assertEquals(2, net.storage().count(i -> i.getType() == Material.CLOCK),
                "el resultado se deposita en la red");
    }

    // ------------------------------------------------------------ crafting grid

    @Test
    void craftingGridUsaLaRedYDevuelveElResultado() {
        registrarReceta();
        Network net = redConCelda();
        // Adyacente al controlador (0,64,0): si no toca un nodo, abrirlo diria que no hay red.
        Block grid = colocar(0, 63, 0, DeviceType.CRAFTING_GRID);
        clicDerecho(grid);
        assertTrue(player.getOpenInventory().getTopInventory().getHolder() instanceof CraftingGridMenu);

        int[] top = {0, 1, 2, 9, 11, 18, 19, 20};
        for (int slot : top) {
            player.getOpenInventory().setCursor(new ItemStack(Material.DIAMOND, 1));
            clickTop(slot, ClickType.LEFT);
        }
        player.getOpenInventory().setCursor(null);

        net.storage().deposit(new ItemStack(Material.DIAMOND, 9));
        clickTop(33, ClickType.LEFT); // Craft x1

        boolean recibio = false;
        for (ItemStack it : player.getInventory().getContents()) {
            if (it != null && it.getType() == Material.CLOCK) {
                recibio = true;
                break;
            }
        }
        assertTrue(recibio, "el jugador recibe el resultado");
        assertEquals(1, net.storage().count(i -> i.getType() == Material.DIAMOND),
                "consumio los 8 ingredientes de la red");
    }

    // ------------------------------------------------------------ monitor y filtrables

    @Test
    void monitorAbreConRed() {
        Network net = redConCelda();
        // Adyacente a la celda (1,64,0): aislado no tendria red y el menu negaria la apertura.
        colocar(2, 64, 0, DeviceType.MONITOR);
        clicDerecho(world.getBlockAt(2, 64, 0));
        assertTrue(player.getOpenInventory().getTopInventory().getHolder() instanceof MonitorMenu);
    }

    @Test
    void grabberYVacuumAbrenFiltro() {
        Block grabber = colocar(6, 64, 0, DeviceType.GRABBER);
        clicDerecho(grabber);
        assertTrue(player.getOpenInventory().getTopInventory().getHolder() instanceof FilterMenu);
        player.closeInventory();

        Block vacuum = colocar(7, 64, 0, DeviceType.VACUUM);
        clicDerecho(vacuum);
        assertTrue(player.getOpenInventory().getTopInventory().getHolder() instanceof FilterMenu);
        player.closeInventory();

        Block crafter = colocar(8, 64, 0, DeviceType.CRAFTER);
        clicDerecho(crafter);
        assertTrue(player.getOpenInventory().getTopInventory().getHolder() instanceof CrafterMenu);
    }
}

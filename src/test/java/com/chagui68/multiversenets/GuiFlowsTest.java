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
import net.kyori.adventure.text.Component;
import org.bukkit.persistence.PersistentDataType;
import java.util.List;
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
 * [EN] Integration tests for all network GUIs (Terminal, Encoder, Crafter, Crafting Grid, Monitor).
 * [ES] Pruebas de integración de todas las GUIs de la red (Terminal, Codificador, Crafter, Mesa de crafteo, Monitor).
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

    private Block place(int x, int y, int z, DeviceType type) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(type.material());
        NodeStore.put(block, NodeBlob.create(type.name()));
        plugin.networks().invalidateNear(block);
        return block;
    }

    private void rightClick(Block block) {
        PlayerInteractEvent interact = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK,
                null, block, BlockFace.NORTH, EquipmentSlot.HAND, null);
        server.getPluginManager().callEvent(interact);
    }

    private void clickTop(int raw, ClickType type) {
        InventoryClickEvent click = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, raw, type, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(click);
    }

    private void clickBottomShift(int invSlot) {
        InventoryClickEvent click = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 54 + invSlot, ClickType.SHIFT_LEFT,
                InventoryAction.MOVE_TO_OTHER_INVENTORY);
        server.getPluginManager().callEvent(click);
    }

    private Network networkWithCell() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        plugin.networks().registerController(controller);
        place(1, 64, 0, DeviceType.MVN_CELL_T2);
        place(0, 64, 1, DeviceType.MVN_TERMINAL);
        Network net = plugin.networks().networkByController(controller.getLocation());
        net.scan();
        return net;
    }

    // ------------------------------------------------------------ terminal

    /**
     * [EN] Right clicking a terminal block opens the TerminalMenu GUI.
     * [ES] Clic derecho en un bloque terminal abre la GUI TerminalMenu.
     */
    @Test
    void terminalBlockOpensTerminal() {
        networkWithCell();
        rightClick(world.getBlockAt(0, 64, 1));
        assertTrue(player.getOpenInventory().getTopInventory().getHolder() instanceof TerminalMenu,
                "right click on terminal block opens terminal GUI");
    }

    /**
     * [EN] Terminal withdraws 1 item on left-click and up to a stack on shift-left click.
     * [ES] La terminal extrae 1 item con clic izquierdo y hasta un stack con shift-clic izquierdo.
     */
    @Test
    void terminalWithdrawsOneWithLeftClickAndStackWithShift() {
        Network net = networkWithCell();
        assertEquals(0, net.storage().deposit(new ItemStack(Material.COBBLESTONE, 128)));
        plugin.networks().invalidateNear(world.getBlockAt(1, 64, 0));

        rightClick(world.getBlockAt(0, 64, 1));

        clickTop(0, ClickType.LEFT);
        assertEquals(Material.COBBLESTONE, player.getItemOnCursor().getType(),
                "left click leaves 1 item on cursor");
        assertEquals(1, player.getItemOnCursor().getAmount());

        player.setItemOnCursor(null);
        clickTop(0, ClickType.SHIFT_LEFT);
        assertEquals(Material.COBBLESTONE, player.getInventory().getItem(0) == null
                        ? null : player.getInventory().getItem(0).getType(),
                "shift sends up to a stack to player inventory");
        assertEquals(64, player.getInventory().getItem(0).getAmount());
        assertEquals(63, net.storage().count(i -> i.getType() == Material.COBBLESTONE));
    }

    /**
     * [EN] Terminal deposits any item left in input slot into network storage on menu close.
     * [ES] La terminal guarda los items dejados en la ranura de entrada en la red al cerrar el menú.
     */
    @Test
    void terminalStoresInputItemsOnClose() {
        Network net = networkWithCell();
        rightClick(world.getBlockAt(0, 64, 1));
        player.getOpenInventory().getTopInventory().setItem(8, new ItemStack(Material.IRON_INGOT, 32));
        player.closeInventory();
        assertEquals(32, net.storage().count(i -> i.getType() == Material.IRON_INGOT),
                "items left in input slot are saved to network upon closing");
    }

    /**
     * [EN] Shift clicking an item in player inventory deposits it into the open network.
     * [ES] Shift-clic en un item del inventario del jugador lo deposita en la red abierta.
     */
    @Test
    void shiftClickOnPlayerInventoryDepositsIntoNetwork() {
        Network net = networkWithCell();
        rightClick(world.getBlockAt(0, 64, 1));
        player.getInventory().setItem(3, new ItemStack(Material.GOLD_INGOT, 10));
        clickBottomShift(3);
        assertEquals(10, net.storage().count(i -> i.getType() == Material.GOLD_INGOT));
        assertNull(player.getInventory().getItem(3));
    }

    /**
     * [EN] Terminal allows withdrawing custom items with custom ID in PDC and lore.
     * [ES] La terminal permite extraer items custom con ID personalizado en PDC y lore.
     */
    @Test
    void terminalWithdrawsCustomItemWithCustomIdAndLore() {
        Network net = networkWithCell();
        ItemStack custom = new ItemStack(Material.STICK, 10);
        var meta = custom.getItemMeta();
        meta.displayName(Component.text("Magic Wand"));
        meta.lore(List.of(Component.text("Custom ID: magic_wand"), Component.text("Tier: Legendary")));
        NamespacedKey customKey = new NamespacedKey(plugin, "custom_id");
        meta.getPersistentDataContainer().set(customKey, PersistentDataType.STRING, "magic_wand");
        custom.setItemMeta(meta);

        assertEquals(0, net.storage().deposit(custom));
        plugin.networks().invalidateNear(world.getBlockAt(1, 64, 0));

        rightClick(world.getBlockAt(0, 64, 1));

        // 1) Left click withdraws 1 custom item onto cursor
        clickTop(0, ClickType.LEFT);
        ItemStack onCursor = player.getItemOnCursor();
        assertNotNull(onCursor, "item on cursor must not be null");
        assertEquals(Material.STICK, onCursor.getType());
        assertEquals(1, onCursor.getAmount());
        assertTrue(onCursor.hasItemMeta());
        assertEquals("magic_wand", onCursor.getItemMeta().getPersistentDataContainer().get(customKey, PersistentDataType.STRING),
                "custom ID in PDC must be fully preserved");
        assertEquals(2, onCursor.getItemMeta().lore().size(),
                "original lore must be fully preserved");

        // 2) Shift click withdraws remaining custom items into inventory
        player.setItemOnCursor(null);
        clickTop(0, ClickType.SHIFT_LEFT);
        ItemStack inPlayerInv = player.getInventory().getItem(0);
        assertNotNull(inPlayerInv, "shift click must withdraw custom items to inventory");
        assertEquals(9, inPlayerInv.getAmount());
        assertEquals("magic_wand", inPlayerInv.getItemMeta().getPersistentDataContainer().get(customKey, PersistentDataType.STRING),
                "custom ID in PDC preserved on shift-click withdrawal");
        assertEquals(2, inPlayerInv.getItemMeta().lore().size(),
                "original lore preserved on shift-click withdrawal");
        assertEquals(0, net.storage().count(i -> i.getType() == Material.STICK));
    }

    // ------------------------------------------------------------ encoder + crafter

    private ShapedRecipe registerRecipe() {
        NamespacedKey key = new NamespacedKey(plugin, "test_blockcraft");
        ShapedRecipe recipe = new ShapedRecipe(key, new ItemStack(Material.CLOCK, 2));
        recipe.shape("DDD", "D D", "DDD");
        recipe.setIngredient('D', Material.DIAMOND);
        server.addRecipe(recipe);
        return recipe;
    }

    /**
     * [EN] Encoder encodes a 3x3 crafting matrix into a blueprint item.
     * [ES] El codificador guarda la matriz de crafteo 3x3 dentro del item blueprint.
     */
    @Test
    void encoderEncodesMatrixIntoBlueprint() {
        registerRecipe();
        Block encoder = place(5, 64, 0, DeviceType.MVN_ENCODER);
        rightClick(encoder);
        assertTrue(player.getOpenInventory().getTopInventory().getHolder() instanceof EncoderMenu);

        int[] matrix = {12, 13, 14, 21, 23, 30, 31, 32};
        for (int slot : matrix) {
            player.getOpenInventory().setCursor(new ItemStack(Material.DIAMOND, 1));
            clickTop(slot, ClickType.LEFT);
        }
        player.getOpenInventory().setCursor(null);
        player.getOpenInventory().getTopInventory().setItem(19, Items.create(DeviceType.MVN_BLUEPRINT));

        clickTop(16, ClickType.LEFT); // encode

        ItemStack output = player.getOpenInventory().getTopInventory().getItem(34);
        assertNotNull(output, "encoding produces blueprint in output slot");
        RecipeData data = Blueprints.read(output);
        assertNotNull(data, "blueprint contains embedded recipe data");
        assertEquals(Material.CLOCK, data.output.getType());
        assertEquals(Material.DIAMOND, data.inputs[0].getType());
        assertNull(data.inputs[4], "empty matrix center remains null");

        NodeBlob blob = NodeStore.get(encoder);
        assertNotNull(blob.craftingMatrix[0], "encoder matrix is persisted in block");
    }

    /**
     * [EN] Crafter with blueprint crafts items from network storage atomically.
     * [ES] El crafter con blueprint fabrica items usando el almacenamiento de la red atómicamente.
     */
    @Test
    void crafterWithBlueprintCraftsFromNetworkAtomically() {
        registerRecipe();
        Network net = networkWithCell();
        place(1, 63, 0, DeviceType.MVN_CELL_T2);
        place(2, 64, 0, DeviceType.MVN_CRAFTER);
        Block crafter = world.getBlockAt(2, 64, 0);

        ItemStack[] matrix = new ItemStack[]{new ItemStack(Material.DIAMOND), new ItemStack(Material.DIAMOND),
                new ItemStack(Material.DIAMOND), new ItemStack(Material.DIAMOND), null,
                new ItemStack(Material.DIAMOND), new ItemStack(Material.DIAMOND), new ItemStack(Material.DIAMOND),
                new ItemStack(Material.DIAMOND)};
        RecipeData data = new RecipeData(matrix, new ItemStack(Material.CLOCK, 2));
        NodeBlob blob = NodeStore.get(crafter);
        blob.blueprintData.add(Blueprints.encode(data));
        NodeStore.put(crafter, blob);

        // Insufficient ingredients (7 out of 8 required) -> nothing consumed
        net.storage().deposit(new ItemStack(Material.DIAMOND, 7));
        assertEquals(false, com.chagui68.multiversenets.craft.CraftingSupport.tryCraftBlueprint(net, data));
        assertEquals(7, net.storage().count(i -> i.getType() == Material.DIAMOND),
                "without sufficient ingredients network storage remains untouched");

        // Sufficient ingredients (8 diamonds) -> crafts atomically
        net.storage().deposit(new ItemStack(Material.DIAMOND, 8));
        assertTrue(com.chagui68.multiversenets.craft.CraftingSupport.tryCraftBlueprint(net, data));
        assertEquals(7, net.storage().count(i -> i.getType() == Material.DIAMOND));
        assertEquals(2, net.storage().count(i -> i.getType() == Material.CLOCK),
                "crafted results are deposited into network storage");
    }

    // ------------------------------------------------------------ crafting grid

    /**
     * [EN] Crafting grid consumes items from network storage and returns result to player.
     * [ES] La mesa de crafteo consume ingredientes de la red y entrega el resultado al jugador.
     */
    @Test
    void craftingGridUsesNetworkAndReturnsResult() {
        registerRecipe();
        Network net = networkWithCell();
        Block grid = place(0, 63, 0, DeviceType.MVN_CRAFTING_GRID);
        rightClick(grid);
        assertTrue(player.getOpenInventory().getTopInventory().getHolder() instanceof CraftingGridMenu);

        int[] top = {0, 1, 2, 9, 11, 18, 19, 20};
        for (int slot : top) {
            player.getOpenInventory().setCursor(new ItemStack(Material.DIAMOND, 1));
            clickTop(slot, ClickType.LEFT);
        }
        player.getOpenInventory().setCursor(null);

        net.storage().deposit(new ItemStack(Material.DIAMOND, 9));
        clickTop(33, ClickType.LEFT); // Craft x1

        boolean received = false;
        for (ItemStack it : player.getInventory().getContents()) {
            if (it != null && it.getType() == Material.CLOCK) {
                received = true;
                break;
            }
        }
        assertTrue(received, "player receives crafted result");
        assertEquals(1, net.storage().count(i -> i.getType() == Material.DIAMOND),
                "consumed 8 ingredients from network storage");
    }

    // ------------------------------------------------------------ monitor and filterables

    /**
     * [EN] Monitor opens its GUI when connected to a network.
     * [ES] El monitor abre su GUI cuando está conectado a una red.
     */
    @Test
    void monitorOpensWithNetwork() {
        networkWithCell();
        place(2, 64, 0, DeviceType.MVN_MONITOR);
        rightClick(world.getBlockAt(2, 64, 0));
        assertTrue(player.getOpenInventory().getTopInventory().getHolder() instanceof MonitorMenu);
    }

    /**
     * [EN] Grabber, Vacuum, and Crafter open their respective menus on right click.
     * [ES] Grabber, Vacuum y Crafter abren sus respectivos menús al hacer clic derecho.
     */
    @Test
    void grabberAndVacuumOpenFilter() {
        Block grabber = place(6, 64, 0, DeviceType.MVN_GRABBER);
        rightClick(grabber);
        assertTrue(player.getOpenInventory().getTopInventory().getHolder() instanceof FilterMenu);
        player.closeInventory();

        Block vacuum = place(7, 64, 0, DeviceType.MVN_VACUUM);
        rightClick(vacuum);
        assertTrue(player.getOpenInventory().getTopInventory().getHolder() instanceof FilterMenu);
        player.closeInventory();

        Block crafter = place(8, 64, 0, DeviceType.MVN_CRAFTER);
        rightClick(crafter);
        assertTrue(player.getOpenInventory().getTopInventory().getHolder() instanceof CrafterMenu);
    }
}

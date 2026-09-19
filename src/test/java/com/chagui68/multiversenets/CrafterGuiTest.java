package com.chagui68.multiversenets;

import com.chagui68.multiversenets.craft.Blueprints;
import com.chagui68.multiversenets.craft.RecipeData;
import com.chagui68.multiversenets.gui.CrafterMenu;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EN] Tests AutoCrafter GUI interactions: installing blueprints, uninstalling, and clearing recipes.
 * [ES] Pruebas de interacción con la GUI del AutoCrafter: instalar blueprints, desinstalar y limpiar recetas.
 */
class CrafterGuiTest {

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

    private Block placeCrafter() {
        Block block = world.getBlockAt(0, 64, 0);
        block.setType(DeviceType.MVN_CRAFTER.material());
        NodeStore.put(block, NodeBlob.create(DeviceType.MVN_CRAFTER.name()));
        return block;
    }

    private ItemStack createSampleBlueprint() {
        ItemStack[] matrix = new ItemStack[]{
                new ItemStack(Material.IRON_INGOT), new ItemStack(Material.IRON_INGOT), new ItemStack(Material.IRON_INGOT),
                null, new ItemStack(Material.STICK), null,
                null, new ItemStack(Material.STICK), null
        };
        RecipeData data = new RecipeData(matrix, new ItemStack(Material.IRON_PICKAXE, 1));
        return Blueprints.toItem(data);
    }

    /**
     * [EN] Right clicking the crafter block opens CrafterMenu.
     * [ES] Clic derecho en el bloque crafter abre CrafterMenu.
     */
    @Test
    void rightClickOpensCrafterMenu() {
        Block crafter = placeCrafter();
        PlayerInteractEvent interact = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK,
                null, crafter, BlockFace.NORTH, EquipmentSlot.HAND, null);
        server.getPluginManager().callEvent(interact);
        assertTrue(player.getOpenInventory().getTopInventory().getHolder() instanceof CrafterMenu,
                "right click on crafter opens CrafterMenu");
    }

    /**
     * [EN] Shift clicking a blueprint in player inventory installs it into the crafter.
     * [ES] Shift-clic en un blueprint del inventario lo instala en el crafter.
     */
    @Test
    void shiftClickInstallsBlueprint() {
        Block crafter = placeCrafter();
        ItemStack bp = createSampleBlueprint();
        player.getInventory().setItem(0, bp);

        new CrafterMenu(plugin, player, crafter).openMenu();

        InventoryClickEvent shift = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 27, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY);
        server.getPluginManager().callEvent(shift);

        NodeBlob blob = NodeStore.get(crafter);
        assertEquals(1, blob.blueprintData.size(), "blueprint is installed in blob");
        RecipeData decoded = Blueprints.decode(blob.blueprintData.get(0));
        assertNotNull(decoded);
        assertEquals(Material.IRON_PICKAXE, decoded.output.getType());
    }

    /**
     * [EN] Clicking an installed blueprint slot with empty cursor uninstalls it back to player.
     * [ES] Clic sobre un blueprint instalado con el cursor vacío lo desinstala devolviéndolo al jugador.
     */
    @Test
    void clickWithEmptyCursorUninstallsBlueprint() {
        Block crafter = placeCrafter();
        ItemStack bp = createSampleBlueprint();
        RecipeData data = Blueprints.read(bp);
        NodeBlob blob = NodeStore.get(crafter);
        blob.blueprintData.add(Blueprints.encode(data));
        NodeStore.put(crafter, blob);

        new CrafterMenu(plugin, player, crafter).openMenu();
        player.getOpenInventory().setCursor(null);

        InventoryClickEvent click = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 0, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(click);

        NodeBlob after = NodeStore.get(crafter);
        assertTrue(after.blueprintData.isEmpty(), "clicking installed blueprint slot uninstalls it");
    }

    /**
     * [EN] Clear button removes all installed blueprints and recipes.
     * [ES] El botón Clear limpia todos los blueprints y recetas instaladas.
     */
    @Test
    void clearButtonRemovesAllBlueprints() {
        Block crafter = placeCrafter();
        ItemStack bp = createSampleBlueprint();
        RecipeData data = Blueprints.read(bp);
        NodeBlob blob = NodeStore.get(crafter);
        blob.blueprintData.add(Blueprints.encode(data));
        blob.recipes.add("minecraft:iron_ingot");
        NodeStore.put(crafter, blob);

        new CrafterMenu(plugin, player, crafter).openMenu();

        InventoryClickEvent clearClick = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, CrafterMenu.CLEAR_SLOT, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(clearClick);

        NodeBlob after = NodeStore.get(crafter);
        assertTrue(after.blueprintData.isEmpty(), "clear must empty blueprintData");
        assertTrue(after.recipes.isEmpty(), "clear must empty recipes");
    }
}

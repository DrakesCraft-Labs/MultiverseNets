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

    private Block colocarCrafter() {
        Block block = world.getBlockAt(0, 64, 0);
        block.setType(DeviceType.CRAFTER.material());
        NodeStore.put(block, NodeBlob.create(DeviceType.CRAFTER.name()));
        return block;
    }

    private ItemStack crearBlueprintSample() {
        ItemStack[] matriz = new ItemStack[]{
                new ItemStack(Material.IRON_INGOT), new ItemStack(Material.IRON_INGOT), new ItemStack(Material.IRON_INGOT),
                null, new ItemStack(Material.STICK), null,
                null, new ItemStack(Material.STICK), null
        };
        RecipeData data = new RecipeData(matriz, new ItemStack(Material.IRON_PICKAXE, 1));
        return Blueprints.toItem(data);
    }

    @Test
    void clicDerechoAbreCrafterMenu() {
        Block crafter = colocarCrafter();
        PlayerInteractEvent interact = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK,
                null, crafter, BlockFace.NORTH, EquipmentSlot.HAND, null);
        server.getPluginManager().callEvent(interact);
        assertTrue(player.getOpenInventory().getTopInventory().getHolder() instanceof CrafterMenu,
                "clic derecho al crafter abre CrafterMenu");
    }

    @Test
    void shiftClickInstalaBlueprint() {
        Block crafter = colocarCrafter();
        ItemStack bp = crearBlueprintSample();
        player.getInventory().setItem(0, bp);

        new CrafterMenu(plugin, player, crafter).openMenu();

        InventoryClickEvent shift = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 27, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY);
        server.getPluginManager().callEvent(shift);

        NodeBlob blob = NodeStore.get(crafter);
        assertEquals(1, blob.blueprintData.size(), "el blueprint se instala en el blob");
        RecipeData decoded = Blueprints.decode(blob.blueprintData.get(0));
        assertNotNull(decoded);
        assertEquals(Material.IRON_PICKAXE, decoded.output.getType());
    }

    @Test
    void clicSinCursorDesinstalaBlueprint() {
        Block crafter = colocarCrafter();
        ItemStack bp = crearBlueprintSample();
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
        assertTrue(after.blueprintData.isEmpty(), "clic sobre el blueprint instalado lo desinstala");
    }

    @Test
    void botonClearLimpiaTodosLosBlueprints() {
        Block crafter = colocarCrafter();
        ItemStack bp = crearBlueprintSample();
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
        assertTrue(after.blueprintData.isEmpty(), "clear debe vaciar blueprintData");
        assertTrue(after.recipes.isEmpty(), "clear debe vaciar recipes");
    }
}

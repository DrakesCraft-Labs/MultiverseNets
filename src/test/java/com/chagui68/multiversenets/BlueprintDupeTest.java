package com.chagui68.multiversenets;

import com.chagui68.multiversenets.craft.Blueprints;
import com.chagui68.multiversenets.craft.RecipeData;
import com.chagui68.multiversenets.gui.CrafterMenu;
import com.chagui68.multiversenets.gui.EncoderMenu;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EN] Blueprints held by the Recipe Encoder and the crafters exist in exactly one place at a time.
 *
 * [ES] Los planos que guardan el Codificador y los crafters existen en un solo sitio a la vez.
 */
class BlueprintDupeTest {

    /** EncoderMenu.BLANK_SLOT / OUTPUT_SLOT (private there). */
    private static final int ENCODER_BLANK_SLOT = 19;

    private ServerMock server;
    private MultiverseNets plugin;
    private WorldMock world;
    private PlayerMock alice;
    private PlayerMock bob;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(MultiverseNets.class);
        world = server.addSimpleWorld("world");
        alice = server.addPlayer();
        bob = server.addPlayer();
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

    private static ItemStack blueprint() {
        ItemStack i = new ItemStack(Material.IRON_INGOT);
        ItemStack s = new ItemStack(Material.STICK);
        return Blueprints.toItem(new RecipeData(new ItemStack[]{i, i, i, null, s, null, null, s, null},
                new ItemStack(Material.IRON_PICKAXE)));
    }

    private static int blueprintsIn(PlayerMock player) {
        int total = 0;
        for (ItemStack it : player.getInventory().getContents()) {
            if (it != null && Items.typeOf(it) == DeviceType.MVN_BLUEPRINT) {
                total += it.getAmount();
            }
        }
        return total;
    }

    @Test
    void twoPlayersOpeningTheEncoderDoNotBothGetItsBlueprints() {
        Block encoder = place(DeviceType.MVN_ENCODER);
        NodeBlob blob = NodeStore.get(encoder);
        ItemStack stored = Items.create(DeviceType.MVN_BLUEPRINT);
        stored.setAmount(16);
        blob.encoderBlank = stored;
        NodeStore.put(encoder, blob);

        new EncoderMenu(plugin, alice, encoder).openMenu();
        new EncoderMenu(plugin, bob, encoder).openMenu();

        ItemStack inAlice = alice.getOpenInventory().getTopInventory().getItem(ENCODER_BLANK_SLOT);
        ItemStack inBob = bob.getOpenInventory().getTopInventory().getItem(ENCODER_BLANK_SLOT);
        int seen = (inAlice == null ? 0 : inAlice.getAmount()) + (inBob == null ? 0 : inBob.getAmount());
        assertEquals(16, seen, "the 16 stored blueprints are shown to one viewer, not copied to both");
        assertNull(NodeStore.get(encoder).encoderBlank, "while a menu holds them, the block does not");

        alice.closeInventory();
        bob.closeInventory();
        assertEquals(16, NodeStore.get(encoder).encoderBlank.getAmount(), "closing hands them back to the block");
    }

    @Test
    void breakingTheEncoderWithTheMenuOpenDoesNotDuplicate() {
        Block encoder = place(DeviceType.MVN_ENCODER);
        NodeBlob blob = NodeStore.get(encoder);
        blob.encoderBlank = Items.create(DeviceType.MVN_BLUEPRINT);
        NodeStore.put(encoder, blob);

        new EncoderMenu(plugin, alice, encoder).openMenu();
        server.getPluginManager().callEvent(new org.bukkit.event.block.BlockBreakEvent(encoder, bob));
        alice.closeInventory();

        long onGround = world.getEntities().stream()
                .filter(e -> e instanceof org.bukkit.entity.Item item
                        && Items.typeOf(item.getItemStack()) == DeviceType.MVN_BLUEPRINT)
                .count();
        assertEquals(1, onGround + blueprintsIn(alice), "the blueprint ends up in exactly one place");
    }

    @Test
    void installingFromTheInventoryConsumesTheBlueprintAndClearAllGivesItBack() {
        Block crafter = place(DeviceType.MVN_CRAFTER);
        alice.getInventory().setItem(0, blueprint());
        new CrafterMenu(plugin, alice, crafter).openMenu();

        server.getPluginManager().callEvent(new InventoryClickEvent(alice.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, 27, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY));

        assertEquals(1, NodeStore.get(crafter).blueprintData.size());
        assertEquals(0, blueprintsIn(alice), "installing takes the blueprint, since removing it gives one back");

        server.getPluginManager().callEvent(new InventoryClickEvent(alice.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, CrafterMenu.CLEAR_SLOT, ClickType.LEFT, InventoryAction.PICKUP_ALL));

        assertTrue(NodeStore.get(crafter).blueprintData.isEmpty());
        assertEquals(1, blueprintsIn(alice), "Clear All returns the installed blueprints");
    }
}

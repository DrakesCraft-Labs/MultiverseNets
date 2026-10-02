package com.chagui68.multiversenets;

import com.chagui68.multiversenets.gui.SfEncoderMenu;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.net.NetworkTicker;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.StackUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EN] Regressions for the issues players reported: pushers emptying a network, pushers flooding a
 * machine with one ingredient, hoppers deleting items, the Infinity Barrel refusing custom items
 * it handed out, and the Slimefun Recipe Encoder not keeping its blueprints.
 *
 * [ES] Regresiones de los problemas reportados: pushers vaciando la red, pushers llenando una
 * máquina con un solo ingrediente, tolvas borrando ítems, el Infinity Barrel rechazando ítems
 * custom que él mismo entregó, y el Slimefun Recipe Encoder sin guardar sus planos.
 */
class ReportedIssuesTest {

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
        if (type == DeviceType.MVN_CONTROLLER) {
            plugin.networks().registerController(block);
        } else {
            plugin.networks().invalidateNear(block);
        }
        return block;
    }

    private Network network(Block controller) {
        Network net = plugin.networks().networkByController(controller.getLocation());
        net.scan();
        return net;
    }

    private void tick() {
        new NetworkTicker(plugin, plugin.networks()).tick();
    }

    private static Inventory inventoryOf(Block block) {
        return ((Container) block.getState()).getInventory();
    }

    private static int countIn(Inventory inv, Material material) {
        int total = 0;
        for (ItemStack it : inv.getContents()) {
            if (it != null && it.getType() == material) {
                total += it.getAmount();
            }
        }
        return total;
    }

    private static int slotsOf(Inventory inv, Material material) {
        int slots = 0;
        for (ItemStack it : inv.getContents()) {
            if (it != null && it.getType() == material) {
                slots++;
            }
        }
        return slots;
    }

    /** A network with storage for several item types and nothing else around. */
    private Network storageNetwork(Block controller, int cells) {
        for (int i = 1; i <= cells; i++) {
            place(i, 64, 0, DeviceType.MVN_CELL_T6);
        }
        return network(controller);
    }

    // ---------------------------------------------------------------- pushers

    @Test
    void advancedPusherNeverLosesItemsPushingAWholeCycleIntoAChest() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        Network net = storageNetwork(controller, 2);
        Block pusher = place(0, 64, 1, DeviceType.MVN_PUSHER_HT);
        NodeBlob blob = NodeStore.get(pusher);
        blob.filterBlacklist = true; // empty blacklist = everything
        blob.targetFace = "SOUTH";
        NodeStore.put(pusher, blob);
        Block chest = world.getBlockAt(0, 64, 2);
        chest.setType(Material.CHEST);
        assertEquals(0, net.storage().deposit(new ItemStack(Material.COBBLESTONE, 5000)));

        tick();

        int inChest = countIn(inventoryOf(chest), Material.COBBLESTONE);
        long inNet = net.storage().count(i -> i.getType() == Material.COBBLESTONE);
        assertTrue(inChest > 0, "the pusher delivered something");
        assertEquals(5000, inChest + inNet, "nothing disappears: chest + network = what there was");
        for (ItemStack it : inventoryOf(chest).getContents()) {
            if (it != null) {
                assertTrue(it.getAmount() <= it.getMaxStackSize(), "no slot holds more than a stack");
            }
        }
    }

    @Test
    void whitelistPusherLeavesRoomForEveryIngredient() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        Network net = storageNetwork(controller, 3);
        Block pusher = place(0, 64, 1, DeviceType.MVN_PUSHER_HT);
        NodeBlob blob = NodeStore.get(pusher);
        blob.filterItems.add(new ItemStack(Material.COPPER_INGOT));
        blob.filterItems.add(new ItemStack(Material.IRON_INGOT));
        blob.filterItems.add(new ItemStack(Material.GOLD_INGOT));
        blob.targetFace = "SOUTH";
        NodeStore.put(pusher, blob);
        Block dispenser = world.getBlockAt(0, 64, 2);
        dispenser.setType(Material.DISPENSER);
        for (Material m : List.of(Material.COPPER_INGOT, Material.IRON_INGOT, Material.GOLD_INGOT)) {
            assertEquals(0, net.storage().deposit(new ItemStack(m, 2000)));
        }

        tick();

        Inventory inv = inventoryOf(dispenser);
        for (Material m : List.of(Material.COPPER_INGOT, Material.IRON_INGOT, Material.GOLD_INGOT)) {
            assertTrue(slotsOf(inv, m) >= 1, m + " reached the machine");
            assertTrue(slotsOf(inv, m) <= 3, m + " takes at most its share of the 9 slots");
            long inNet = net.storage().count(i -> i.getType() == m);
            assertEquals(2000, countIn(inv, m) + inNet, m + " is conserved");
        }
    }

    @Test
    void blacklistedItemsStayInTheNetwork() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        Network net = storageNetwork(controller, 2);
        Block pusher = place(0, 64, 1, DeviceType.MVN_PUSHER);
        NodeBlob blob = NodeStore.get(pusher);
        blob.filterBlacklist = true;
        blob.filterItems.add(new ItemStack(Material.DIAMOND));
        NodeStore.put(pusher, blob);
        world.getBlockAt(0, 64, 2).setType(Material.CHEST);
        net.storage().deposit(new ItemStack(Material.DIAMOND, 50));
        net.storage().deposit(new ItemStack(Material.DIRT, 50));

        for (int i = 0; i < 5; i++) {
            tick();
        }

        assertEquals(50, net.storage().count(i -> i.getType() == Material.DIAMOND), "the blacklisted item stays");
        assertEquals(0, net.storage().count(i -> i.getType() == Material.DIRT), "everything else is exported");
    }

    @Test
    void pusherDoesNotFillTheHiddenInventoryOfAnInfinityBarrel() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        Network net = storageNetwork(controller, 1);
        Block pusher = place(0, 64, 1, DeviceType.MVN_PUSHER);
        NodeBlob blob = NodeStore.get(pusher);
        blob.filterItems.add(new ItemStack(Material.DIRT));
        NodeStore.put(pusher, blob);
        Block barrel = place(1, 64, 1, DeviceType.MVN_INFINITY_BARREL);
        net.storage().deposit(new ItemStack(Material.DIRT, 10));

        tick();

        assertEquals(0, countIn(inventoryOf(barrel), Material.DIRT), "nothing goes into the barrel block's own inventory");
        assertEquals(10, net.storage().count(i -> i.getType() == Material.DIRT));
    }

    // ---------------------------------------------------------------- hoppers

    @Test
    void hoppersNeverTouchANetworkDevice() {
        Block barrel = place(5, 64, 5, DeviceType.MVN_INFINITY_BARREL);
        Block hopper = world.getBlockAt(5, 65, 5);
        hopper.setType(Material.HOPPER);
        Inventory hopperInv = inventoryOf(hopper);
        hopperInv.setItem(0, new ItemStack(Material.IRON_INGOT, 64));
        Inventory barrelInv = inventoryOf(barrel);

        InventoryMoveItemEvent push = new InventoryMoveItemEvent(hopperInv, new ItemStack(Material.IRON_INGOT, 1), barrelInv, true);
        server.getPluginManager().callEvent(push);
        assertTrue(push.isCancelled(), "a hopper cannot feed a network device");
        assertEquals(64, countIn(hopperInv, Material.IRON_INGOT), "the hopper keeps its whole stack");
        NodeBlob after = NodeStore.get(barrel);
        assertTrue(after.cellSample == null && after.cellAmount == 0, "the barrel stored nothing");

        InventoryMoveItemEvent pull = new InventoryMoveItemEvent(barrelInv, new ItemStack(Material.IRON_INGOT, 1), hopperInv, false);
        server.getPluginManager().callEvent(pull);
        assertTrue(pull.isCancelled(), "a hopper cannot drain a network device");
    }

    // ---------------------------------------------------------------- infinity barrel

    private static ItemStack customItem(Component name) {
        ItemStack item = new ItemStack(Material.IRON_INGOT);
        var meta = item.getItemMeta();
        meta.displayName(name);
        meta.getPersistentDataContainer().set(new NamespacedKey("slimefun", "slimefun_item"), PersistentDataType.STRING, "STEEL_INGOT");
        item.setItemMeta(meta);
        return item;
    }

    @Test
    void customItemsStillMatchAfterTheirNameIsStoredDifferently() {
        ItemStack original = customItem(Component.text("Steel Ingot", NamedTextColor.WHITE));
        ItemStack reloaded = customItem(Component.text("").append(Component.text("Steel Ingot", NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false)));
        assertTrue(StackUtils.itemsMatch(original, reloaded), "same plugin id and same visible text = same item");

        ItemStack other = customItem(Component.text("Steel Ingot"));
        var meta = other.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey("slimefun", "slimefun_item"), PersistentDataType.STRING, "DAMASCUS_STEEL_INGOT");
        other.setItemMeta(meta);
        assertFalse(StackUtils.itemsMatch(original, other), "a different plugin id is a different item");

        ItemStack renamed = customItem(Component.text("My Steel"));
        assertFalse(StackUtils.itemsMatch(original, renamed), "a renamed copy is still a different item");
    }

    @Test
    void infinityBarrelTakesBackACustomItemItHandedOut() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        place(1, 64, 0, DeviceType.MVN_INFINITY_BARREL);
        Network net = network(controller);
        assertEquals(0, net.storage().deposit(StackUtils.getAsQuantity(customItem(Component.text("Steel Ingot")), 10)));
        ItemStack out = net.storage().withdraw(i -> true, 4);
        assertNotNull(out);
        ItemStack back = StackUtils.getAsQuantity(customItem(Component.text("")
                .append(Component.text("Steel Ingot"))), out.getAmount());
        assertEquals(0, net.storage().deposit(back), "the same item goes back in");
        assertEquals(10, net.storage().count(i -> "STEEL_INGOT".equals(
                com.chagui68.multiversenets.compat.SlimefunBridge.getId(i))));
    }

    // ---------------------------------------------------------------- encoders

    @Test
    void slimefunEncoderKeepsItsBlueprintsWhenClosed() {
        Block encoder = place(9, 64, 9, DeviceType.MVN_SF_ENCODER);
        ItemStack blanks = Items.create(DeviceType.MVN_BLUEPRINT);
        blanks.setAmount(12);
        new SfEncoderMenu(plugin, player, encoder).openMenu();
        player.getOpenInventory().getTopInventory().setItem(19, blanks);
        player.closeInventory();

        NodeBlob blob = NodeStore.get(encoder);
        assertNotNull(blob.encoderBlank, "closing leaves the blueprints in the encoder");
        assertEquals(12, blob.encoderBlank.getAmount());
        for (ItemStack it : player.getInventory().getContents()) {
            assertTrue(it == null || Items.typeOf(it) != DeviceType.MVN_BLUEPRINT, "nothing was handed back");
        }

        new SfEncoderMenu(plugin, player, encoder).openMenu();
        ItemStack shown = player.getOpenInventory().getTopInventory().getItem(19);
        assertNotNull(shown);
        assertEquals(12, shown.getAmount(), "reopening shows them again");
        assertNull(NodeStore.get(encoder).encoderBlank, "while the menu holds them the block does not");
    }
}

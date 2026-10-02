package com.chagui68.multiversenets;

import com.chagui68.multiversenets.craft.CraftingSupport;
import com.chagui68.multiversenets.craft.RecipeData;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.net.NetworkTicker;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.Keys;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EN] Item and fluid transmission between devices, internal storages and networks: every case here
 * used to lose, duplicate or ignore something.
 *
 * [ES] Transmisión de ítems y fluidos entre dispositivos, almacenamientos internos y redes: cada caso
 * de aquí perdía, duplicaba o ignoraba algo.
 */
class TransmissionFixesTest {

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

    private void click(ItemStack held, Block block) {
        player.getInventory().setItemInMainHand(held);
        server.getPluginManager().callEvent(new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, held, block,
                BlockFace.NORTH, EquipmentSlot.HAND, null));
    }

    private static ItemStack named(Material material, String name) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        item.setItemMeta(meta);
        return item;
    }

    // ---------------------------------------------------------------- fluids

    @Test
    void fluidDepositIsAllOrNothing() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        place(1, 64, 0, DeviceType.MVN_FLUID_CELL);
        Network net = network(controller);

        assertEquals(0, net.fluidStorage().deposit("WATER", 63_500));
        assertEquals(1_000, net.fluidStorage().deposit("WATER", 1_000), "a bucket that does not fit is refused whole");
        assertEquals(63_500, net.fluidStorage().count("WATER"), "nothing of the refused bucket stays in the cell");
    }

    @Test
    void pumpKeepsTheSourceWhenTheNetworkCannotTakeAWholeBucket() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        place(1, 64, 0, DeviceType.MVN_FLUID_CELL);
        place(2, 64, 0, DeviceType.MVN_LIQUID_PUMP);
        Block source = world.getBlockAt(2, 63, 0);
        source.setType(Material.WATER);
        Network net = network(controller);
        net.fluidStorage().deposit("WATER", 63_500);

        tick();

        assertEquals(Material.WATER, source.getType(), "the source block stays when its 1,000 mB do not fit");
        assertEquals(63_500, net.fluidStorage().count("WATER"), "no partial fluid is created out of nothing");
    }

    @Test
    void pumpDrainsTheSourceWhenThereIsRoom() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        place(1, 64, 0, DeviceType.MVN_FLUID_CELL);
        place(2, 64, 0, DeviceType.MVN_LIQUID_PUMP);
        Block source = world.getBlockAt(2, 63, 0);
        source.setType(Material.WATER);
        Network net = network(controller);

        tick();

        assertEquals(Material.AIR, source.getType());
        assertEquals(1_000, net.fluidStorage().count("WATER"));
    }

    // ---------------------------------------------------------------- wireless bridge

    @Test
    void receiverBridgesWithAnItemOnlyFilter() {
        Block controllerA = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        place(1, 64, 0, DeviceType.MVN_CELL_T1);
        Block transmitter = place(2, 64, 0, DeviceType.MVN_TRANSMITTER);
        Block controllerB = place(20, 64, 0, DeviceType.MVN_CONTROLLER);
        place(21, 64, 0, DeviceType.MVN_CELL_T1);
        Block receiver = place(22, 64, 0, DeviceType.MVN_RECEIVER);

        NodeBlob rx = NodeStore.get(receiver);
        rx.txWorld = world.getUID().toString();
        rx.txX = transmitter.getX();
        rx.txY = transmitter.getY();
        rx.txZ = transmitter.getZ();
        // Filtro solo por plantilla exacta: es lo que guarda el FilterMenu para un item con nombre.
        rx.filterItems = new ArrayList<>(List.of(new ItemStack(Material.IRON_INGOT)));
        NodeStore.put(receiver, rx);

        Network a = network(controllerA);
        Network b = network(controllerB);
        a.storage().deposit(new ItemStack(Material.IRON_INGOT, 50));

        tick();

        assertEquals(0, a.storage().count(i -> i.getType() == Material.IRON_INGOT));
        assertEquals(50, b.storage().count(i -> i.getType() == Material.IRON_INGOT),
                "a filter configured only with item templates must still open the bridge");
    }

    @Test
    void receiverDoesNotDrainRemoteGreedyCells() {
        Block controllerA = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        Block greedy = place(1, 64, 0, DeviceType.MVN_GREEDY_CELL);
        Block transmitter = place(2, 64, 0, DeviceType.MVN_TRANSMITTER);
        Block controllerB = place(20, 64, 0, DeviceType.MVN_CONTROLLER);
        place(21, 64, 0, DeviceType.MVN_CELL_T1);
        Block receiver = place(22, 64, 0, DeviceType.MVN_RECEIVER);

        NodeBlob g = NodeStore.get(greedy);
        g.filterMaterials.add("GOLD_INGOT");
        g.addGreedyItem(new ItemStack(Material.GOLD_INGOT), 30);
        NodeStore.put(greedy, g);

        NodeBlob rx = NodeStore.get(receiver);
        rx.txWorld = world.getUID().toString();
        rx.txX = transmitter.getX();
        rx.txY = transmitter.getY();
        rx.txZ = transmitter.getZ();
        rx.filterMaterials.add("GOLD_INGOT");
        NodeStore.put(receiver, rx);

        network(controllerA);
        Network b = network(controllerB);

        tick();

        assertEquals(0, b.storage().count(i -> i.getType() == Material.GOLD_INGOT),
                "stock reserved in a Greedy Cell does not cross the bridge, same as with Pushers");
        assertEquals(30, NodeStore.get(greedy).totalGreedyAmount());
    }

    // ---------------------------------------------------------------- shared buses

    @Test
    void deviceSharedByTwoControllersWorksOncePerCycle() {
        Block controllerA = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        place(1, 64, 0, DeviceType.MVN_CABLE);
        Block controllerB = place(2, 64, 0, DeviceType.MVN_CONTROLLER);
        place(1, 63, 0, DeviceType.MVN_CELL_T2);
        Block purger = place(1, 65, 0, DeviceType.MVN_PURGER);
        NodeBlob p = NodeStore.get(purger);
        p.filterMaterials.add("COBBLESTONE");
        NodeStore.put(purger, p);

        Network a = network(controllerA);
        Network b = network(controllerB);
        assertTrue(a.contains(com.chagui68.multiversenets.util.PosUtil.pack(1, 65, 0)));
        assertTrue(b.contains(com.chagui68.multiversenets.util.PosUtil.pack(1, 65, 0)),
                "both controllers index the purger: this is the shared-bus case");
        a.storage().deposit(new ItemStack(Material.COBBLESTONE, 1_000));

        tick();

        int perOp = com.chagui68.multiversenets.util.Settings.itemsPerOp();
        assertEquals(1_000 - perOp, a.storage().count(i -> i.getType() == Material.COBBLESTONE),
                "the purger works once per cycle, not once per controller");
        assertEquals(a.storage().count(i -> true), b.storage().count(i -> true),
                "the shared cell is still visible from both networks");
    }

    // ---------------------------------------------------------------- crafting

    @Test
    void blueprintCraftIsUndoneWhenTheResultOnlyPartlyFits() {
        NamespacedKey key = new NamespacedKey(plugin, "test_partial_fit");
        ShapedRecipe recipe = new ShapedRecipe(key, new ItemStack(Material.CLOCK, 2));
        recipe.shape("DDD", "D D", "DDD");
        recipe.setIngredient('D', Material.DIAMOND);
        server.addRecipe(recipe);

        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        place(1, 64, 0, DeviceType.MVN_CELL_T2);
        place(1, 63, 0, DeviceType.MVN_CELL_T2);
        Block limiter = place(1, 65, 0, DeviceType.MVN_LIMITER);
        NodeBlob l = NodeStore.get(limiter);
        l.quotaSample = new ItemStack(Material.CLOCK);
        l.quotaLimit = 1;
        l.quotaActive = true;
        NodeStore.put(limiter, l);
        Network net = network(controller);
        net.storage().deposit(new ItemStack(Material.DIAMOND, 8));

        ItemStack d = new ItemStack(Material.DIAMOND);
        RecipeData data = new RecipeData(new ItemStack[]{d, d, d, d, null, d, d, d, d}, new ItemStack(Material.CLOCK, 2));

        assertFalse(CraftingSupport.tryCraftBlueprint(net, data));
        assertEquals(8, net.storage().count(i -> i.getType() == Material.DIAMOND), "ingredients come back");
        assertEquals(0, net.storage().count(i -> i.getType() == Material.CLOCK),
                "the part of the result that fit is taken back, so nothing is duplicated");
    }

    // ---------------------------------------------------------------- tools and node state

    @Test
    void wrenchPastesTheExactFilterTemplates() {
        Block source = place(0, 64, 0, DeviceType.MVN_GRABBER);
        Block target = place(5, 64, 0, DeviceType.MVN_GRABBER);
        NodeBlob s = NodeStore.get(source);
        s.filterItems.add(named(Material.DIAMOND, "Gem"));
        s.filterMaterials.add("DIAMOND");
        NodeStore.put(source, s);
        NodeBlob t = NodeStore.get(target);
        t.filterItems.add(new ItemStack(Material.COAL));
        t.filterMaterials.add("COAL");
        NodeStore.put(target, t);

        ItemStack wrench = Items.create(DeviceType.MVN_CONFIGURATOR);
        player.setSneaking(true);
        click(wrench, source);
        player.setSneaking(false);
        click(wrench, target);

        NodeBlob pasted = NodeStore.get(target);
        assertEquals(List.of("DIAMOND"), pasted.filterMaterials);
        assertEquals(1, pasted.filterItems.size(), "the old COAL template is replaced, not kept");
        assertTrue(pasted.filterItems.get(0).isSimilar(named(Material.DIAMOND, "Gem")),
                "the named template survives the copy, not just its material");
    }

    @Test
    void rakeGivesTheDeviceBackWithItsFilter() {
        Block grabber = place(0, 64, 0, DeviceType.MVN_GRABBER);
        NodeBlob g = NodeStore.get(grabber);
        g.filterMaterials.add("REDSTONE");
        g.filterItems.add(new ItemStack(Material.REDSTONE));
        NodeStore.put(grabber, g);

        click(Items.rake(), grabber);

        assertEquals(Material.AIR, grabber.getType());
        ItemStack recovered = null;
        for (ItemStack it : player.getInventory().getContents()) {
            if (it != null && Items.typeOf(it) == DeviceType.MVN_GRABBER) {
                recovered = it;
            }
        }
        assertNotNull(recovered, "the rake returns the dismantled device");
        String cargo = recovered.getItemMeta().getPersistentDataContainer().get(Keys.CELL_CARGO, PersistentDataType.STRING);
        assertNotNull(cargo, "its filter travels inside the item");
        assertEquals(List.of("REDSTONE"), NodeStore.decode(cargo).filterMaterials);
    }

    @Test
    void breakingAndPlacingKeepsFilterTemplatesFaceAndTransitBuffer() {
        Block grabber = place(0, 64, 0, DeviceType.MVN_GRABBER_HT);
        NodeBlob g = NodeStore.get(grabber);
        g.filterItems.add(named(Material.IRON_INGOT, "Special"));
        g.targetFace = "UP";
        g.transitBuffer = new ItemStack(Material.IRON_INGOT, 7);
        NodeStore.put(grabber, g);

        server.getPluginManager().callEvent(new BlockBreakEvent(grabber, player));
        ItemStack drop = null;
        for (var entity : world.getEntities()) {
            if (entity instanceof Item item && Items.typeOf(item.getItemStack()) == DeviceType.MVN_GRABBER_HT) {
                drop = item.getItemStack();
            }
        }
        assertNotNull(drop, "a grabber with only filter templates and a transit buffer still keeps its state");

        Block target = world.getBlockAt(10, 64, 0);
        var previous = target.getState();
        target.setType(DeviceType.MVN_GRABBER_HT.material());
        server.getPluginManager().callEvent(new BlockPlaceEvent(target, previous, target.getRelative(BlockFace.DOWN),
                drop, player, true, EquipmentSlot.HAND));

        NodeBlob restored = NodeStore.get(target);
        assertEquals(1, restored.filterItems.size());
        assertTrue(restored.filterItems.get(0).isSimilar(named(Material.IRON_INGOT, "Special")));
        assertEquals("UP", restored.targetFace);
        assertNotNull(restored.transitBuffer);
        assertEquals(7, restored.transitBuffer.getAmount(), "items waiting in the transit buffer are not lost");
    }
}

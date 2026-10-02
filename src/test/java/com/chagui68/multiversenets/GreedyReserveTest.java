package com.chagui68.multiversenets;

import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.net.NetworkTicker;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * [EN] Greedy Cell as an internal filter: Pushers can take from it but it always keeps 1 of every
 * item defined in its filter; a Pusher that whitelists the item releases it completely to the cells.
 *
 * [ES] Greedy Cell como filtro interno: los Pushers pueden sacar de ella pero siempre conserva 1 de
 * cada ítem definido en su filtro; un Pusher con el ítem en su whitelist lo libera entero a las celdas.
 */
class GreedyReserveTest {

    private ServerMock server;
    private MultiverseNets plugin;
    private WorldMock world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(MultiverseNets.class);
        world = server.addSimpleWorld("world");
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

    private void tick() {
        new NetworkTicker(plugin, plugin.networks()).tick();
    }

    private static long greedyStone(Block greedy) {
        NodeBlob blob = NodeStore.get(greedy);
        int idx = blob.indexOfGreedySample(new ItemStack(Material.STONE));
        return idx < 0 ? 0 : blob.greedyAmounts.get(idx);
    }

    private static int stoneIn(Block chest) {
        Inventory inv = ((Container) chest.getState()).getInventory();
        int total = 0;
        for (ItemStack it : inv.getContents()) {
            if (it != null && it.getType() == Material.STONE) {
                total += it.getAmount();
            }
        }
        return total;
    }

    /** Controller, a Greedy Cell that defines stone holding 100 of it, and a cell. */
    private Network setUpNetwork(Block[] greedyOut) {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        Block greedy = place(1, 64, 0, DeviceType.MVN_GREEDY_CELL);
        place(2, 64, 0, DeviceType.MVN_CELL_T1);
        NodeBlob blob = NodeStore.get(greedy);
        blob.filterItems.add(new ItemStack(Material.STONE));
        blob.addGreedyItem(new ItemStack(Material.STONE), 100);
        NodeStore.put(greedy, blob);
        greedyOut[0] = greedy;
        Network net = plugin.networks().networkByController(controller.getLocation());
        net.scan();
        return net;
    }

    @Test
    void aPusherTakesFromTheGreedyCellButLeavesOne() {
        Block[] greedy = new Block[1];
        setUpNetwork(greedy);
        Block pusher = place(0, 64, 1, DeviceType.MVN_PUSHER);
        NodeBlob pBlob = NodeStore.get(pusher);
        pBlob.filterBlacklist = true; // everything, stone is not named
        NodeStore.put(pusher, pBlob);
        Block chest = world.getBlockAt(0, 64, 2);
        chest.setType(Material.CHEST);

        tick();
        tick();

        assertEquals(99, stoneIn(chest), "all the stone leaves except one");
        assertEquals(1, greedyStone(greedy[0]), "the Greedy Cell keeps 1 as its internal filter");
    }

    @Test
    void anyWithdrawalLeavesTheReservedUnit() {
        Block[] greedy = new Block[1];
        Network net = setUpNetwork(greedy);

        ItemStack out = net.storage().withdraw(i -> i.getType() == Material.STONE, 1000);

        assertNotNull(out);
        assertEquals(99, out.getAmount());
        assertEquals(1, greedyStone(greedy[0]));
    }

    @Test
    void aPusherWhitelistingTheItemEmptiesTheGreedyCellIntoTheCells() {
        Block[] greedy = new Block[1];
        Network net = setUpNetwork(greedy);
        // A pusher with stone in its whitelist and nothing next to it to push into: the stone
        // simply moves from the Greedy Cell to the cells.
        Block pusher = place(0, 64, -1, DeviceType.MVN_PUSHER);
        NodeBlob pBlob = NodeStore.get(pusher);
        pBlob.filterItems.add(new ItemStack(Material.STONE));
        NodeStore.put(pusher, pBlob);
        net.scan();

        tick();

        assertEquals(0, greedyStone(greedy[0]), "the Greedy Cell is emptied completely, reserve included");
        assertEquals(100, net.storage().count(i -> i.getType() == Material.STONE), "nothing is lost");
        assertEquals(0, net.storage().deposit(new ItemStack(Material.STONE, 10)));
        assertEquals(0, greedyStone(greedy[0]), "new stone goes to the cells while that pusher exists");

        world.getBlockAt(0, 64, -1).setType(Material.AIR);
        NodeStore.remove(pusher);
        plugin.networks().invalidateNear(pusher);
        net.storage().deposit(new ItemStack(Material.STONE, 5));
        assertEquals(5, greedyStone(greedy[0]), "without the pusher, the Greedy Cell takes stone again");
    }
}

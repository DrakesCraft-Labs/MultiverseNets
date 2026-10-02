package com.chagui68.multiversenets;

import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.net.NetworkManager;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.Settings;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests verifying CPU Virtual Cache (T1-T5), Router Antenna, and Zero-Drop routing.
 */
class UpgradedFeaturesTest {

    private ServerMock server;
    private MultiverseNets plugin;
    private WorldMock world;
    private NetworkManager manager;

    @BeforeEach
    void setup() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(MultiverseNets.class);
        world = server.addSimpleWorld("world");
        manager = plugin.networks();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private Block place(int x, int y, int z, DeviceType type) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(type.material());
        NodeBlob blob = NodeBlob.create(type.name());
        NodeStore.put(block, blob);
        if (type == DeviceType.MVN_CONTROLLER) {
            manager.registerController(block);
        } else {
            manager.invalidateNear(block);
        }
        return block;
    }

    @Test
    void testVirtualCacheDepositAndWithdraw() {
        Block ctrl = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        // L1 module (2,048 items) in a DRAM Bay: modules no longer live in the Controller.
        Block bay = place(1, 64, 0, DeviceType.MVN_DRAM_BAY);
        NodeBlob blob = NodeStore.get(bay);
        assertNotNull(blob);
        assertTrue(com.chagui68.multiversenets.net.MemoryModules.install(blob,
                com.chagui68.multiversenets.item.Items.create(DeviceType.MVN_CACHE_L1)));
        NodeStore.put(bay, blob);

        Network net = manager.networkAt(ctrl);
        assertNotNull(net);
        net.scan();

        assertEquals(2048L, Settings.virtualCacheCapacity(1));

        // Deposit 100 diamonds
        ItemStack diamonds = new ItemStack(Material.DIAMOND, 64);
        int leftover1 = net.storage().deposit(diamonds);
        assertEquals(0, leftover1, "64 diamonds should fit entirely into L1 cache");

        ItemStack diamonds2 = new ItemStack(Material.DIAMOND, 36);
        int leftover2 = net.storage().deposit(diamonds2);
        assertEquals(0, leftover2, "36 diamonds should fit into L1 cache");

        // Verify total stored in virtual cache
        assertEquals(100L, net.storage().count(it -> it.getType() == Material.DIAMOND));

        // Withdraw 40 diamonds
        ItemStack withdrawn = net.storage().withdraw(it -> it.getType() == Material.DIAMOND, 40);
        assertNotNull(withdrawn);
        assertEquals(40, withdrawn.getAmount());

        // 60 should remain
        assertEquals(60L, net.storage().count(it -> it.getType() == Material.DIAMOND));
    }

    @Test
    void testRouterDeviceProperties() {
        assertTrue(DeviceType.MVN_ROUTER.placeable());
        assertTrue(DeviceType.MVN_ROUTER.isRouter());
        assertFalse(DeviceType.MVN_ROUTER.isCacheModule());

        assertTrue(DeviceType.MVN_CACHE_L1.isCacheModule());
        assertEquals(1, DeviceType.MVN_CACHE_L1.cacheTier());
        assertEquals(5, DeviceType.MVN_CACHE_QUANTUM.cacheTier());
        assertFalse(DeviceType.MVN_CACHE_L1.placeable());
    }

    @Test
    void testChunkNodeLimitCounting() {
        Block b1 = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        Block b2 = place(1, 64, 0, DeviceType.MVN_CABLE);
        Block b3 = place(2, 64, 0, DeviceType.MVN_CABLE);

        int count = NodeStore.countNodesInChunk(b1.getChunk());
        assertEquals(3, count, "Chunk should have 3 registered nodes");
    }

    @Test
    void testGrabberTransitBufferZeroDrop() {
        Block ctrl = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        Block grabber = place(1, 64, 0, DeviceType.MVN_GRABBER);
        NodeBlob blob = NodeStore.get(grabber);
        assertNotNull(blob);

        // Put transit buffer
        blob.transitBuffer = new ItemStack(Material.EMERALD, 16);
        NodeStore.put(grabber, blob);

        NodeBlob reloaded = NodeStore.get(grabber);
        assertNotNull(reloaded.transitBuffer);
        assertEquals(16, reloaded.transitBuffer.getAmount());
    }

    @Test
    void testPreserveVirtualCacheOnBreakAndPlace() {
        Block ctrl = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        NodeBlob blob = NodeStore.get(ctrl);
        assertNotNull(blob);
        blob.virtualCacheTier = 2; // L2 Cache
        blob.addVirtualItem(new ItemStack(Material.GOLD_INGOT), 500);
        NodeStore.put(ctrl, blob);

        // Encode and simulate drop
        String encoded = NodeStore.encode(blob);
        assertNotNull(encoded);

        NodeBlob decoded = NodeStore.decode(encoded);
        assertNotNull(decoded);
        assertEquals(2, decoded.virtualCacheTier);
        assertEquals(500L, decoded.totalVirtualAmount());

        // Simulate restore cargo on place
        Block newCtrl = place(10, 64, 10, DeviceType.MVN_CONTROLLER);
        NodeBlob actual = NodeStore.get(newCtrl);
        assertNotNull(actual);
        actual.virtualCacheTier = decoded.virtualCacheTier;
        actual.virtualSamples = decoded.virtualSamples;
        actual.virtualAmounts = decoded.virtualAmounts;
        NodeStore.put(newCtrl, actual);

        NodeBlob reloaded = NodeStore.get(newCtrl);
        assertEquals(2, reloaded.virtualCacheTier);
        assertEquals(500L, reloaded.totalVirtualAmount());
    }

    @Test
    void testCreativePlayerDoesNotDropItemOnBreak() {
        org.mockbukkit.mockbukkit.entity.PlayerMock player = server.addPlayer();
        player.setGameMode(org.bukkit.GameMode.CREATIVE);

        Block ctrl = place(5, 64, 5, DeviceType.MVN_CONTROLLER);
        org.bukkit.event.block.BlockBreakEvent event = new org.bukkit.event.block.BlockBreakEvent(ctrl, player);
        server.getPluginManager().callEvent(event);

        // Verify no dropped entities in world
        assertEquals(0, world.getEntitiesByClass(org.bukkit.entity.Item.class).size(),
                "Creative player breaking network node should not drop item");
    }
}

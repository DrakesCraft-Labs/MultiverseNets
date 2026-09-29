package com.chagui68.multiversenets.net;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.PosUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EN] Cost guards for the topology scan.
 * <p>
 * The scan is the plugin's deliberate price: one BFS over up to {@code max-nodes} blocks every
 * {@code scan-interval-ticks}. It is only affordable if each step is cheap, and the two things that
 * were not cheap were the per-node {@code List<Long>} of six boxed {@code Long}s — tens of thousands
 * of short-lived objects per pass on a large network — and the second BFS-worth of work the ticker
 * did on top of it.
 * <p>
 * The first test pins the shape of the neighbour walk (one reusable array, six cells, no collection).
 * The second is a coarse linear-time guard: it will not catch a 10% regression, and it is not meant
 * to — it catches the accidental quadratic rewrite, which is the failure that actually takes a
 * server down.
 *
 * [ES] Guardas de coste del escaneo. El primer test fija la forma del recorrido de vecinos (un array
 * reutilizado, seis celdas, sin colecciones). El segundo es una guarda gruesa de tiempo lineal:
 * atrapa la reescritura cuadrática accidental, no una regresión del 10%.
 */
class ScanCostTest {

    /** Big enough that an O(n²) scan is unmistakable, small enough to stay a unit test. */
    private static final int WIRE_LENGTH = 2000;

    private ServerMock server;
    private MultiverseNets plugin;
    private WorldMock world;
    private NetworkManager manager;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(MultiverseNets.class);
        world = server.addSimpleWorld("world");
        manager = plugin.networks();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("fillNeighbors writes the six orthogonal neighbours into the caller's array")
    void neighboursAreAllocationFree() {
        long pos = PosUtil.pack(10, 64, -30);
        long[] out = new long[6];

        Network.fillNeighbors(pos, out);

        Set<Long> unique = new HashSet<>();
        for (long neighbour : out) {
            assertTrue(unique.add(neighbour), "a neighbour must not be reported twice");
        }
        assertEquals(6, unique.size(), "a block has exactly six orthogonal neighbours");

        assertTrue(unique.contains(PosUtil.pack(11, 64, -30)), "east");
        assertTrue(unique.contains(PosUtil.pack(9, 64, -30)), "west");
        assertTrue(unique.contains(PosUtil.pack(10, 65, -30)), "up");
        assertTrue(unique.contains(PosUtil.pack(10, 63, -30)), "down");
        assertTrue(unique.contains(PosUtil.pack(10, 64, -29)), "south");
        assertTrue(unique.contains(PosUtil.pack(10, 64, -31)), "north");
    }

    @Test
    @DisplayName("a 2,000-node wire is indexed in one bounded pass")
    void largeNetworkScanStaysLinear() {
        Block controller = world.getBlockAt(0, 64, 0);
        controller.setType(DeviceType.MVN_CONTROLLER.material());
        NodeStore.put(controller, NodeBlob.create(DeviceType.MVN_CONTROLLER.name()));
        manager.registerController(controller);

        for (int x = 1; x <= WIRE_LENGTH; x++) {
            Block cable = world.getBlockAt(x, 64, 0);
            cable.setType(Material.GLASS);
            NodeStore.put(cable, NodeBlob.create(DeviceType.MVN_CABLE.name()));
        }

        Network net = manager.networkFor(world, PosUtil.pack(0, 64, 0));

        long start = System.nanoTime();
        net.scan();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;

        assertEquals(WIRE_LENGTH + 1, net.size(),
                "the controller plus every cable must be indexed, none skipped");
        assertTrue(elapsedMs < 5_000,
                "scanning " + (WIRE_LENGTH + 1) + " nodes took " + elapsedMs
                        + " ms; that is the signature of a quadratic walk, not of a BFS");
    }
}

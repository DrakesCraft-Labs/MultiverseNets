package com.chagui68.multiversenets.persist;

import com.chagui68.multiversenets.MultiverseNets;
import org.bukkit.block.Block;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * [EN] Tests for the decode cache that keeps the storage engine off the deserializer.
 * <p>
 * {@code NetworkStorage} asks about every cell on every ticker operation, and decoding Base64 plus a
 * {@code BukkitObjectInputStream} per cell per operation was the plugin's heaviest recurring cost.
 * {@link NodeStore#canonical(Block)} now hands out the live instance instead of a fresh decode.
 * <p>
 * The two properties that make that safe are what these tests pin:
 * <ul>
 *   <li>{@code get} still returns an independent copy, because half the codebase does
 *       get/mutate/put and relies on owning its object.</li>
 *   <li>Whoever writes becomes the live value, so a shared reader can never be holding something
 *       older than the last write — the failure mode that would turn into an item dupe.</li>
 * </ul>
 *
 * [ES] Tests de la caché de decodificación. {@code get} sigue devolviendo una copia independiente
 * (medio código hace get/mutar/put y cuenta con ser dueño de su objeto) y quien escribe pasa a ser el
 * valor vivo, de modo que un lector compartido nunca pueda estar sosteniendo algo más viejo que la
 * última escritura: el fallo que se convertiría en un duplicado de ítems.
 */
class NodeStoreCanonicalTest {

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

    private Block cell(int x) {
        Block block = world.getBlockAt(x, 64, 0);
        NodeStore.put(block, NodeBlob.create("MVN_CELL_T1"));
        return block;
    }

    @Test
    @DisplayName("canonical() hands out one shared instance instead of decoding every call")
    void canonicalIsDecodedOnce() {
        Block block = cell(0);

        NodeBlob first = NodeStore.canonical(block);
        NodeBlob second = NodeStore.canonical(block);

        assertSame(first, second, "the whole point: no re-decode between reads");
    }

    @Test
    @DisplayName("get() stays copy-on-read so callers keep owning their object")
    void getStillReturnsACopy() {
        Block block = cell(1);

        NodeBlob canonical = NodeStore.canonical(block);
        NodeBlob copy = NodeStore.get(block);

        assertNotSame(canonical, copy, "get() must not start handing out the shared instance");
        assertNotSame(NodeStore.get(block), NodeStore.get(block), "and not even to itself");
    }

    @Test
    @DisplayName("a write from outside the storage engine becomes the live value")
    void externalWritesReplaceTheLiveValue() {
        Block block = cell(2);
        assertEquals(0L, NodeStore.canonical(block).cellAmount);

        // Exactly the shape of GreedyCellTick: read a copy, mutate it, write it back.
        NodeBlob copy = NodeStore.get(block);
        copy.cellAmount = 4096;
        copy.cellSample = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND);
        NodeStore.put(block, copy);

        assertEquals(4096L, NodeStore.canonical(block).cellAmount,
                "a shared reader must see the newest write, not the object it decoded earlier");
        assertEquals(org.bukkit.Material.DIAMOND, NodeStore.canonical(block).cellSample.getType());
    }

    @Test
    @DisplayName("removing a node drops its cached blob")
    void removeDropsTheCache() {
        Block block = cell(3);
        assertSame(NodeStore.canonical(block), NodeStore.canonical(block));

        NodeStore.remove(block);

        assertNull(NodeStore.canonical(block), "a removed node has no blob to serve");
        assertNull(NodeStore.get(block));
    }

    @Test
    @DisplayName("the cache is dropped on init, so a reload cannot resurrect old state")
    void initDropsEverything() {
        Block block = cell(4);
        NodeBlob before = NodeStore.canonical(block);

        NodeStore.init(plugin);

        NodeBlob after = NodeStore.canonical(block);
        assertNotSame(before, after, "init() must clear the decode cache, not keep serving pre-reload objects");
        assertEquals(before.typeName, after.typeName);
    }
}

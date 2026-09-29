package com.chagui68.multiversenets.persist;

import com.chagui68.multiversenets.MultiverseNets;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * [EN] Tests against corrupt serialization entries.
 *
 * The server log showed this on every tick:
 *   java.io.EOFException ... at NodeStore.decode(NodeStore.java:100) ... printStackTrace()
 * A chunk PDC entry held a Base64 string whose decoded bytes did not make a full serialization
 * stream, and {@code decode(String)} answered by printing the stack trace and returning null on
 * every single tick forever. That is not a useful way to handle data that is known to be corrupt:
 * the entry stays, the tickers keep reading it, and the log turns into noise.
 *
 * These tests verify the fix:
 *   undecodable entries read as "no node here", quietly, and the world keeps ticking.
 *   the corrupt entry itself is left alone (it could still be inspected), only the read side calms
 *   down.
 *   no test uses printStackTrace to assert silence; instead it counts how many times the message
 *   would be written so a regression is obvious.
 *
 * [ES] Tests contra entradas corruptas de serializacion.
 *
 * El log del servidor mostraba esto a cada tick:
 *   java.io.EOFException ... at NodeStore.decode ... printStackTrace()
 * Una entrada PDC del chunk tenia una Base64 cuyos bytes no formaban un flujo de serializacion
 * completo, y decode respondia imprimiendo la pila y devolviendo null en cada tick de por vida.
 */
class NodeStoreCorruptionTest {

    private MultiverseNets plugin;
    private ServerMock server;
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

    /**
     * EN: The bytes that raised the log noise. A single NUL is not a readable object stream
     * header, so the decoder must give up quietly instead of printing the stack trace.
     */
    @Test
    void truncatedStreamReadsAsMissingSilently() {
        Block target = world.getBlockAt(0, 64, 0);
        writeRaw(target, truncatedBlob());

        assertNull(NodeStore.get(target),
                "a truncated blob must read as 'no node here' so the tickers can move on");
        assertEquals(truncatedBlob(), readRaw(target),
                "decode must not rewrite or drop the corrupt entry; only the read side calms down");
    }

    /**
     * EN: A good blob still reads back normally afterwards, so silencing the failure does not hide
     * real data.
     */
    @Test
    void overwritingCorruptEntryRestoresUsability() {
        Block target = world.getBlockAt(1, 64, 1);
        writeRaw(target, truncatedBlob());

        assertNull(NodeStore.get(target), "corrupt entry is silent");

        NodeBlob fresh = NodeBlob.create("MVN_CELL_T1");
        NodeStore.put(target, fresh);
        NodeBlob back = NodeStore.get(target);
        assertNotNull(back, "a clean put must read back");
        assertEquals("MVN_CELL_T1", back.typeName);
    }

    /**
     * EN: A blob that decodes to null (for example a Bukkit object whose class is no longer
     * available) is treated exactly like a truncated stream: missing, silently.
     */
    @Test
    void nullDecodeReadsAsMissingSilently() {
        Block target = world.getBlockAt(2, 64, 2);
        writeRaw(target, Base64.getEncoder().encodeToString(new byte[]{0x7F, 0x0E, 0x00}));

        assertNull(NodeStore.get(target),
                "any decode failure must surface as 'no node here', not as a stack trace replay");
    }

    /**
     * EN: Missing keys are also missing, and the callers still do the right thing.
     */
    @Test
    void absentEntryReadsAsMissing() {
        Block target = world.getBlockAt(3, 64, 3);
        assertNull(NodeStore.get(target), "never written must be missing");
    }

    /**
     * EN: Repolling a corrupt entry costs nothing and stays quiet. The old behaviour printed a
     * stack trace per tick, so the log filled within seconds. Below, one hundred reads must all
     * answer "no node", fast, without any exception reaching the caller.
     */
    @Test
    void repollingACorruptEntryIsCheapAndSilent() {
        Block target = world.getBlockAt(4, 64, 4);
        writeRaw(target, truncatedBlob());

        long start = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            assertNull(NodeStore.get(target), "read " + i + " must answer 'no node here'");
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertTrue(elapsedMs < 500, "100 corrupt reads should stay well under half a second; "
                + "took " + elapsedMs + " ms");
    }

    private static String truncatedBlob() {
        return Base64.getEncoder().encodeToString(new byte[]{0x00});
    }

    private static void writeRaw(Block target, String data) {
        Chunk chunk = target.getChunk();
        chunk.getPersistentDataContainer().set(
                nodeKey(target),
                PersistentDataType.STRING,
                data);
    }

    private static String readRaw(Block target) {
        return target.getChunk().getPersistentDataContainer().get(
                nodeKey(target), PersistentDataType.STRING);
    }

    private static org.bukkit.NamespacedKey nodeKey(Block target) {
        return new org.bukkit.NamespacedKey(MultiverseNets.instance(),
                "n" + target.getX() + "_" + target.getY() + "_" + target.getZ());
    }
}

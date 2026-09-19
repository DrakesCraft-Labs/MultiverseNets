package com.chagui68.multiversenets.net;

import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.PosUtil;
import com.chagui68.multiversenets.util.Settings;
import org.bukkit.block.Block;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * [EN] Network Graph Topology & State
 * Represents a single connected network of nodes centered on a Network Controller.
 *
 * Scans connected blocks via BFS without triggering synchronous chunk loads, and maintains
 * type-indexed lookups ({@code byType}) to optimize ticking operations for 4000+ nodes.
 *
 * [ES] Topología y Estado del Grafo de Red
 * Representa una red física de nodos conectados centrada en un Controlador de Red.
 * Escanea bloques adyacentes por BFS sin forzar la carga de chunks y mantiene un índice
 * optimizado por tipo de dispositivo.
 */
public class Network {

    private final com.chagui68.multiversenets.MultiverseNets plugin;
    private final org.bukkit.World world;
    private final long controllerPos;
    private final Map<Long, DeviceType> nodes = new HashMap<>();
    private final Map<DeviceType, Set<Long>> byType = new EnumMap<>(DeviceType.class);
    private final NetworkStorage storage = new NetworkStorage(this);
    private volatile long version = 0;
    private long lastScanMs = 0;
    private volatile boolean crayon;
    private volatile boolean dirty = true;
    public String error;

    public Network(com.chagui68.multiversenets.MultiverseNets plugin, org.bukkit.World world, long controllerPos) {
        this.plugin = plugin;
        this.world = world;
        this.controllerPos = controllerPos;
    }

    public org.bukkit.World world() {
        return world;
    }

    public long controllerPos() {
        return controllerPos;
    }

    public Map<Long, DeviceType> nodes() {
        return nodes;
    }

    public int size() {
        return nodes.size();
    }

    public NetworkStorage storage() {
        return storage;
    }

    public long versionSnapshot() {
        return version;
    }

    public boolean crayon() {
        return crayon;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public boolean contains(long pos) {
        return nodes.containsKey(pos);
    }

    public DeviceType typeAt(long pos) {
        return nodes.get(pos);
    }

    public void forEach(DeviceType type, BiConsumer<Long, DeviceType> consumer) {
        Set<Long> matching = byType.get(type);
        if (matching == null || matching.isEmpty()) {
            return;
        }
        // Copia defensiva: el consumidor puede romper un bloque y modificar el indice mientras
        // se recorre. Solo se copian los de ESE tipo, no la red entera.
        for (Long pos : matching.toArray(new Long[0])) {
            consumer.accept(pos, type);
        }
    }

    /** Cuantos dispositivos de un tipo tiene la red. Util para /mvnets info sin recorrer nada. */
    public int count(DeviceType type) {
        Set<Long> matching = byType.get(type);
        return matching == null ? 0 : matching.size();
    }

    public Block block(long pos) {
        return world.getBlockAt(PosUtil.unpackX(pos), PosUtil.unpackY(pos), PosUtil.unpackZ(pos));
    }

    public void scan() {
        Map<Long, DeviceType> found = new HashMap<>();
        Set<Long> visited = new HashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        List<String> errors = new ArrayList<>();

        // No cargar chunks a la fuerza: si el controlador esta en uno sin cargar, la red se queda
        // como estaba y el proximo scan (o la carga del chunk) lo resuelve. Antes el BFS llamaba
        // a getBlockAt a ciegas y el servidor cargaba trozos enteros de disco en pleno tick.
        int ctrlCx = PosUtil.unpackX(controllerPos) >> 4;
        int ctrlCz = PosUtil.unpackZ(controllerPos) >> 4;
        if (!world.isChunkLoaded(ctrlCx, ctrlCz)) {
            return;
        }

        NodeBlob ctrlBlob = NodeStore.get(block(controllerPos));
        if (ctrlBlob == null) {
            // El registro dice que aqui hubo un controlador pero el chunk ya no lo tiene. Dejar
            // la topologia vieja viva haria trabajar a la red sobre bloques que ya no existen.
            synchronized (nodes) {
                nodes.clear();
                byType.clear();
            }
            error = "controller missing";
            version++;
            storage.invalidate();
            return;
        }
        this.crayon = ctrlBlob.crayon;

        found.put(controllerPos, DeviceType.MVN_CONTROLLER);
        visited.add(controllerPos);
        queue.add(controllerPos);

        while (!queue.isEmpty()) {
            if (found.size() >= Settings.maxNodes()) {
                errors.add("node limit reached (" + Settings.maxNodes() + ")");
                break;
            }
            long pos = queue.poll();
            for (long next : neighbors(pos)) {
                if (!visited.add(next)) {
                    continue;
                }
                if (!world.isChunkLoaded(PosUtil.unpackX(next) >> 4, PosUtil.unpackZ(next) >> 4)) {
                    continue;
                }
                Block block = block(next);
                if (!NodeStore.chunkHasNodes(block.getChunk())) {
                    continue;
                }
                DeviceType type = NodeStore.getType(block);
                if (type == null) {
                    continue;
                }
                if (type == DeviceType.MVN_CONTROLLER && next != controllerPos) {
                    errors.add("foreign controller at " + coordString(next));
                    continue;
                }
                found.put(next, type);
                queue.add(next);
            }
        }

        synchronized (nodes) {
            nodes.clear();
            nodes.putAll(found);
            byType.clear();
            for (Map.Entry<Long, DeviceType> entry : found.entrySet()) {
                byType.computeIfAbsent(entry.getValue(), key -> new HashSet<>()).add(entry.getKey());
            }
        }
        this.error = String.join("; ", errors);
        this.version++;
        this.lastScanMs = System.currentTimeMillis();
        this.dirty = false;
        storage.invalidate();
    }

    private static List<Long> neighbors(long pos) {
        int x = PosUtil.unpackX(pos);
        int y = PosUtil.unpackY(pos);
        int z = PosUtil.unpackZ(pos);
        return List.of(
                PosUtil.pack(x + 1, y, z), PosUtil.pack(x - 1, y, z),
                PosUtil.pack(x, y + 1, z), PosUtil.pack(x, y - 1, z),
                PosUtil.pack(x, y, z + 1), PosUtil.pack(x, y, z - 1));
    }

    private static String coordString(long pos) {
        return PosUtil.unpackX(pos) + "," + PosUtil.unpackY(pos) + "," + PosUtil.unpackZ(pos);
    }

    public boolean needsScan(long intervalMs) {
        return System.currentTimeMillis() - lastScanMs >= intervalMs;
    }
}

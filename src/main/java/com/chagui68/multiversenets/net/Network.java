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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public class Network {

    private final com.chagui68.multiversenets.MultiverseNets plugin;
    private final org.bukkit.World world;
    private final long controllerPos;
    private final Map<Long, DeviceType> nodes = new HashMap<>();
    private final NetworkStorage storage = new NetworkStorage(this);
    private volatile long version = 0;
    private long lastScanMs = 0;
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

    public boolean contains(long pos) {
        return nodes.containsKey(pos);
    }

    public DeviceType typeAt(long pos) {
        return nodes.get(pos);
    }

    public void forEach(DeviceType type, BiConsumer<Long, DeviceType> consumer) {
        for (Map.Entry<Long, DeviceType> entry : nodes.entrySet()) {
            if (entry.getValue() == type) {
                consumer.accept(entry.getKey(), entry.getValue());
            }
        }
    }

    public Block block(long pos) {
        return world.getBlockAt(PosUtil.unpackX(pos), PosUtil.unpackY(pos), PosUtil.unpackZ(pos));
    }

    public void scan() {
        Map<Long, DeviceType> found = new HashMap<>();
        Set<Long> visited = new HashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        List<String> errors = new ArrayList<>();

        NodeBlob ctrlBlob = NodeStore.get(block(controllerPos));
        if (ctrlBlob == null) {
            error = "controller missing";
            return;
        }
        found.put(controllerPos, DeviceType.CONTROLLER);
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
                Block block = block(next);
                if (!NodeStore.chunkHasNodes(block.getChunk())) {
                    continue;
                }
                NodeBlob blob = NodeStore.get(block);
                if (blob == null) {
                    continue;
                }
                DeviceType type = DeviceType.parse(blob.typeName);
                if (type == null) {
                    continue;
                }
                if (type == DeviceType.CONTROLLER && next != controllerPos) {
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
        }
        this.error = String.join("; ", errors);
        this.version++;
        this.lastScanMs = System.currentTimeMillis();
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

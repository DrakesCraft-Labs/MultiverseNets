package com.chagui68.multiversenets.persist;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.util.Keys;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class NodeStore {

    private static MultiverseNets plugin;
    private static File registryFile;
    private static final Map<UUID, List<String>> CONTROLLERS = new HashMap<>();

    private NodeStore() {
    }

    public static void init(MultiverseNets pl) {
        plugin = pl;
        registryFile = new File(pl.getDataFolder(), "networks.yml");
        if (registryFile.isFile()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(registryFile);
            ConfigurationSection root = yaml.getConfigurationSection("controllers");
            if (root != null) {
                for (String worldId : root.getKeys(false)) {
                    CONTROLLERS.put(UUID.fromString(worldId), new ArrayList<>(root.getStringList(worldId)));
                }
            }
        }
    }

    public static void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, List<String>> entry : CONTROLLERS.entrySet()) {
            yaml.set("controllers." + entry.getKey(), entry.getValue());
        }
        try {
            yaml.save(registryFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save networks.yml: " + e.getMessage());
        }
    }

    public static String encode(NodeBlob blob) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             BukkitObjectOutputStream out = new BukkitObjectOutputStream(bytes)) {
            out.writeObject(blob);
            out.flush();
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Could not serialize node", e);
        }
    }

    public static NodeBlob decode(String data) {
        try (BukkitObjectInputStream in = new BukkitObjectInputStream(
                new ByteArrayInputStream(Base64.getDecoder().decode(data)))) {
            return (NodeBlob) in.readObject();
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            return null;
        }
    }

    public static NodeBlob get(Block block) {
        Chunk chunk = block.getChunk();
        if (!chunk.isLoaded()) {
            return null;
        }
        String data = chunk.getPersistentDataContainer()
                .get(nodeKey(block), PersistentDataType.STRING);
        return data == null ? null : decode(data);
    }

    public static void put(Block block, NodeBlob blob) {
        Chunk chunk = block.getChunk();
        chunk.getPersistentDataContainer().set(nodeKey(block), PersistentDataType.STRING, encode(blob));
        chunk.getPersistentDataContainer().set(Keys.CHUNK_HAS_NODES, PersistentDataType.BYTE, (byte) 1);
    }

    public static void remove(Block block) {
        block.getChunk().getPersistentDataContainer().remove(nodeKey(block));
    }

    public static boolean chunkHasNodes(Chunk chunk) {
        return chunk.isLoaded()
                && Byte.valueOf((byte) 1).equals(chunk.getPersistentDataContainer().get(Keys.CHUNK_HAS_NODES, PersistentDataType.BYTE));
    }

    private static org.bukkit.NamespacedKey nodeKey(Block block) {
        return new org.bukkit.NamespacedKey(plugin,
                "n" + block.getX() + "_" + block.getY() + "_" + block.getZ());
    }

    public static List<long[]> controllers(UUID worldId) {
        List<String> raw = CONTROLLERS.get(worldId);
        if (raw == null) {
            return List.of();
        }
        List<long[]> result = new ArrayList<>();
        for (String entry : raw) {
            String[] parts = entry.split(",");
            result.add(new long[]{Long.parseLong(parts[0]), Long.parseLong(parts[1]), Long.parseLong(parts[2])});
        }
        return result;
    }

    public static boolean isController(UUID worldId, int x, int y, int z) {
        List<String> raw = CONTROLLERS.get(worldId);
        if (raw == null) {
            return false;
        }
        return raw.contains(x + "," + y + "," + z);
    }

    public static void addController(UUID worldId, int x, int y, int z) {
        List<String> raw = CONTROLLERS.computeIfAbsent(worldId, k -> new ArrayList<>());
        String key = x + "," + y + "," + z;
        if (!raw.contains(key)) {
            raw.add(key);
            save();
        }
    }

    public static void removeController(UUID worldId, int x, int y, int z) {
        List<String> raw = CONTROLLERS.get(worldId);
        if (raw != null && raw.remove(x + "," + y + "," + z)) {
            save();
        }
    }
}

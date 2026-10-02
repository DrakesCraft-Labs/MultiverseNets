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

/**
 * [EN] Chunk Persistent Storage & Network Registry
 * Handles serialization/deserialization of {@link NodeBlob} data into chunk PDC containers,
 * and maintains the persistent controller position registry in {@code networks.yml}.
 *
 * [ES] Almacenamiento Persistente en Chunks y Registro de Redes
 * Gestiona la serialización y deserialización de objetos {@link NodeBlob} en los contenedores PDC de chunks,
 * y mantiene el registro de controladores persistente en {@code networks.yml}.
 */
public final class NodeStore {

    private static MultiverseNets plugin;
    private static File registryFile;
    private static final Map<UUID, List<String>> CONTROLLERS = new HashMap<>();

    /**
     * [EN] Live, shared instances of decoded node blobs, keyed by world+position.
     * <p>
     * {@link #get} stays copy-on-read on purpose — plenty of code does get/mutate/put and relies on
     * owning its own object. This map exists for the one caller that cannot afford that:
     * {@code NetworkStorage}, which asks about every cell on every deposit and withdrawal, i.e.
     * thousands of times per second. Deserialising Base64 + {@code BukkitObjectInputStream} there was
     * the plugin's heaviest recurring cost.
     * <p>
     * Correctness rule: the map is always replaced wholesale by whoever writes, so it can never hold
     * a value that is older than the last {@link #put} or {@link #remove}. A caller that mutates an
     * object it read with {@link #get} without calling {@link #put} never reaches this map, exactly
     * as before.
     *
     * [ES] Instancias vivas y compartidas de los blobs ya decodificados, indexadas por mundo+posición.
     * <p>
     * {@link #get} sigue devolviendo una copia a propósito: mucho código hace get/mutar/put y cuenta
     * con ser dueño de su objeto. Este mapa existe para el único consumidor que no puede permitírselo:
     * {@code NetworkStorage}, que pregunta por todas las celdas en cada depósito y retirada, es decir
     * miles de veces por segundo. Deserializar Base64 + {@code BukkitObjectInputStream} ahí era el
     * gasto recurrente más alto del plugin.
     * <p>
     * Regla de corrección: quien escribe reemplaza la entrada entera, así que el mapa nunca puede
     * contener un valor más viejo que el último {@link #put} o {@link #remove}.
     */
    private static final Map<String, NodeBlob> CANONICAL = new java.util.concurrent.ConcurrentHashMap<>();
    /** Beyond this the map is dropped outright; a re-decode spike beats unbounded growth. */
    private static final int CANONICAL_LIMIT = 8192;

    private NodeStore() {
    }

    /**
     * EN: Initializes the controller registry file and loads saved network coordinates.
 *
     * ES: Inicializa el archivo de registro de controladores y carga las coordenadas guardadas.
     */
    public static void init(MultiverseNets pl) {
        plugin = pl;
        CANONICAL.clear();
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
        synchronized (CONTROLLERS) {
            for (Map.Entry<UUID, List<String>> entry : CONTROLLERS.entrySet()) {
                yaml.set("controllers." + entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        }
        if (plugin != null && plugin.isEnabled() && plugin.getServer() != null && plugin.getServer().isPrimaryThread()) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    yaml.save(registryFile);
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not save networks.yml: " + e.getMessage());
                }
            });
        } else {
            try {
                yaml.save(registryFile);
            } catch (IOException e) {
                if (plugin != null) {
                    plugin.getLogger().severe("Could not save networks.yml: " + e.getMessage());
                }
            }
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
            NodeBlob blob = (NodeBlob) in.readObject();
            normalize(blob);
            return blob;
        } catch (Throwable e) {
            // A truncated or misplaced stream means the chunk PDC entry got damaged (a crash
            // mid-write will do that). The block is not a node anymore, so the answer is null,
            // and the world moves on. printStackTrace here would spam the console on every tick
            // for the rest of the server session.
            if (plugin != null && com.chagui68.multiversenets.util.Settings.debug()) {
                plugin.getLogger().warning("Could not deserialize node data; the corrupt entry "
                        + "is still in place and can be inspected. " + e.getClass().getSimpleName()
                        + ": " + e.getMessage());
            }
            return null;
        }
    }

    /**
     * Los campos anadidos despues de la primera version se deserializan a null en los blobs
     * viejos (la deserializacion de Java no ejecuta constructores). Aqui vuelven a su default.
     */
    private static void normalize(NodeBlob blob) {
        if (blob == null) {
            return;
        }
        if (blob.filterMaterials == null) {
            blob.filterMaterials = new ArrayList<>();
        }
        if (blob.filterItems == null) {
            blob.filterItems = new ArrayList<>();
        }
        if (blob.recipes == null) {
            blob.recipes = new ArrayList<>();
        }
        if (blob.blueprintData == null) {
            blob.blueprintData = new ArrayList<>();
        }
        if (blob.craftingMatrix == null) {
            blob.craftingMatrix = new org.bukkit.inventory.ItemStack[9];
        }
        if (blob.greedySamples == null) {
            blob.greedySamples = new ArrayList<>();
        }
        if (blob.greedyAmounts == null) {
            blob.greedyAmounts = new ArrayList<>();
        }
        if (blob.virtualSamples == null) {
            blob.virtualSamples = new ArrayList<>();
        }
        if (blob.virtualAmounts == null) {
            blob.virtualAmounts = new ArrayList<>();
        }
        if (blob.recoveredModules == null) {
            blob.recoveredModules = new ArrayList<>();
        }
        if (blob.dramFluids == null) {
            blob.dramFluids = new ArrayList<>();
        }
        if (blob.dramFluidAmounts == null) {
            blob.dramFluidAmounts = new ArrayList<>();
        }
        if (blob.chickenProducts == null) {
            blob.chickenProducts = new ArrayList<>();
        }
        if (("GREEDY_CELL".equals(blob.typeName) || "MVN_GREEDY_CELL".equals(blob.typeName))
                && blob.cellSample != null && blob.cellAmount > 0) {
            if (blob.greedySamples.isEmpty()) {
                blob.greedySamples.add(blob.cellSample);
                blob.greedyAmounts.add(blob.cellAmount);
                blob.cellSample = null;
                blob.cellAmount = 0;
            }
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

    /**
     * [EN] The shared live instance of this node's blob, decoded once and reused. Only safe for
     * callers that treat the object as the authoritative in-memory state and always write it back
     * with {@link #put}; everybody else wants {@link #get}.
     *
     * [ES] La instancia compartida y viva del blob de este nodo, decodificada una vez y reutilizada.
     * Solo es segura para quien trata el objeto como estado autoritativo en memoria y siempre lo
     * reescribe con {@link #put}; todos los demás quieren {@link #get}.
     */
    public static NodeBlob canonical(Block block) {
        Chunk chunk = block.getChunk();
        if (!chunk.isLoaded()) {
            return null;
        }
        String key = canonicalKey(block);
        NodeBlob hit = CANONICAL.get(key);
        if (hit != null) {
            return hit;
        }
        String data = chunk.getPersistentDataContainer()
                .get(nodeKey(block), PersistentDataType.STRING);
        if (data == null) {
            return null;
        }
        NodeBlob blob = decode(data);
        if (blob != null) {
            publish(key, blob);
        }
        return blob;
    }

    private static void publish(String key, NodeBlob blob) {
        if (CANONICAL.size() >= CANONICAL_LIMIT) {
            CANONICAL.clear();
        }
        CANONICAL.put(key, blob);
    }

    private static String canonicalKey(Block block) {
        return block.getWorld().getUID() + "|" + block.getX() + "_" + block.getY() + "_" + block.getZ();
    }

    /**
     * EN: Retrieves the DeviceType directly without deserializing the entire NodeBlob.
     *
     * ES: Obtiene el DeviceType directamente sin deserializar el NodeBlob completo.
     */
    public static com.chagui68.multiversenets.item.DeviceType getType(Block block) {
        Chunk chunk = block.getChunk();
        if (!chunk.isLoaded()) {
            return null;
        }
        var pdc = chunk.getPersistentDataContainer();
        String typeName = pdc.get(nodeTypeKey(block), PersistentDataType.STRING);
        if (typeName != null) {
            return com.chagui68.multiversenets.item.DeviceType.parse(typeName);
        }
        // Fallback para nodos guardados antes del tipado rápido: deserializa y auto-repara el tag
        String data = pdc.get(nodeKey(block), PersistentDataType.STRING);
        if (data == null) {
            return null;
        }
        NodeBlob blob = decode(data);
        if (blob == null || blob.typeName == null) {
            return null;
        }
        pdc.set(nodeTypeKey(block), PersistentDataType.STRING, blob.typeName);
        return com.chagui68.multiversenets.item.DeviceType.parse(blob.typeName);
    }

    /**
     * EN: Checks if a block is registered as a network node without deserializing.
     *
     * ES: Comprueba si un bloque está registrado como nodo de red sin deserializar.
     */
    public static boolean hasNode(Block block) {
        Chunk chunk = block.getChunk();
        if (!chunk.isLoaded()) {
            return false;
        }
        var pdc = chunk.getPersistentDataContainer();
        return pdc.has(nodeTypeKey(block), PersistentDataType.STRING)
                || pdc.has(nodeKey(block), PersistentDataType.STRING);
    }

    public static void put(Block block, NodeBlob blob) {
        Chunk chunk = block.getChunk();
        var pdc = chunk.getPersistentDataContainer();
        pdc.set(nodeKey(block), PersistentDataType.STRING, encode(blob));
        if (blob != null && blob.typeName != null) {
            pdc.set(nodeTypeKey(block), PersistentDataType.STRING, blob.typeName);
        }
        pdc.set(Keys.CHUNK_HAS_NODES, PersistentDataType.BYTE, (byte) 1);
        if (blob != null) {
            // Whoever writes becomes the live value, so a shared reader can never be holding
            // something older than the last write. See CANONICAL.
            publish(canonicalKey(block), blob);
        }
    }

    public static void remove(Block block) {
        Chunk chunk = block.getChunk();
        var pdc = chunk.getPersistentDataContainer();
        pdc.remove(nodeKey(block));
        pdc.remove(nodeTypeKey(block));
        CANONICAL.remove(canonicalKey(block));
    }

    public static int countNodesInChunk(Chunk chunk) {
        if (!chunk.isLoaded()) {
            return 0;
        }
        var pdc = chunk.getPersistentDataContainer();
        int count = 0;
        for (org.bukkit.NamespacedKey key : pdc.getKeys()) {
            if (plugin != null && key.getNamespace().equalsIgnoreCase(plugin.getName())
                    && key.getKey().startsWith("t")) {
                count++;
            }
        }
        return count;
    }

    public static boolean chunkHasNodes(Chunk chunk) {
        return chunk.isLoaded()
                && Byte.valueOf((byte) 1).equals(chunk.getPersistentDataContainer().get(Keys.CHUNK_HAS_NODES, PersistentDataType.BYTE));
    }

    private static org.bukkit.NamespacedKey nodeKey(Block block) {
        return new org.bukkit.NamespacedKey(plugin,
                "n" + block.getX() + "_" + block.getY() + "_" + block.getZ());
    }

    private static org.bukkit.NamespacedKey nodeTypeKey(Block block) {
        return new org.bukkit.NamespacedKey(plugin,
                "t" + block.getX() + "_" + block.getY() + "_" + block.getZ());
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

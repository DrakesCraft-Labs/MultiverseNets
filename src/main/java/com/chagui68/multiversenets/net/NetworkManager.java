package com.chagui68.multiversenets.net;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.compat.SlimefunBridge;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.PosUtil;
import com.chagui68.multiversenets.util.StackUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * [EN] Multiverse Network Registry & Manager
 * Central registry tracking active networks across multiple worlds by controller location.
 * Provides helper predicates for node item filtering and inventory transfers.
 *
 * [ES] Gestor y Registro Multiverso de Redes
 * Registro central que rastrea las redes activas en múltiples mundos según la ubicación de su controlador.
 * Proporciona predicados de filtrado de ítems y utilidades de transferencia de inventario.
 */
public class NetworkManager {

    private final MultiverseNets plugin;
    private final Map<UUID, Map<Long, Network>> networksByWorld = new HashMap<>();

    public NetworkManager(MultiverseNets plugin) {
        this.plugin = plugin;
    }

    /**
     * EN: Loads all saved controller positions from disk into memory.
 *
     * ES: Carga todas las posiciones de controladores guardadas en disco.
     */
    public void load() {
        for (org.bukkit.World world : plugin.getServer().getWorlds()) {
            for (long[] ctrl : NodeStore.controllers(world.getUID())) {
                long pos = PosUtil.pack((int) ctrl[0], (int) ctrl[1], (int) ctrl[2]);
                networkFor(world, pos);
            }
        }
    }

    /**
     * EN: Saves all persistent network controller data to disk.
 *
     * ES: Guarda todos los datos de controladores persistentes en disco.
     */
    public void saveAll() {
        NodeStore.save();
    }

    /**
     * EN: Retrieves or creates the Network instance for a specific controller position.
 *
     * ES: Obtiene o crea la instancia de Network para una posición de controlador específica.
     */
    public Network networkFor(org.bukkit.World world, long controllerPos) {
        return networksByWorld
                .computeIfAbsent(world.getUID(), k -> new HashMap<>())
                .computeIfAbsent(controllerPos, p -> {
                    Network net = new Network(plugin, world, p);
                    net.scan();
                    return net;
                });
    }

    /**
     * EN: Registers a new network controller block.
 *
     * ES: Registra un nuevo bloque controlador de red.
     */
    public void registerController(Block block) {
        networkFor(block.getWorld(), PosUtil.pack(block.getX(), block.getY(), block.getZ()));
        NodeStore.addController(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }

    /**
     * EN: Unregisters and removes a network controller block.
 *
     * ES: Desregistra y elimina un bloque controlador de red.
     */
    public void removeController(Block block) {
        UUID worldId = block.getWorld().getUID();
        Map<Long, Network> nets = networksByWorld.get(worldId);
        if (nets != null) {
            nets.remove(PosUtil.pack(block.getX(), block.getY(), block.getZ()));
        }
        NodeStore.removeController(worldId, block.getX(), block.getY(), block.getZ());
    }

    /**
     * EN: Finds the network containing the given block position.
 *
     * ES: Encuentra la red que contiene la posición del bloque dado.
     */
    public Network networkAt(Block block) {
        UUID worldId = block.getWorld().getUID();
        Map<Long, Network> nets = networksByWorld.get(worldId);
        if (nets == null) {
            return null;
        }
        long pos = PosUtil.pack(block.getX(), block.getY(), block.getZ());
        for (Network net : nets.values()) {
            if (net.contains(pos)) {
                return net;
            }
        }
        return null;
    }

    /**
     * EN: Finds the network whose controller is at the given Location.
 *
     * ES: Encuentra la red cuyo controlador está en la ubicación dada.
     */
    public Network networkByController(Location loc) {
        UUID worldId = loc.getWorld().getUID();
        Map<Long, Network> nets = networksByWorld.get(worldId);
        if (nets == null) {
            return null;
        }
        return nets.get(PosUtil.pack(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
    }

    /**
     * EN: Rescans networks touching the given block or any of its adjacent neighbors.
 *
     * ES: Reescanea las redes que tocan al bloque o a cualquiera de sus vecinos adyacentes.
     */
    public void invalidateNear(Block block) {
        Set<Network> touched = new HashSet<>();
        Network own = networkAt(block);
        if (own != null) {
            touched.add(own);
        }
        for (BlockFace face : new BlockFace[]{
                BlockFace.NORTH, BlockFace.SOUTH,
                BlockFace.EAST, BlockFace.WEST,
                BlockFace.UP, BlockFace.DOWN}) {
            Network neighbor = networkAt(block.getRelative(face));
            if (neighbor != null) {
                touched.add(neighbor);
            }
        }
        for (Network net : touched) {
            net.markDirty();
            net.scan();
        }
    }

    /**
     * EN: Returns a list of all active networks across all worlds.
 *
     * ES: Devuelve una lista de todas las redes activas en todos los mundos.
     */
    public List<Network> all() {
        List<Network> all = new ArrayList<>();
        for (Map<Long, Network> nets : networksByWorld.values()) {
            all.addAll(nets.values());
        }
        return all;
    }

    /**
     * EN: Triggers an immediate topology rescan on all active networks.
 *
     * ES: Provoca un reescaneo de topología inmediato en todas las redes activas.
     */
    public void rescanAll() {
        for (Network net : all()) {
            net.scan();
        }
    }

    /**
     * EN: Returns the total number of connected nodes across all active networks.
 *
     * ES: Devuelve el número total de nodos conectados en todas las redes activas.
     */
    public int totalNodes() {
        int total = 0;
        for (Network net : all()) {
            total += net.size();
        }
        return total;
    }

    /**
     * EN: Creates an item filter predicate from a node's configuration blob.
     * Recognizes vanilla items, MultiverseNets DeviceTypes, and Slimefun IDs.
     * Supports Whitelist (default) and Blacklist modes.
 *
     * ES: Crea un predicado de filtrado de ítems a partir de la configuración del blob.
     * Reconoce ítems vanilla, DeviceTypes de MultiverseNets e IDs de Slimefun.
     * Soporta modos Whitelist (por defecto) y Blacklist.
     */
    public static Predicate<ItemStack> filterPredicate(NodeBlob blob) {
        if (blob == null) {
            return item -> true;
        }
        boolean hasItems = blob.filterItems != null && !blob.filterItems.isEmpty();
        boolean hasMats = blob.filterMaterials != null && !blob.filterMaterials.isEmpty();

        if (!hasItems && !hasMats) {
            return item -> true;
        }

        return item -> {
            if (item == null || item.getType().isAir()) {
                return false;
            }
            boolean matched = false;

            if (hasItems) {
                for (ItemStack filterTemplate : blob.filterItems) {
                    if (filterTemplate != null && !filterTemplate.getType().isAir() && matchesFilter(filterTemplate, item)) {
                        matched = true;
                        break;
                    }
                }
            } else {
                for (String entry : blob.filterMaterials) {
                    if (matchesMaterialOrId(entry, item)) {
                        matched = true;
                        break;
                    }
                }
            }

            return blob.filterBlacklist != matched;
        };
    }

    /**
     * EN: Checks if a candidate item matches a filter template item.
 *
     * ES: Comprueba si un ítem candidato coincide con la plantilla de filtro.
     */
    public static boolean matchesFilter(ItemStack filterTemplate, ItemStack candidate) {
        if (filterTemplate == null || candidate == null) {
            return filterTemplate == candidate;
        }
        // 1) DeviceType check
        DeviceType ftType = Items.typeOf(filterTemplate);
        DeviceType cdType = Items.typeOf(candidate);
        if (ftType != null || cdType != null) {
            return ftType == cdType;
        }

        // 2) Slimefun ID check
        String ftSf = SlimefunBridge.getId(filterTemplate);
        String cdSf = SlimefunBridge.getId(candidate);
        if (ftSf != null || cdSf != null) {
            return java.util.Objects.equals(ftSf, cdSf);
        }

        // 3) Custom meta / display name check
        if (filterTemplate.hasItemMeta() && filterTemplate.getItemMeta().hasDisplayName()) {
            return StackUtils.itemsMatch(filterTemplate, candidate, false);
        }

        // 4) Vanilla standard item check
        return candidate.getType() == filterTemplate.getType() && cdType == null && cdSf == null;
    }

    private static boolean matchesMaterialOrId(String entry, ItemStack candidate) {
        if (entry == null || candidate == null) {
            return false;
        }
        if (entry.startsWith("MULTIVERSENETS:")) {
            String devName = entry.substring("MULTIVERSENETS:".length());
            DeviceType candType = Items.typeOf(candidate);
            return candType != null && candType.name().equalsIgnoreCase(devName);
        }
        if (entry.startsWith("SLIMEFUN:")) {
            String sfId = entry.substring("SLIMEFUN:".length());
            return sfId.equalsIgnoreCase(SlimefunBridge.getId(candidate));
        }
        Material mat = Material.matchMaterial(entry);
        if (mat != null) {
            return candidate.getType() == mat && Items.typeOf(candidate) == null && !SlimefunBridge.isSlimefunItem(candidate);
        }
        return false;
    }

    /**
     * EN: Extracts the first matching item from an inventory up to {@code max} units.
 *
     * ES: Extrae el primer ítem coincidente de un inventario hasta {@code max} unidades.
     */
    public static ItemStack extractFirst(Inventory inv, Predicate<ItemStack> pred, int max) {
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || !pred.test(it)) {
                continue;
            }
            int take = Math.min(it.getAmount(), max);
            ItemStack out = it.clone();
            out.setAmount(take);
            if (take >= it.getAmount()) {
                inv.setItem(i, null);
            } else {
                it.setAmount(it.getAmount() - take);
            }
            return out;
        }
        return null;
    }

    /**
     * EN: Inserts an ItemStack into a vanilla inventory, returning leftover amount.
 *
     * ES: Inserta un ItemStack en un inventario vanilla y devuelve la cantidad sobrante.
     */
    public static int insertInto(Inventory inv, ItemStack stack) {
        Map<Integer, ItemStack> overflow = inv.addItem(stack);
        int left = 0;
        for (ItemStack over : overflow.values()) {
            left += over.getAmount();
        }
        return left;
    }
}

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
import org.bukkit.inventory.FurnaceInventory;
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

    public static boolean matchesMaterialOrId(String entry, ItemStack candidate) {
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
     * EN: Extracts up to {@code max} units of a single matching item from an inventory, merging
     * every slot that holds an equivalent stack. Stops early once the quota is reached.
     * Slots holding a different item are left untouched, so a filter that allows several items
     * still only drains one of them per call.
     *
     * ES: Extrae hasta {@code max} unidades de un mismo ítem coincidente de un inventario,
     * fusionando todas las ranuras que tengan un stack equivalente. Se detiene al alcanzar la
     * cuota. Las ranuras con otro ítem no se tocan, así que un filtro que permita varios ítems
     * sigue vaciando solo uno de ellos por llamada.
     */
    public static ItemStack extractMatching(Inventory inv, Predicate<ItemStack> pred, int max) {
        if (inv == null || max <= 0) {
            return null;
        }
        ItemStack result = null;
        int got = 0;
        for (int i : extractSlots(inv)) {
            if (got >= max) {
                break;
            }
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType().isAir() || !pred.test(it)) {
                continue;
            }
            if (result == null) {
                result = it.clone();
            } else if (!StackUtils.itemsMatch(result, it)) {
                continue;
            }
            int take = Math.min(it.getAmount(), max - got);
            int left = it.getAmount() - take;
            if (left <= 0) {
                inv.setItem(i, null);
            } else {
                it.setAmount(left);
                inv.setItem(i, it);
            }
            got += take;
        }
        if (result == null || got <= 0) {
            return null;
        }
        result.setAmount(got);
        return result;
    }

    /**
     * EN: Inserts an ItemStack into a vanilla inventory, returning leftover amount.
 *
     * ES: Inserta un ItemStack en un inventario vanilla y devuelve la cantidad sobrante.
     */
    public static int insertInto(Inventory inv, ItemStack stack) {
        if (stack == null || stack.getType().isAir() || stack.getAmount() <= 0) {
            return 0;
        }
        if (inv == null) {
            return stack.getAmount();
        }
        // Por trozos de un stack como maximo. Un pusher saca 128 (o 1.024) unidades de golpe, y
        // addItem con mas de un stack podia dejar en una ranura mas de lo que el item admite:
        // el servidor recortaba la ranura y el exceso desaparecia de la red.
        int perStack = maxPerSlot(inv, stack);
        int remaining = stack.getAmount();
        while (remaining > 0) {
            int chunk = Math.min(perStack, remaining);
            ItemStack part = stack.clone();
            part.setAmount(chunk);
            int left = 0;
            for (ItemStack over : inv.addItem(part).values()) {
                left += over.getAmount();
            }
            remaining -= chunk - left;
            if (left > 0) {
                break;
            }
        }
        return remaining;
    }

    /**
     * EN: Inserts {@code amount} units of {@code sample} the way a Pusher should: never more than
     * one stack per slot, never into a furnace's result slot, and, when {@code kinds} &gt; 1, never
     * letting one item type take more than its share of the slots. A whitelist of three
     * ingredients into a 9-slot dispenser leaves three slots for each, so the first ingredient
     * cannot flood the machine and lock the recipe out. {@code face} is the direction from the
     * pusher to this inventory; a furnace fed from above gets everything in its input slot, from
     * any other side fuel goes to the fuel slot.
     *
     * ES: Inserta {@code amount} unidades de {@code sample} como debe hacerlo un Pusher: nunca más
     * de un stack por ranura, nunca en la ranura de resultado de un horno y, con {@code kinds} &gt; 1,
     * sin que un tipo de ítem ocupe más que su parte de las ranuras. Una whitelist de tres
     * ingredientes hacia un dispensador de 9 ranuras deja tres para cada uno, así el primero no
     * llena la máquina y bloquea la receta. {@code face} es la dirección del pusher hacia este
     * inventario: un horno alimentado desde arriba lo recibe todo en la entrada; desde otro lado el
     * combustible va a su ranura.
     *
     * @return units that did not fit / unidades que no cupieron
     */
    public static int insertSmart(Inventory inv, ItemStack sample, int amount, BlockFace face, int kinds) {
        if (sample == null || sample.getType().isAir() || amount <= 0) {
            return 0;
        }
        if (inv == null) {
            return amount;
        }
        int[] slots = insertSlots(inv, sample, face);
        if (slots.length == 0) {
            return amount;
        }
        int perSlot = maxPerSlot(inv, sample);
        int share = kinds > 1 ? Math.max(1, slots.length / kinds) : Integer.MAX_VALUE;
        int remaining = amount;
        int occupied = 0;
        // 1) Completar stacks que ya son de este item.
        for (int slot : slots) {
            ItemStack current = inv.getItem(slot);
            if (current == null || current.getType().isAir() || !StackUtils.itemsMatch(current, sample)) {
                continue;
            }
            occupied++;
            int room = perSlot - current.getAmount();
            if (room <= 0 || remaining <= 0) {
                continue;
            }
            int add = Math.min(room, remaining);
            current.setAmount(current.getAmount() + add);
            inv.setItem(slot, current);
            remaining -= add;
        }
        // 2) Ranuras vacias, sin pasar de la parte que le toca a este tipo.
        for (int slot : slots) {
            if (remaining <= 0 || occupied >= share) {
                break;
            }
            ItemStack current = inv.getItem(slot);
            if (current != null && !current.getType().isAir()) {
                continue;
            }
            int add = Math.min(perSlot, remaining);
            inv.setItem(slot, StackUtils.getAsQuantity(sample, add));
            occupied++;
            remaining -= add;
        }
        return remaining;
    }

    private static int maxPerSlot(Inventory inv, ItemStack sample) {
        return Math.max(1, Math.min(sample.getMaxStackSize(), inv.getMaxStackSize()));
    }

    private static int[] allSlots(Inventory inv) {
        int size = inv.getStorageContents().length;
        int[] slots = new int[size];
        for (int i = 0; i < size; i++) {
            slots[i] = i;
        }
        return slots;
    }

    /** Ranuras donde un Pusher puede dejar este item. Hornos: entrada o combustible, nunca resultado. */
    static int[] insertSlots(Inventory inv, ItemStack sample, BlockFace face) {
        if (inv instanceof FurnaceInventory) {
            if (face != BlockFace.DOWN && sample.getType().isFuel()) {
                return new int[]{1};
            }
            return new int[]{0};
        }
        return allSlots(inv);
    }

    /** Ranuras de las que un Grabber puede sacar. De un horno, solo el resultado. */
    static int[] extractSlots(Inventory inv) {
        if (inv instanceof FurnaceInventory) {
            return new int[]{2};
        }
        int size = inv.getSize();
        int[] slots = new int[size];
        for (int i = 0; i < size; i++) {
            slots[i] = i;
        }
        return slots;
    }
}

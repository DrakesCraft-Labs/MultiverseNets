package com.chagui68.multiversenets.net;

import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.Settings;
import com.chagui68.multiversenets.util.StackUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * [EN] Network Storage Engine
 * Aggregated virtual storage summing all Quantum Cells (T1-T6), Greedy Cells, and Infinity Barrels.
 * - Insertion priority: Greedy cells matching type -> Normal cells matching type -> Empty cells.
 * - Extraction priority: Normal cells first -> Greedy cells last (acting as output buffers).
 * - Linear deduplication: Aggregated view is merged using {@link StackUtils#itemsMatch} to avoid Bukkit ItemStack hash bugs.
 *
 * [ES] Motor de Almacenamiento Agregado de Red
 * Almacenamiento virtual agregado que suma todas las celdas (T1-T6), celdas greedy y barriles infinitos.
 * - Prioridad de inserción: Greedy cells con muestra -> Celdas normales con muestra -> Celdas vacías.
 * - Prioridad de extracción: Celdas normales primero -> Greedy cells al final.
 * - Deduplicación lineal: Fusión de vista mediante {@link StackUtils#itemsMatch} para evitar fallos de hash.
 */
public class NetworkStorage {

    private static final long VIEW_CACHE_MS = 500;

    /**
     * EN: Consolidated view entry for an item sample and its total network count.
 *
     * ES: Entrada de vista consolidada para una muestra de ítem y su conteo total en la red.
     */
    public record View(ItemStack sample, long amount) {
    }

    private record CellRef(long pos, int tier, boolean greedy, boolean barrel) {
    }

    /** Una celda con su blob ya decodificado para una operacion concreta. */
    private static final class CellState {
        private final long pos;
        private final boolean greedy;
        private final boolean barrel;
        private final Block block;
        private final NodeBlob blob;
        private final long capacity;
        private boolean dirty;

        private CellState(long pos, boolean greedy, boolean barrel, Block block, NodeBlob blob, long capacity) {
            this.pos = pos;
            this.greedy = greedy;
            this.barrel = barrel;
            this.block = block;
            this.blob = blob;
            this.capacity = capacity;
        }
    }

    private final Network network;
    private final List<CellRef> cells = new ArrayList<>();
    private long boundVersion = -1;
    private List<View> viewCache;
    private long viewCacheAt;

    public NetworkStorage(Network network) {
        this.network = network;
    }

    public void invalidate() {
        boundVersion = -1;
        viewCache = null;
    }

    private void sync() {
        long current = network.versionSnapshot();
        if (boundVersion == current) {
            return;
        }
        cells.clear();
        synchronized (network.nodes()) {
            for (var entry : network.nodes().entrySet()) {
                DeviceType type = entry.getValue();
                if (type.isCell()) {
                    cells.add(new CellRef(entry.getKey(), type.cellTier(), false, false));
                } else if (type == DeviceType.MVN_GREEDY_CELL) {
                    cells.add(new CellRef(entry.getKey(), 0, true, false));
                } else if (type == DeviceType.MVN_INFINITY_BARREL) {
                    cells.add(new CellRef(entry.getKey(), 0, false, true));
                }
            }
        }
        boundVersion = current;
        viewCache = null;
    }

    /** Las celdas con su blob decodificado, en el orden del indice. Salta las ilegibles. */
    private List<CellState> load() {
        sync();
        List<CellState> states = new ArrayList<>(cells.size());
        for (CellRef ref : cells) {
            // Si el chunk se descargo despues del scan, la celda se salta en vez de forzar una
            // carga sincrona desde el ticker. Su contenido no desaparece: vuelve en cuanto el
            // chunk cargue y el scan la redescubra.
            int cx = com.chagui68.multiversenets.util.PosUtil.unpackX(ref.pos()) >> 4;
            int cz = com.chagui68.multiversenets.util.PosUtil.unpackZ(ref.pos()) >> 4;
            if (!network.world().isChunkLoaded(cx, cz)) {
                continue;
            }
            Block block = network.block(ref.pos());
            NodeBlob blob = NodeStore.get(block);
            if (blob == null) {
                continue;
            }
            DeviceType real = DeviceType.parse(blob.typeName);
            boolean stillValid;
            long cap;
            if (ref.greedy()) {
                stillValid = real == DeviceType.MVN_GREEDY_CELL;
                cap = Settings.greedyCapacity();
            } else if (ref.barrel()) {
                stillValid = real == DeviceType.MVN_INFINITY_BARREL;
                cap = Settings.barrelCapacity();
            } else {
                stillValid = real != null && real.isCell();
                cap = Settings.cellCapacity(ref.tier());
            }
            if (!stillValid) {
                continue;
            }
            states.add(new CellState(ref.pos(), ref.greedy(), ref.barrel(), block, blob, cap));
        }
        return states;
    }

    private void flush(List<CellState> states) {
        boolean anyDirty = false;
        for (CellState state : states) {
            if (state.dirty) {
                NodeStore.put(state.block, state.blob);
                anyDirty = true;
            }
        }
        if (anyDirty) {
            viewCache = null;
        }
    }

    /**
     * Mete un item en la red. Devuelve cuantas unidades NO entraron (0 = todo dentro).
     *
     * No muta el stack recibido; el llamante decide que hacer con el sobrante restandolo el
     * mismo, como hace addItemStack0 en NetworksV6.
     */
    public int deposit(ItemStack item) {
        if (item == null || item.getType().isAir() || item.getAmount() <= 0) {
            return 0;
        }
        List<CellState> states = load();
        long remaining = item.getAmount();

        // 1) Greedy cells: preferred network sink if they already store this item or if their filter accepts it.
        // Shared capacity (Option B): blob.totalGreedyAmount() <= state.capacity.
        for (CellState state : states) {
            if (!state.greedy) {
                continue;
            }
            long space = state.capacity - state.blob.totalGreedyAmount();
            if (space <= 0) {
                continue;
            }
            boolean matchesExisting = state.blob.indexOfGreedySample(item) >= 0;
            boolean matchesFilter = (state.blob.filterMaterials != null && !state.blob.filterMaterials.isEmpty())
                    || (state.blob.filterItems != null && !state.blob.filterItems.isEmpty());
            if (matchesExisting || (matchesFilter && NetworkManager.filterPredicate(state.blob).test(item))) {
                long take = Math.min(space, remaining);
                state.blob.addGreedyItem(item, take);
                state.dirty = true;
                remaining -= take;
                if (remaining <= 0) {
                    break;
                }
            }
        }
        // 2) celdas normales con el mismo tipo.
        if (remaining > 0) {
            for (CellState state : states) {
                if (state.greedy || state.blob.cellSample == null
                        || !StackUtils.itemsMatch(state.blob.cellSample, item)) {
                    continue;
                }
                remaining = pour(state, item, remaining);
                if (remaining <= 0) {
                    break;
                }
            }
        }
        // 3) celdas vacias: adoptan el tipo entrante (una greedy vacia NO se auto-asigna aqui;
        //    su tipo lo fija ella misma tirando de su filtro, que es para lo que existe).
        if (remaining > 0) {
            for (CellState state : states) {
                if (state.greedy || state.blob.cellSample != null) {
                    continue;
                }
                state.blob.cellSample = StackUtils.getAsQuantity(item, 1);
                remaining = pour(state, item, remaining);
                if (remaining <= 0) {
                    break;
                }
            }
        }
        flush(states);
        return (int) remaining;
    }

    private static long pour(CellState state, ItemStack item, long remaining) {
        long space = state.capacity - state.blob.cellAmount;
        if (space <= 0) {
            return remaining;
        }
        long take = Math.min(space, remaining);
        state.blob.cellAmount += take;
        state.dirty = true;
        return remaining - take;
    }

    public int depositAll(List<ItemStack> items) {
        int leftover = 0;
        for (ItemStack item : items) {
            leftover += deposit(item);
        }
        return leftover;
    }

    /**
     * Saca hasta {@code want} unidades que cumplan el matcher, combinando celdas: normales
     * primero y greedy al final, que es su papel de "buffer de salida".
     *
     * @return el stack con lo obtenido, o null si no habia nada
     */
    public ItemStack withdraw(Predicate<ItemStack> matcher, int want) {
        return withdraw(matcher, want, -1L);
    }

    /**
     * Variante con una celda excluida: la greedy cell se auto-surtiria con su propio contenido
     * cada tick (sacar para volver a meter) si no se le prohibe tocarse a si misma.
     */
    public ItemStack withdraw(Predicate<ItemStack> matcher, int want, long excludePos) {
        if (want <= 0) {
            return null;
        }
        List<CellState> states = load();
        ItemStack result = null;
        long got = 0;
        for (int pass = 0; pass < 2 && got < want; pass++) {
            boolean greedyPass = pass == 1;
            for (CellState state : states) {
                if (state.greedy != greedyPass || state.pos == excludePos) {
                    continue;
                }
                if (state.greedy) {
                    if (state.blob.greedySamples == null || state.blob.greedyAmounts == null) {
                        continue;
                    }
                    for (int i = 0; i < state.blob.greedySamples.size(); i++) {
                        ItemStack sample = state.blob.greedySamples.get(i);
                        Long amount = state.blob.greedyAmounts.get(i);
                        if (sample == null || amount == null || amount <= 0 || !matcher.test(sample)) {
                            continue;
                        }
                        if (result == null) {
                            result = StackUtils.getAsQuantity(sample, 0);
                        } else if (!StackUtils.itemsMatch(result, sample)) {
                            continue;
                        }
                        long take = Math.min(want - got, amount);
                        long removed = state.blob.removeGreedyItem(i, take);
                        got += removed;
                        state.dirty = true;
                        if (removed >= amount) {
                            i--;
                        }
                        if (got >= want) {
                            break;
                        }
                    }
                } else {
                    if (blobEmpty(state.blob) || !matcher.test(state.blob.cellSample)) {
                        continue;
                    }
                    if (result == null) {
                        result = StackUtils.getAsQuantity(state.blob.cellSample, 0);
                    } else if (!StackUtils.itemsMatch(result, state.blob.cellSample)) {
                        continue;
                    }
                    long take = Math.min(want - got, state.blob.cellAmount);
                    state.blob.cellAmount -= take;
                    got += take;
                    if (state.blob.cellAmount <= 0) {
                        state.blob.cellAmount = 0;
                        state.blob.cellSample = null;
                    }
                    state.dirty = true;
                }
                if (got >= want) {
                    break;
                }
            }
        }
        flush(states);
        if (result == null || got <= 0) {
            return null;
        }
        result.setAmount((int) got);
        return result;
    }

    private static boolean blobEmpty(NodeBlob blob) {
        return blob.cellSample == null || blob.cellAmount <= 0;
    }

    public long count(Predicate<ItemStack> matcher) {
        long total = 0;
        for (CellState state : load()) {
            if (state.greedy) {
                if (state.blob.greedySamples != null && state.blob.greedyAmounts != null) {
                    for (int i = 0; i < state.blob.greedySamples.size(); i++) {
                        ItemStack sample = state.blob.greedySamples.get(i);
                        Long amount = state.blob.greedyAmounts.get(i);
                        if (sample != null && amount != null && amount > 0 && matcher.test(sample)) {
                            total += amount;
                        }
                    }
                }
            } else {
                if (!blobEmpty(state.blob) && matcher.test(state.blob.cellSample)) {
                    total += state.blob.cellAmount;
                }
            }
        }
        return total;
    }

    /**
     * La foto del contenido para las grillas, cacheada 500 ms (equivalente al CACHE_ITEMS_MS del
     * NetworkRoot). La agregacion es por escaneo lineal con itemsMatch, nunca por hash.
     */
    public List<View> view() {
        long now = System.currentTimeMillis();
        if (viewCache != null && now - viewCacheAt < VIEW_CACHE_MS) {
            return new ArrayList<>(viewCache);
        }
        Map<Material, List<View>> buckets = new EnumMap<>(Material.class);
        for (CellState state : load()) {
            if (state.greedy) {
                if (state.blob.greedySamples != null && state.blob.greedyAmounts != null) {
                    for (int i = 0; i < state.blob.greedySamples.size(); i++) {
                        ItemStack sample = state.blob.greedySamples.get(i);
                        Long amount = state.blob.greedyAmounts.get(i);
                        if (sample == null || amount == null || amount <= 0) {
                            continue;
                        }
                        addToBuckets(buckets, sample, amount);
                    }
                }
            } else {
                if (blobEmpty(state.blob)) {
                    continue;
                }
                addToBuckets(buckets, state.blob.cellSample, state.blob.cellAmount);
            }
        }
        List<View> merged = new ArrayList<>();
        for (List<View> bucket : buckets.values()) {
            merged.addAll(bucket);
        }
        viewCache = new ArrayList<>(merged);
        viewCacheAt = now;
        return merged;
    }

    private static void addToBuckets(Map<Material, List<View>> buckets, ItemStack sample, long amount) {
        Material mat = sample.getType();
        List<View> bucket = buckets.computeIfAbsent(mat, k -> new ArrayList<>());
        boolean found = false;
        for (int i = 0; i < bucket.size(); i++) {
            View v = bucket.get(i);
            if (StackUtils.itemsMatch(v.sample(), sample)) {
                long sum = v.amount() + amount;
                if (sum < 0) {
                    sum = Long.MAX_VALUE;
                }
                bucket.set(i, new View(v.sample(), sum));
                found = true;
                break;
            }
        }
        if (!found) {
            bucket.add(new View(StackUtils.getAsQuantity(sample, 1), amount));
        }
    }

    /**
     * EN: Returns the total quantity of this item stored across all Greedy Cells in the network.
     *
     * ES: Devuelve la cantidad total de este ítem almacenada en celdas Greedy de la red.
     */
    public long getGreedyStoredAmount(ItemStack item) {
        if (item == null) {
            return 0;
        }
        long total = 0;
        for (CellState state : load()) {
            if (state.greedy && state.blob.greedySamples != null && state.blob.greedyAmounts != null) {
                for (int i = 0; i < state.blob.greedySamples.size(); i++) {
                    ItemStack sample = state.blob.greedySamples.get(i);
                    Long amt = state.blob.greedyAmounts.get(i);
                    if (sample != null && amt != null && amt > 0 && StackUtils.itemsMatch(sample, item)) {
                        total += amt;
                    }
                }
            }
        }
        return total;
    }

    /**
     * EN: Checks if an item matches the filter of any active Purger on the network.
     *
     * ES: Comprueba si un ítem cumple los filtros de algún Purger activo en la red.
     */
    public boolean isItemPurged(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        synchronized (network.nodes()) {
            for (var entry : network.nodes().entrySet()) {
                if (entry.getValue() == DeviceType.MVN_PURGER) {
                    long pos = entry.getKey();
                    int cx = com.chagui68.multiversenets.util.PosUtil.unpackX(pos) >> 4;
                    int cz = com.chagui68.multiversenets.util.PosUtil.unpackZ(pos) >> 4;
                    if (!network.world().isChunkLoaded(cx, cz)) {
                        continue;
                    }
                    Block block = network.block(pos);
                    NodeBlob blob = NodeStore.get(block);
                    if (blob == null) {
                        continue;
                    }
                    boolean hasItems = blob.filterItems != null && !blob.filterItems.isEmpty();
                    boolean hasMats = blob.filterMaterials != null && !blob.filterMaterials.isEmpty();
                    if (!hasItems && !hasMats) {
                        continue;
                    }
                    Predicate<ItemStack> pred = NetworkManager.filterPredicate(blob);
                    if (pred.test(item)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * EN: Returns a view of items targeted for voiding by active Purgers.
     * Includes currently stored matching items and items configured in Purger filters.
     *
     * ES: Devuelve la lista de ítems dirigidos a purga por los Purgers activos.
     */
    public List<View> getPurgedItemsView() {
        Map<Material, List<View>> buckets = new EnumMap<>(Material.class);
        List<View> allStored = view();

        for (View v : allStored) {
            if (isItemPurged(v.sample())) {
                addToBuckets(buckets, v.sample(), v.amount());
            }
        }

        synchronized (network.nodes()) {
            for (var entry : network.nodes().entrySet()) {
                if (entry.getValue() == DeviceType.MVN_PURGER) {
                    long pos = entry.getKey();
                    int cx = com.chagui68.multiversenets.util.PosUtil.unpackX(pos) >> 4;
                    int cz = com.chagui68.multiversenets.util.PosUtil.unpackZ(pos) >> 4;
                    if (!network.world().isChunkLoaded(cx, cz)) {
                        continue;
                    }
                    Block b = network.block(pos);
                    NodeBlob blob = NodeStore.get(b);
                    if (blob == null) {
                        continue;
                    }
                    if (!blob.filterBlacklist) {
                        // Misma prioridad que NetworkManager.filterPredicate: si hay plantillas
                        // (filterItems) se usan SOLO ellas; filterMaterials solo cuando no las hay.
                        boolean hasItems = blob.filterItems != null && !blob.filterItems.isEmpty();
                        if (hasItems) {
                            for (ItemStack sample : blob.filterItems) {
                                if (sample != null && !sample.getType().isAir()) {
                                    addToBuckets(buckets, sample, 0);
                                }
                            }
                        } else if (blob.filterMaterials != null) {
                            for (String matName : blob.filterMaterials) {
                                Material mat = Material.matchMaterial(matName);
                                if (mat != null && !mat.isAir() && mat.isItem()) {
                                    addToBuckets(buckets, new ItemStack(mat), 0);
                                }
                            }
                        }
                    }
                }
            }
        }
        List<View> merged = new ArrayList<>();
        for (List<View> bucket : buckets.values()) {
            merged.addAll(bucket);
        }
        return merged;
    }

    public int countActivePurgers() {
        int count = 0;
        synchronized (network.nodes()) {
            for (var entry : network.nodes().entrySet()) {
                if (entry.getValue() == DeviceType.MVN_PURGER) {
                    count++;
                }
            }
        }
        return count;
    }

    public int countActiveGreedyCells() {
        int count = 0;
        synchronized (network.nodes()) {
            for (var entry : network.nodes().entrySet()) {
                if (entry.getValue() == DeviceType.MVN_GREEDY_CELL) {
                    count++;
                }
            }
        }
        return count;
    }

    public boolean isEmpty() {
        return view().isEmpty();
    }
}

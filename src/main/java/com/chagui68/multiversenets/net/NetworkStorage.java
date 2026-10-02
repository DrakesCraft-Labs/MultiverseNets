package com.chagui68.multiversenets.net;

import com.chagui68.multiversenets.compat.SlimefunBridge;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.PosUtil;
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
 * Aggregated storage engine combining:
 * 1. Memory modules (T1-T5 in DRAM Bays; a legacy module inside the Controller still counts)
 * 2. Quantum Cells (T1-T6) & Infinity Barrels
 * 3. External Slimefun Barrels (Native integration via SlimefunBridge)
 * 4. Dedicated Greedy Cells (Output buffers / sinks)
 *
 * [ES] Motor de Almacenamiento Agregado de Red
 * Combina los módulos de memoria de los DRAM Bays, Celdas Cuánticas, Barriles de Slimefun y Celdas Greedy.
 */
public class NetworkStorage {

    private static final long VIEW_CACHE_MS = 500;

    public record View(ItemStack sample, long amount) {
    }

    private record CellRef(long pos, int tier, boolean greedy, boolean barrel) {
    }

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

    private static final class VirtualCacheState {
        private final Block block;
        private final NodeBlob blob;
        private final long capacity;
        private boolean dirty;

        private VirtualCacheState(Block block, NodeBlob blob, long capacity) {
            this.block = block;
            this.blob = blob;
            this.capacity = capacity;
        }
    }

    private final Network network;
    private final List<CellRef> cells = new ArrayList<>();
    private final List<Long> bays = new ArrayList<>();
    private long boundVersion = -1;
    private List<View> viewCache;
    private long viewCacheAt;

    public NetworkStorage(Network network) {
        this.network = network;
    }

    public synchronized void invalidate() {
        boundVersion = -1;
        viewCache = null;
    }

    private void sync() {
        long current = network.versionSnapshot();
        if (boundVersion == current) {
            return;
        }
        cells.clear();
        bays.clear();
        synchronized (network.nodes()) {
            for (var entry : network.nodes().entrySet()) {
                DeviceType type = entry.getValue();
                if (type == DeviceType.MVN_DRAM_BAY) {
                    bays.add(entry.getKey());
                } else if (type.isCell()) {
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

    /**
     * EN: Every item memory module of the network: one per DRAM Bay holding a cache module, plus
     * the module of a Controller built before the DRAM Bay existed (it keeps working until it is
     * moved to a bay).
     *
     * ES: Todos los módulos de memoria de ítems de la red: uno por DRAM Bay con módulo de caché,
     * más el módulo de un Controlador anterior al DRAM Bay (sigue funcionando hasta moverlo).
     */
    private List<VirtualCacheState> loadVirtualCaches() {
        List<VirtualCacheState> caches = new ArrayList<>();
        addVirtualCache(caches, network.controllerPos(), false);
        for (long pos : bays) {
            addVirtualCache(caches, pos, true);
        }
        return caches;
    }

    private void addVirtualCache(List<VirtualCacheState> caches, long pos, boolean bay) {
        int cx = PosUtil.unpackX(pos) >> 4;
        int cz = PosUtil.unpackZ(pos) >> 4;
        if (!network.world().isChunkLoaded(cx, cz)) {
            return;
        }
        Block block = network.block(pos);
        NodeBlob blob = NodeStore.canonical(block);
        if (blob == null || blob.virtualCacheTier <= 0) {
            return;
        }
        if (bay) {
            DeviceType module = MemoryModules.installed(blob);
            if (DeviceType.parse(blob.typeName) != DeviceType.MVN_DRAM_BAY || module == null || !module.isCacheModule()) {
                return;
            }
        }
        long cap = Settings.virtualCacheCapacity(blob.virtualCacheTier);
        if (cap <= 0) {
            return;
        }
        caches.add(new VirtualCacheState(block, blob, cap));
    }

    private List<Block> loadSfBarrels() {
        if (!Settings.compatSlimefun() || !SlimefunBridge.isAvailable()) {
            return List.of();
        }
        List<Block> list = new ArrayList<>();
        for (long pos : network.slimefunBarrels()) {
            int cx = PosUtil.unpackX(pos) >> 4;
            int cz = PosUtil.unpackZ(pos) >> 4;
            if (!network.world().isChunkLoaded(cx, cz)) {
                continue;
            }
            Block b = network.block(pos);
            if (SlimefunBridge.isBarrel(b)) {
                list.add(b);
            }
        }
        return list;
    }

    private List<CellState> load() {
        sync();
        List<CellState> states = new ArrayList<>(cells.size());
        for (CellRef ref : cells) {
            int cx = PosUtil.unpackX(ref.pos()) >> 4;
            int cz = PosUtil.unpackZ(ref.pos()) >> 4;
            if (!network.world().isChunkLoaded(cx, cz)) {
                continue;
            }
            Block block = network.block(ref.pos());
            // canonical(), no get(): esta lista se reconstruye en cada deposito y retirada, y
            // decodificar el blob de cada celda cada vez era el coste dominante del ticker.
            NodeBlob blob = NodeStore.canonical(block);
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

    private void flush(List<CellState> states, List<VirtualCacheState> vCaches) {
        boolean anyDirty = false;
        for (CellState state : states) {
            if (state.dirty) {
                NodeStore.put(state.block, state.blob);
                anyDirty = true;
            }
        }
        for (VirtualCacheState vCache : vCaches) {
            if (vCache.dirty) {
                NodeStore.put(vCache.block, vCache.blob);
                anyDirty = true;
            }
        }
        if (anyDirty) {
            viewCache = null;
        }
    }

    public synchronized long remainingQuota(ItemStack item) {
        List<CellState> states = load();
        return remainingQuota(item, states, loadVirtualCaches(), loadSfBarrels());
    }

    /**
     * Misma regla que la version publica, pero sobre estructuras que el llamante ya leyó. Existe para
     * que un deposito no vuelva a leer y deserializar cada celda solo para calcular su cuota.
     */
    private long remainingQuota(ItemStack item, List<CellState> states,
                                List<VirtualCacheState> vCaches, List<Block> sfBarrels) {
        if (item == null || item.getType().isAir()) {
            return Long.MAX_VALUE;
        }
        long minAllowed = Long.MAX_VALUE;
        boolean hasLimiter = false;
        synchronized (network.nodes()) {
            for (var entry : network.nodes().entrySet()) {
                if (entry.getValue() == DeviceType.MVN_LIMITER) {
                    long pos = entry.getKey();
                    int cx = PosUtil.unpackX(pos) >> 4;
                    int cz = PosUtil.unpackZ(pos) >> 4;
                    if (!network.world().isChunkLoaded(cx, cz)) {
                        continue;
                    }
                    Block b = network.block(pos);
                    NodeBlob blob = NodeStore.canonical(b);
                    if (blob == null || !blob.quotaActive || blob.quotaSample == null || blob.quotaLimit < 0) {
                        continue;
                    }
                    if (StackUtils.itemsMatch(blob.quotaSample, item)) {
                        hasLimiter = true;
                        if (blob.quotaLimit < minAllowed) {
                            minAllowed = blob.quotaLimit;
                        }
                    }
                }
            }
        }
        if (!hasLimiter) {
            return Long.MAX_VALUE;
        }
        long currentTotal = count(i -> StackUtils.itemsMatch(i, item), states, vCaches, sfBarrels);
        return Math.max(0, minAllowed - currentTotal);
    }

    public synchronized int deposit(ItemStack item) {
        if (item == null || item.getType().isAir() || item.getAmount() <= 0) {
            return 0;
        }
        // Una sola lectura de las celdas para toda la operacion. Antes el calculo de cuota volvia a
        // llamar a count(), que re-deserializaba cada blob, y un deposito pagaba tres lecturas por
        // celda en lugar de una.
        List<CellState> states = load();
        List<VirtualCacheState> vCaches = loadVirtualCaches();
        List<Block> sfBarrels = loadSfBarrels();

        long quotaHeadroom = remainingQuota(item, states, vCaches, sfBarrels);
        if (quotaHeadroom <= 0) {
            return item.getAmount();
        }
        long amountToDeposit = Math.min((long) item.getAmount(), quotaHeadroom);
        long rejectedByQuota = item.getAmount() - amountToDeposit;
        long remaining = amountToDeposit;

        // 1. Greedy cells
        for (CellState state : states) {
            if (!state.greedy) continue;
            long space = state.capacity - state.blob.totalGreedyAmount();
            if (space <= 0) continue;
            boolean matchesExisting = state.blob.indexOfGreedySample(item) >= 0;
            boolean matchesFilter = (state.blob.filterMaterials != null && !state.blob.filterMaterials.isEmpty())
                    || (state.blob.filterItems != null && !state.blob.filterItems.isEmpty());
            if (matchesExisting || (matchesFilter && NetworkManager.filterPredicate(state.blob).test(item))) {
                long take = Math.min(space, remaining);
                state.blob.addGreedyItem(item, take);
                state.dirty = true;
                remaining -= take;
                if (remaining <= 0) break;
            }
        }

        // 2. Memory modules (matching existing sample)
        for (VirtualCacheState vCache : vCaches) {
            if (remaining <= 0) break;
            long space = vCache.capacity - vCache.blob.totalVirtualAmount();
            if (space > 0 && vCache.blob.indexOfVirtualSample(item) >= 0) {
                long take = Math.min(space, remaining);
                vCache.blob.addVirtualItem(item, take);
                vCache.dirty = true;
                remaining -= take;
            }
        }

        // 3. Slimefun Barrels (matching existing sample)
        if (remaining > 0 && !sfBarrels.isEmpty()) {
            for (Block barrel : sfBarrels) {
                ItemStack storedSample = SlimefunBridge.getBarrelStoredItem(barrel);
                if (storedSample != null && StackUtils.itemsMatch(storedSample, item)) {
                    ItemStack toDeposit = StackUtils.getAsQuantity(item, (int) Math.min(Integer.MAX_VALUE, remaining));
                    int unhoused = SlimefunBridge.depositBarrel(barrel, toDeposit);
                    long deposited = toDeposit.getAmount() - unhoused;
                    remaining -= deposited;
                    if (remaining <= 0) break;
                }
            }
        }

        // 4. Normal cells with same type
        if (remaining > 0) {
            for (CellState state : states) {
                if (state.greedy || state.blob.cellSample == null
                        || !StackUtils.itemsMatch(state.blob.cellSample, item)) {
                    continue;
                }
                remaining = pour(state, item, remaining);
                if (remaining <= 0) break;
            }
        }

        // 5. Memory modules (empty / new item space)
        for (VirtualCacheState vCache : vCaches) {
            if (remaining <= 0) break;
            long space = vCache.capacity - vCache.blob.totalVirtualAmount();
            if (space > 0) {
                long take = Math.min(space, remaining);
                vCache.blob.addVirtualItem(item, take);
                vCache.dirty = true;
                remaining -= take;
            }
        }

        // 6. Empty Slimefun Barrels
        if (remaining > 0 && !sfBarrels.isEmpty()) {
            for (Block barrel : sfBarrels) {
                if (SlimefunBridge.getBarrelStoredAmount(barrel) == 0) {
                    ItemStack toDeposit = StackUtils.getAsQuantity(item, (int) Math.min(Integer.MAX_VALUE, remaining));
                    int unhoused = SlimefunBridge.depositBarrel(barrel, toDeposit);
                    long deposited = toDeposit.getAmount() - unhoused;
                    remaining -= deposited;
                    if (remaining <= 0) break;
                }
            }
        }

        // 7. Empty normal cells
        if (remaining > 0) {
            for (CellState state : states) {
                if (state.greedy || state.blob.cellSample != null) {
                    continue;
                }
                state.blob.cellSample = StackUtils.getAsQuantity(item, 1);
                remaining = pour(state, item, remaining);
                if (remaining <= 0) break;
            }
        }

        // 8. Open Greedy cells (fallback when greedy cell has no filter configured: acts as shared general storage)
        if (remaining > 0) {
            for (CellState state : states) {
                if (!state.greedy) continue;
                boolean hasFilter = (state.blob.filterMaterials != null && !state.blob.filterMaterials.isEmpty())
                        || (state.blob.filterItems != null && !state.blob.filterItems.isEmpty());
                if (hasFilter) continue; // Filtered greedy cells were handled in step 1
                long space = state.capacity - state.blob.totalGreedyAmount();
                if (space <= 0) continue;
                long take = Math.min(space, remaining);
                state.blob.addGreedyItem(item, take);
                state.dirty = true;
                remaining -= take;
                if (remaining <= 0) break;
            }
        }

        flush(states, vCaches);
        return (int) (remaining + rejectedByQuota);
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

    public synchronized int depositAll(List<ItemStack> items) {
        int leftover = 0;
        for (ItemStack item : items) {
            leftover += deposit(item);
        }
        return leftover;
    }

    public synchronized ItemStack withdraw(Predicate<ItemStack> matcher, int want) {
        return withdraw(matcher, want, -1L, true);
    }

    public synchronized ItemStack withdraw(Predicate<ItemStack> matcher, int want, long excludePos) {
        return withdraw(matcher, want, excludePos, true);
    }

    public synchronized ItemStack withdraw(Predicate<ItemStack> matcher, int want, long excludePos, boolean includeGreedy) {
        if (want <= 0) {
            return null;
        }
        List<CellState> states = load();
        List<VirtualCacheState> vCaches = loadVirtualCaches();
        List<Block> sfBarrels = loadSfBarrels();
        ItemStack result = null;
        long got = 0;

        // Pass 0: Memory modules (DRAM Bays and a legacy Controller cache)
        for (VirtualCacheState vCache : vCaches) {
            if (got >= want || vCache.blob.virtualSamples == null) {
                continue;
            }
            for (int i = 0; i < vCache.blob.virtualSamples.size(); i++) {
                ItemStack sample = vCache.blob.virtualSamples.get(i);
                Long amount = vCache.blob.virtualAmounts.get(i);
                if (sample == null || amount == null || amount <= 0 || !matcher.test(sample)) {
                    continue;
                }
                if (result == null) {
                    result = StackUtils.getAsQuantity(sample, 0);
                } else if (!StackUtils.itemsMatch(result, sample)) {
                    continue;
                }
                long take = Math.min(want - got, amount);
                long removed = vCache.blob.removeVirtualItem(i, take);
                got += removed;
                vCache.dirty = true;
                if (removed >= amount) {
                    i--;
                }
                if (got >= want) break;
            }
        }

        // Pass 1: Quantum Cells & Infinity Barrels
        if (got < want) {
            for (CellState state : states) {
                if (state.greedy || state.pos == excludePos || blobEmpty(state.blob) || !matcher.test(state.blob.cellSample)) {
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
                    if (!state.barrel) {
                        state.blob.cellSample = null;
                    }
                }
                state.dirty = true;
                if (got >= want) break;
            }
        }

        // Pass 2: Slimefun Barrels
        if (got < want && !sfBarrels.isEmpty()) {
            for (Block barrel : sfBarrels) {
                ItemStack sample = SlimefunBridge.getBarrelStoredItem(barrel);
                if (sample == null || !matcher.test(sample)) continue;
                if (result == null) {
                    result = StackUtils.getAsQuantity(sample, 0);
                } else if (!StackUtils.itemsMatch(result, sample)) {
                    continue;
                }
                ItemStack extracted = SlimefunBridge.withdrawBarrel(barrel, matcher, (int) Math.min(Integer.MAX_VALUE, want - got));
                if (extracted != null) {
                    got += extracted.getAmount();
                }
                if (got >= want) break;
            }
        }

        // Pass 3: Greedy cells (output buffer sink)
        if (includeGreedy && got < want) {
            for (CellState state : states) {
                if (!state.greedy || state.pos == excludePos || state.blob.greedySamples == null || state.blob.greedyAmounts == null) {
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
                    if (got >= want) break;
                }
                if (got >= want) break;
            }
        }

        flush(states, vCaches);
        if (result == null || got <= 0) {
            return null;
        }
        result.setAmount((int) got);
        return result;
    }

    private static boolean blobEmpty(NodeBlob blob) {
        return blob.cellSample == null || blob.cellAmount <= 0;
    }

    public synchronized long count(Predicate<ItemStack> matcher) {
        List<CellState> states = load();
        return count(matcher, states, loadVirtualCaches(), loadSfBarrels());
    }

    private long count(Predicate<ItemStack> matcher, List<CellState> states,
                       List<VirtualCacheState> vCaches, List<Block> sfBarrels) {
        long total = 0;
        for (VirtualCacheState vCache : vCaches) {
            if (vCache.blob.virtualSamples == null) {
                continue;
            }
            for (int i = 0; i < vCache.blob.virtualSamples.size(); i++) {
                ItemStack sample = vCache.blob.virtualSamples.get(i);
                Long amt = vCache.blob.virtualAmounts.get(i);
                if (sample != null && amt != null && amt > 0 && matcher.test(sample)) {
                    total += amt;
                }
            }
        }
        for (CellState state : states) {
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
        for (Block barrel : sfBarrels) {
            ItemStack sample = SlimefunBridge.getBarrelStoredItem(barrel);
            if (sample != null && matcher.test(sample)) {
                total += SlimefunBridge.getBarrelStoredAmount(barrel);
            }
        }
        return total;
    }

    public synchronized List<View> view() {
        long now = System.currentTimeMillis();
        if (viewCache != null && now - viewCacheAt < VIEW_CACHE_MS) {
            return new ArrayList<>(viewCache);
        }
        Map<Material, List<View>> buckets = new EnumMap<>(Material.class);

        // Memory modules
        List<CellState> cellStates = load();
        for (VirtualCacheState vCache : loadVirtualCaches()) {
            if (vCache.blob.virtualSamples == null) {
                continue;
            }
            for (int i = 0; i < vCache.blob.virtualSamples.size(); i++) {
                ItemStack sample = vCache.blob.virtualSamples.get(i);
                Long amt = vCache.blob.virtualAmounts.get(i);
                if (sample != null && amt != null && amt > 0) {
                    addToBuckets(buckets, sample, amt);
                }
            }
        }

        // Cells
        for (CellState state : cellStates) {
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

        // Slimefun Barrels
        for (Block barrel : loadSfBarrels()) {
            ItemStack sample = SlimefunBridge.getBarrelStoredItem(barrel);
            long amt = SlimefunBridge.getBarrelStoredAmount(barrel);
            if (sample != null && amt > 0) {
                addToBuckets(buckets, sample, amt);
            }
        }

        List<View> merged = new ArrayList<>();
        for (List<View> bucket : buckets.values()) {
            merged.addAll(bucket);
        }
        viewCache = List.copyOf(merged);
        viewCacheAt = now;
        return new ArrayList<>(viewCache);
    }


    public synchronized long getGreedyStoredAmount(ItemStack item) {
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

    /** Units of {@code item} held by memory modules (DRAM Bays), for the Terminal's lore. */
    public synchronized long getMemoryStoredAmount(ItemStack item) {
        if (item == null) {
            return 0;
        }
        load();
        long total = 0;
        for (VirtualCacheState vCache : loadVirtualCaches()) {
            if (vCache.blob.virtualSamples == null) {
                continue;
            }
            for (int i = 0; i < vCache.blob.virtualSamples.size(); i++) {
                ItemStack sample = vCache.blob.virtualSamples.get(i);
                Long amt = vCache.blob.virtualAmounts.get(i);
                if (sample != null && amt != null && amt > 0 && StackUtils.itemsMatch(sample, item)) {
                    total += amt;
                }
            }
        }
        return total;
    }

    public synchronized boolean isItemPurged(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        synchronized (network.nodes()) {
            for (var entry : network.nodes().entrySet()) {
                if (entry.getValue() == DeviceType.MVN_PURGER) {
                    long pos = entry.getKey();
                    int cx = PosUtil.unpackX(pos) >> 4;
                    int cz = PosUtil.unpackZ(pos) >> 4;
                    if (!network.world().isChunkLoaded(cx, cz)) {
                        continue;
                    }
                    Block block = network.block(pos);
                    NodeBlob blob = NodeStore.canonical(block);
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

    public synchronized List<View> getPurgedItemsView() {
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
                    int cx = PosUtil.unpackX(pos) >> 4;
                    int cz = PosUtil.unpackZ(pos) >> 4;
                    if (!network.world().isChunkLoaded(cx, cz)) {
                        continue;
                    }
                    Block b = network.block(pos);
                    NodeBlob blob = NodeStore.canonical(b);
                    if (blob == null) {
                        continue;
                    }
                    if (!blob.filterBlacklist) {
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

    public synchronized boolean isEmpty() {
        return view().isEmpty();
    }

    private static void addToBuckets(Map<Material, List<View>> buckets, ItemStack sample, long amount) {
        List<View> bucket = buckets.computeIfAbsent(sample.getType(), k -> new ArrayList<>());
        for (int i = 0; i < bucket.size(); i++) {
            View v = bucket.get(i);
            if (StackUtils.itemsMatch(v.sample, sample)) {
                bucket.set(i, new View(v.sample, v.amount + amount));
                return;
            }
        }
        bucket.add(new View(StackUtils.getAsQuantity(sample, 1), amount));
    }
}

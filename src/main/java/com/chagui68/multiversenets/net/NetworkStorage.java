package com.chagui68.multiversenets.net;

import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.PosUtil;
import com.chagui68.multiversenets.util.Settings;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class NetworkStorage {

    public record View(ItemStack sample, long amount) {
    }

    private record CellRef(long pos, int tier) {
    }

    private final Network network;
    private final List<CellRef> cells = new ArrayList<>();
    private long boundVersion = -1;

    public NetworkStorage(Network network) {
        this.network = network;
    }

    public void invalidate() {
        boundVersion = -1;
    }

    private void sync() {
        if (boundVersion == network.versionSnapshot()) {
            return;
        }
        cells.clear();
        synchronized (network.nodes()) {
            for (Map.Entry<Long, DeviceType> entry : network.nodes().entrySet()) {
                if (entry.getValue().isCell()) {
                    cells.add(new CellRef(entry.getKey(), entry.getValue().cellTier()));
                }
            }
        }
        boundVersion = network.versionSnapshot();
    }

    public int depositAll(List<ItemStack> items) {
        int leftover = 0;
        for (ItemStack item : items) {
            leftover += deposit(item);
        }
        return leftover;
    }

    public int deposit(ItemStack item) {
        if (item == null || item.getType().isAir() || item.getAmount() <= 0) {
            return 0;
        }
        sync();
        int remaining = item.getAmount();
        List<CellRef> candidates = new ArrayList<>(cells);

        candidates.sort((a, b) -> Long.compare(freeSpace(a, item), freeSpace(b, item)));

        for (CellRef cell : candidates) {
            Block block = network.block(cell.pos());
            NodeBlob blob = NodeStore.get(block);
            if (blob == null || !DeviceType.parse(blob.typeName).isCell()) {
                continue;
            }
            if (blob.cellSample != null && !blob.cellSample.isSimilar(item)) {
                continue;
            }
            if (blob.cellAmount == 0 || blob.cellSample == null) {
                blob.cellSample = item.clone();
                blob.cellSample.setAmount(1);
            }
            long cap = Settings.cellCapacity(cell.tier());
            long space = cap - blob.cellAmount;
            if (space <= 0) {
                continue;
            }
            int take = (int) Math.min(space, remaining);
            blob.cellAmount += take;
            remaining -= take;
            NodeStore.put(block, blob);
            if (remaining <= 0) {
                break;
            }
        }
        return remaining;
    }

    private long freeSpace(CellRef cell, ItemStack sample) {
        Block block = network.block(cell.pos());
        NodeBlob blob = NodeStore.get(block);
        if (blob == null) {
            return 0;
        }
        if (blob.cellSample != null && !blob.cellSample.isSimilar(sample)) {
            return -1;
        }
        return Settings.cellCapacity(cell.tier()) - Math.max(0, blob.cellAmount);
    }

    public ItemStack withdraw(Predicate<ItemStack> matcher, int want) {
        sync();
        int got = 0;
        ItemStack result = null;
        for (CellRef cell : new ArrayList<>(cells)) {
            if (got >= want) {
                break;
            }
            Block block = network.block(cell.pos());
            NodeBlob blob = NodeStore.get(block);
            if (blob == null || blob.cellSample == null || blob.cellAmount <= 0) {
                continue;
            }
            if (!matcher.test(blob.cellSample)) {
                continue;
            }
            if (result == null) {
                result = blob.cellSample.clone();
            }
            int take = (int) Math.min((long) want - got, blob.cellAmount);
            got += take;
            blob.cellAmount -= take;
            if (blob.cellAmount <= 0) {
                blob.cellAmount = 0;
                blob.cellSample = null;
            }
            NodeStore.put(block, blob);
        }
        if (result == null || got <= 0) {
            return null;
        }
        result.setAmount(got);
        return result;
    }

    public long count(Predicate<ItemStack> matcher) {
        sync();
        long total = 0;
        for (CellRef cell : cells) {
            Block block = network.block(cell.pos());
            NodeBlob blob = NodeStore.get(block);
            if (blob != null && blob.cellSample != null && matcher.test(blob.cellSample)) {
                total += blob.cellAmount;
            }
        }
        return total;
    }

    public List<View> view() {
        sync();
        Map<String, View> merged = new LinkedHashMap<>();
        for (CellRef cell : cells) {
            Block block = network.block(cell.pos());
            NodeBlob blob = NodeStore.get(block);
            if (blob == null || blob.cellSample == null || blob.cellAmount <= 0) {
                continue;
            }
            String key = blob.cellSample.getType() + "|" + (blob.cellSample.hasItemMeta()
                    ? String.valueOf(blob.cellSample.getItemMeta().hashCode()) : "-");
            View existing = merged.get(key);
            merged.put(key, existing == null
                    ? new View(blob.cellSample.clone(), blob.cellAmount)
                    : new View(existing.sample(), existing.amount() + blob.cellAmount));
        }
        return new ArrayList<>(merged.values());
    }

    public boolean isEmpty() {
        return view().isEmpty();
    }
}

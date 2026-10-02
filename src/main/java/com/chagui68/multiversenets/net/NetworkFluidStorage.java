package com.chagui68.multiversenets.net;

import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.PosUtil;
import com.chagui68.multiversenets.util.Settings;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * [EN] Network Fluid Storage Engine
 * Manages aggregated fluid storage (Water, Lava, Milk, Honey, Powder Snow, etc.) across
 * all connected Quantum Fluid Cells (MVN_FLUID_CELL) on a network.
 *
 * [ES] Motor de Almacenamiento de Fluidos de Red
 * Gestiona el almacenamiento agregado de líquidos en todas las Celdas Cuánticas de Fluidos conectadas.
 */
public class NetworkFluidStorage {

    private final Network network;

    public NetworkFluidStorage(Network network) {
        this.network = network;
    }

    private static final class FluidCellRef {
        private final Block block;
        private final NodeBlob blob;

        private FluidCellRef(Block block, NodeBlob blob) {
            this.block = block;
            this.blob = blob;
        }
    }

    private List<FluidCellRef> loadCells() {
        List<FluidCellRef> list = new ArrayList<>();
        network.forEach(DeviceType.MVN_FLUID_CELL, (pos, type) -> {
            int cx = PosUtil.unpackX(pos) >> 4;
            int cz = PosUtil.unpackZ(pos) >> 4;
            if (!network.world().isChunkLoaded(cx, cz)) {
                return;
            }
            Block block = network.block(pos);
            if (block == null) {
                return;
            }
            NodeBlob blob = NodeStore.get(block);
            if (blob != null) {
                list.add(new FluidCellRef(block, blob));
            }
        });
        return list;
    }

    /**
     * @return Map of fluid type names to total stored volume in millibuckets (mB).
     */
    public synchronized Map<String, Long> getFluids() {
        Map<String, Long> totals = new LinkedHashMap<>();
        for (FluidCellRef ref : loadCells()) {
            if (ref.blob.fluidType != null && ref.blob.fluidAmount > 0) {
                String type = ref.blob.fluidType.toUpperCase(Locale.ROOT);
                totals.merge(type, ref.blob.fluidAmount, Long::sum);
            }
        }
        return totals;
    }

    /**
     * @param fluidType Fluid type name (e.g. "WATER", "LAVA")
     * @return Total millibuckets of the given fluid currently in storage.
     */
    public synchronized long count(String fluidType) {
        if (fluidType == null) {
            return 0L;
        }
        String target = fluidType.toUpperCase(Locale.ROOT);
        long total = 0L;
        for (FluidCellRef ref : loadCells()) {
            if (ref.blob.fluidType != null && target.equalsIgnoreCase(ref.blob.fluidType)) {
                total += ref.blob.fluidAmount;
            }
        }
        return total;
    }

    /**
     * EN: Deposits a volume of fluid, all or nothing. Every caller (pump, terminal, input slot)
     * consumes a whole bucket, bottle or source block only when the result is 0, so a partial fill
     * used to keep part of the fluid in the cells AND the bucket in the player's hand.
     *
     * ES: Deposita un volumen de fluido, todo o nada. Cada llamante (bomba, terminal, ranura de
     * entrada) solo consume el cubo, la botella o el bloque fuente si el resultado es 0, así que un
     * llenado parcial dejaba parte del fluido en las celdas Y el cubo en la mano del jugador.
     *
     * @param fluidType Fluid type name (e.g. "WATER", "LAVA", "MILK", "HONEY")
     * @param amountMb  Volume to deposit in millibuckets (mB)
     * @return 0 if everything was stored, otherwise {@code amountMb} (nothing was stored).
     */
    public synchronized long deposit(String fluidType, long amountMb) {
        if (fluidType == null || amountMb <= 0) {
            return amountMb;
        }
        String target = fluidType.toUpperCase(Locale.ROOT);
        long capacity = Settings.fluidCellCapacity();
        long remaining = amountMb;
        List<FluidCellRef> cells = loadCells();
        if (spaceFor(target, cells, capacity) < amountMb) {
            return amountMb;
        }

        // Pass 1: Fill existing cells of matching fluid type
        for (FluidCellRef ref : cells) {
            if (remaining <= 0) break;
            if (ref.blob.fluidType != null && target.equalsIgnoreCase(ref.blob.fluidType)) {
                long space = Math.max(0, capacity - ref.blob.fluidAmount);
                if (space > 0) {
                    long toAdd = Math.min(space, remaining);
                    ref.blob.fluidAmount += toAdd;
                    remaining -= toAdd;
                    NodeStore.put(ref.block, ref.blob);
                }
            }
        }

        // Pass 2: Fill empty cells
        for (FluidCellRef ref : cells) {
            if (remaining <= 0) break;
            if (ref.blob.fluidAmount <= 0 || ref.blob.fluidType == null) {
                ref.blob.fluidType = target;
                long toAdd = Math.min(capacity, remaining);
                ref.blob.fluidAmount = toAdd;
                remaining -= toAdd;
                NodeStore.put(ref.block, ref.blob);
            }
        }

        return remaining;
    }

    private static long spaceFor(String target, List<FluidCellRef> cells, long capacity) {
        long space = 0L;
        for (FluidCellRef ref : cells) {
            if (ref.blob.fluidAmount <= 0 || ref.blob.fluidType == null) {
                space += capacity;
            } else if (target.equalsIgnoreCase(ref.blob.fluidType)) {
                space += Math.max(0, capacity - ref.blob.fluidAmount);
            }
        }
        return space;
    }

    /**
     * Withdraws a volume of fluid from cells containing it.
     *
     * @param fluidType Fluid type name
     * @param amountMb  Maximum volume to withdraw in millibuckets (mB)
     * @return Total volume in mB successfully withdrawn.
     */
    public synchronized long withdraw(String fluidType, long amountMb) {
        if (fluidType == null || amountMb <= 0) {
            return 0L;
        }
        String target = fluidType.toUpperCase(Locale.ROOT);
        long needed = amountMb;
        long extracted = 0L;

        for (FluidCellRef ref : loadCells()) {
            if (needed <= 0) break;
            if (ref.blob.fluidType != null && target.equalsIgnoreCase(ref.blob.fluidType) && ref.blob.fluidAmount > 0) {
                long available = ref.blob.fluidAmount;
                long toTake = Math.min(available, needed);
                ref.blob.fluidAmount -= toTake;
                if (ref.blob.fluidAmount <= 0) {
                    ref.blob.fluidAmount = 0;
                    ref.blob.fluidType = null;
                }
                needed -= toTake;
                extracted += toTake;
                NodeStore.put(ref.block, ref.blob);
            }
        }

        return extracted;
    }

    /**
     * @return Total capacity of all connected fluid cells combined in millibuckets (mB).
     */
    public synchronized long totalCapacity() {
        return (long) loadCells().size() * Settings.fluidCellCapacity();
    }

    /**
     * @return Total fluid stored across all fluid cells in millibuckets (mB).
     */
    public synchronized long totalStored() {
        long sum = 0L;
        for (FluidCellRef ref : loadCells()) {
            if (ref.blob.fluidType != null && ref.blob.fluidAmount > 0) {
                sum += ref.blob.fluidAmount;
            }
        }
        return sum;
    }

    /**
     * Alias for {@link #totalStored()}.
     */
    public synchronized long getTotalAmountMb() {
        return totalStored();
    }
}
